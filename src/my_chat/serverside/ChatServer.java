package my_chat.serverside;

import my_chat.data_transfer.ChatListener;
import my_chat.data_transfer.MessageInput;
import my_chat.data_transfer.MessageOutput;
import my_chat.message_types.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * some code from https://cs.lmu.edu/~ray/notes/javanetexamples/
 *
 * @author Elisha
 */
public class ChatServer implements Runnable
{
	public static final int port = 59101;

	private static final String nameExistsMessage = "That name already exists. Try a different one",
			nameInvalidMessage = "That name is invalid. Try a different one",
			loginAcceptedMessage = "Login Accepted with source: ";

	public static final String nameRegexPattern = "\\w";

	private final Map<String, MessageOutput> clients = new HashMap<>();

	public final List<IMessage> allMessages = new LinkedList<>();

	private final int numberOfThreads;
	private final ChatListener listener;

	/**
	 * Construct a server.
	 *
	 * @param listener        a listener that is called when messages are sent / received
	 * @param numberOfThreads the maximum number of online clients this server can hold.
	 */
	public ChatServer(ChatListener listener, int numberOfThreads)
	{
		this.numberOfThreads = numberOfThreads;
		this.listener = listener;
	}

	private boolean runnning = true;
	private final Object serverRunningLock = new Object();

	/**
	 * Start the Server.
	 * You should run this from a separate thread.
	 * Because this class implements Runnable, you can run:
	 * <pre>
	 * {@code
	 * Thread thread = new Thread(server);
	 * thread.run();
	 * }
	 * </pre>
	 * where 'server' is an instance of this class.
	 */
	@Override
	public void run()
	{
		synchronized (serverRunningLock)
		{
			runnning = true;
			listener.stat("Server is running...", false);

			var pool = Executors.newFixedThreadPool(numberOfThreads);

			try (var listener = new ServerSocket(port))
			{
				while (runnning)
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
		runnning = false;

		listener.stat("Server stopping ...", false);

		closeAndClearClients();
		allMessages.clear();
		listener.stat("Server stopped", false);
	}

	/**
	 * @return true if the server is running.
	 */
	public boolean isRunning()
	{
		return runnning;
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
				listener.stat("Stream " + stream + " was already closed", true);
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
		private MessageInput in;
		private MessageOutput out;

		Handler(Socket socket)
		{
			this.socket = socket;
		}

		@Override
		public void run()
		{
			try
			{
				in = new MessageInput(new BufferedInputStream(socket.getInputStream()));
				out = new MessageOutput(new BufferedOutputStream(socket.getOutputStream()));


				// get login info
				boolean loginAccepted = false;
				while (!loginAccepted)
				{
					if (in.available())
					{
						IMessage m = in.readMessage();
						if (m instanceof IClientMessage)
						{
							IClientMessage message = (IClientMessage) m;
							if (message instanceof LoginMessage)
							{
								loginAccepted = clientConnected(message, out);

								if (loginAccepted)
								{
									name = message.getSource();
								}
							}
						} else
						{
							listener.stat("A non-client message was received. Message dropped.", true);
						}
					} else
					{
						out.writeMessage(new LoginRequestMessage());
						out.flush();
					}
				}

				// receive messages from the client.
				boolean logout = false;
				while (!logout)
				{
					if (in.available())
					{
						Object obj = in.readMessage();
						if (obj instanceof IClientMessage)
						{
							IClientMessage message = (IClientMessage) obj;
							if (message instanceof LoginMessage)
							{
								logout = clientLogout(message);
							} else
							{
								messageReceived(message);
							}
						} else
						{
							listener.stat("A non-client message was received. Message dropped.", true);
						}
					}
				}

			} catch (IOException e)
			{
				listener.stat("IOException. Thread dropped. Name: " + (name == null ? "'null'" : name), true);
				clientDisconnected(name);
			} catch (ClassNotFoundException e)
			{
				listener.stat("Problem reading from stream. Thread dropped. Name: " + (name == null ? "'null'" : name)
						, true);
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
	private synchronized boolean clientConnected(IClientMessage loginMessage, MessageOutput out)
	{
		messageReceived(loginMessage);

		boolean accepted, nameExists;
		String message;

		if (loginMessage.getSource() == null || clients.containsKey(loginMessage.getSource()))
		{
			message = nameExistsMessage;
			accepted = false;
			nameExists = true;
		} else
		{
			if (isValidName(loginMessage.getSource()))
			{
				clients.put(loginMessage.getSource(), out);
				message = loginAcceptedMessage + loginMessage.getSource();
				accepted = true;
				nameExists = false;
			} else
			{
				message = nameInvalidMessage;
				accepted = false;
				nameExists = false;
			}
		}

		try
		{
			out.writeMessage(new LoginResponseMessage(accepted, nameExists, loginMessage.getSource()));
		} catch (IOException e)
		{
			listener.stat("Problem writing to new login: " + loginMessage.getSource() + ". login is rejected.", true);
			accepted = false;
			clients.remove(loginMessage.getSource());
		}

		if (accepted)
		{
			sendMessage(new ClientJoinedMessage(loginMessage.getSource()));
		}

		return accepted;
	}

	/**
	 * Checks if the name the client gave is valid.
	 *
	 * @param name the name the client gave.
	 */
	private boolean isValidName(String name)
	{
		return Pattern.matches(nameRegexPattern, name);
	}

	/**
	 * Deals with incoming messages.
	 *
	 * @param message the incoming message.
	 */
	private void messageReceived(IClientMessage message)
	{
		if (message.getDest() != null)
		{
			if (!clients.containsKey(message.getDest()))
			{
				sendMessage(new InvalidUserMessage(message.getSource(), message.getDest()));
				return;
			}
		}

		allMessages.add(message);
		listener.messageReceived(message);

		if (message instanceof NameRequestMessage)
		{
			sendMessage(new NameListMessage(message.getSource(), new HashSet<>(clients.keySet())));
		} else if (message instanceof IServerMessage)
		{
			sendMessage(message);
		}
	}

	/**
	 * Sends a message to whoever needs to receive it.
	 *
	 * @param message the message to send.
	 * @return true if the message was sent, and false otherwise (i.e. if there was an error).
	 */
	private synchronized boolean sendMessage(IMessage message)
	{
		boolean sent = true;

		allMessages.add(message);
		listener.messageSent(message);

		if (message.getDest() == null)
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
			sent = sendMessageTo(message, message.getDest());

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
	private boolean sendMessageTo(IMessage message, String dest)
	{
		if (dest != null && clients.containsKey(dest))
		{
			try
			{
				var out = clients.get(message.getDest());
				out.writeMessage(message);
				out.flush();

			} catch (IOException e)
			{
				listener.stat("problem sending the message + " + message + " to " + message.getDest(), true);
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
	private synchronized boolean clientLogout(IClientMessage logoutMessage)
	{
		messageReceived(logoutMessage);
		return clientDisconnected(logoutMessage.getSource());
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
		sendMessage(new ClientLeftMessage(name));
		return true;
	}
}
