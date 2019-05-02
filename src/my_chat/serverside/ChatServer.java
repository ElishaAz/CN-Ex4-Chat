package my_chat.serverside;

import my_chat.message_types.ClientMessage;
import my_chat.message_types.IMessage;
import my_chat.message_types.ServerMessage;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * @author Elisha
 */
public class ChatServer implements IMessage
{

	private static final String nameExistsMessage = "That source already exists. Try a different one",
			nameInvalidMessage = "That source is invalid. Try a different one",
			loginAcceptedMessage = "Login Accepted with source: ";

	private static final String nameRegexPattern = "\\w";

	private final Map<String, ObjectOutputStream> clients = new HashMap<>();

	public final List<IMessage> allMessages = new LinkedList<>();

	private final int numberOfThreads;
	private final int port;
	private final ServerListener listener;

	public ChatServer(int port, int numberOfThreads, ServerListener listener)
	{
		this.port = port;
		this.numberOfThreads = numberOfThreads;
		this.listener = listener;
	}

	private boolean keepGoing = true;
	private final Object serverRunningLock = new Object();

	/**
	 * Start the server.
	 */
	public void run()
	{
		synchronized (serverRunningLock)
		{
			keepGoing = true;
			System.out.println("Server is running...");
			listener.stat("Server is running...",false);

			var pool = Executors.newFixedThreadPool(numberOfThreads);

			try (var listener = new ServerSocket(port))
			{
				while (keepGoing)
				{
					pool.execute(new Handler(listener.accept()));
				}
			} catch (IOException e)
			{
				e.printStackTrace();
			} finally
			{
				stop();

			}
		}
	}

	/**
	 * Stop the server.
	 */
	public synchronized void stop()
	{
		keepGoing = false;

		System.out.println("Server stopping ...");
		listener.stat("Server stopping ...",false);

		closeAndClearClients();
		allMessages.clear();
		System.out.println("Server stopped");
		listener.stat("Server stopped",false);
	}

	/**
	 * Closes all the streams in 'clients' and the clears it.
	 */
	private void closeAndClearClients()
	{
		for (var stream : clients.values())
		{
			try
			{
				stream.close();
			} catch (IOException e)
			{
				System.out.println("stream " + stream + " was already closed");
				listener.stat("Stream " + stream + " was already closed",true);
			}
		}
		clients.clear();
	}

	/**
	 * A class for handling the clients
	 */
	private class Handler implements Runnable
	{
		private String name;
		private Socket socket;
		private ObjectInputStream in;
		private ObjectOutputStream out;

		private boolean keepGoing;

		Handler(Socket socket)
		{
			this.socket = socket;
		}

		@Override
		public void run()
		{
			try
			{
				in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
				out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));


				// get login info
				boolean loginAccepted = false;
				while (!loginAccepted)
				{
					if (in.available() > 0)
					{
						ClientMessage message = (ClientMessage) in.readObject();
						if (message.getType() == ClientMessage.Type.Login)
						{
							loginAccepted = clientConnected(message, out);

							if (loginAccepted)
							{
								name = message.source;
							}
						}
					} else
					{
						out.writeObject(ServerMessage.loginRequest());
						out.flush();
					}
				}

				// receive messages from the client.
				boolean logout = false;
				while (!logout)
				{
					if (in.available() > 0)
					{
						ClientMessage message = (ClientMessage) in.readObject();
						if (message.getType() == ClientMessage.Type.Logout)
						{
							logout = clientLogout(message);
						} else
						{
							messageReceived(message);
						}
					}
				}

			} catch (IOException e)
			{
				System.out.println("IOException. Thread dropped. Name: " + (name == null ? "'null'" : name));
				listener.stat("IOException. Thread dropped. Name: " + (name == null ? "'null'" : name),true);
				clientDisconnected(name);
			} catch (ClassNotFoundException e)
			{
				System.out.println("Problem reading from stream. Thread dropped. Name: " + (name == null ? "'null'" : name));
				listener.stat("Problem reading from stream. Thread dropped. Name: " + (name == null ? "'null'" : name),true);
				clientDisconnected(name);
			} finally
			{
				if (name != null)
				{
					clientDisconnected(name);
				} else if (out != null)
				{
					//TODO remove out stream
				}

				try
				{
					socket.close();
				} catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Deals with the client's request to log in.
	 *
	 * @param loginMessage the client's message asking to log in.
	 * @param out          the output stream to the client.
	 * @return true if the login is valid and false otherwise.
	 */
	private synchronized boolean clientConnected(ClientMessage loginMessage, ObjectOutputStream out)
	{
		messageReceived(loginMessage);

		boolean accepted;
		String message;

		if (loginMessage.source == null || clients.containsKey(loginMessage.source))
		{
			message = nameExistsMessage;
			accepted = false;
		} else
		{
			if (isValidName(loginMessage.source))
			{
				clients.put(loginMessage.source, out);
				message = loginAcceptedMessage + loginMessage.source;
				accepted = true;
			} else
			{
				message = nameInvalidMessage;
				accepted = false;
			}
		}

		try
		{
			out.writeObject(ServerMessage.loginResponse(accepted, message, loginMessage.source));
		} catch (IOException e)
		{
			System.out.println("Problem writing to new login: " + loginMessage.source + ". login is rejected.");
			listener.stat("Problem writing to new login: " + loginMessage.source + ". login is rejected.",true);
			accepted = false;
			clients.remove(loginMessage.source);
		}

		if (accepted)
		{
			sendMessage(ServerMessage.clientJoined(loginMessage.source));
		}

		return accepted;
	}

	/**
	 * Checks if the source the client gave is valid.
	 *
	 * @param name the source the client gave.
	 */
	private boolean isValidName(String name)
	{
		return Pattern.matches(nameRegexPattern, name) && !name.toLowerCase().equals("server");
	}

	/**
	 * Deals with incoming messages.
	 *
	 * @param message the incoming message.
	 */
	private void messageReceived(ClientMessage message)
	{
		allMessages.add(message);
		listener.messageReceived(message);
		switch (message.getType())
		{
			case Message:
				sendMessage(ServerMessage.message(message.source, message.dest, message.message));
				break;
			case ListNames:
				sendMessage(ServerMessage.names(new HashSet<>(clients.keySet()), message.source));
				break;
		}
	}

	/**
	 * Sends a message to whoever needs to receive it.
	 *
	 * @param message the message to send.
	 * @return true if the message was sent, and false otherwise (i.e. if there was an error).
	 */
	private synchronized boolean sendMessage(ServerMessage message)
	{
		boolean sent = true;

		allMessages.add(message);
		listener.messageSent(message);

		if (message.dest == null)
		{
			for (var name : clients.keySet())
			{
				if (!sendMessageTo(message, name))
				{
					sent = false;
				}
			}
		} else
		{
			sent = sendMessageTo(message, message.dest);

		}

		return sent;
	}

	/**
	 * Send a message to a specific client.
	 *
	 * @param message the message to send.
	 * @param dest    the source of the destination client.
	 * @return true if the message was sent, and false otherwise (i.e. if there was an error).
	 */
	private boolean sendMessageTo(ServerMessage message, String dest)
	{
		if (dest != null && clients.containsKey(dest))
		{
			try
			{
				clients.get(message.dest).writeObject(message);
			} catch (IOException e)
			{
				System.out.println("problem sending the message + " + message + " to " + message.dest);
				listener.stat("problem sending the message + " + message + " to " + message.dest, true);
				return false;
			}
			return true;
		}
		return false;
	}

	/**
	 * Deals with a client Logging out.
	 *
	 * @param logoutMessage The client's message asking to log out.
	 * @return false iff there was an error and the clietn was not disconnected.
	 */
	private synchronized boolean clientLogout(ClientMessage logoutMessage)
	{
		messageReceived(logoutMessage);
		return clientDisconnected(logoutMessage.source);
	}

	/**
	 * Deals with a client disconncting without logging out first.
	 *
	 * @param name the source of the client.
	 * @return true if the disconnection was successful.
	 */
	private synchronized boolean clientDisconnected(String name)
	{
		clients.remove(name);
		sendMessage(ServerMessage.clientLeft(name));
		return true;
	}
}
