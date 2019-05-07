package my_chat.serverside;

import my_chat.data_transfer.ChatListener;
import my_chat.data_transfer.MessageInput;
import my_chat.data_transfer.MessageOutput;
import my_chat.message_types.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * some code from https://cs.lmu.edu/~ray/notes/javanetexamples/
 *
 * @author Elisha
 */
public class ChatServer implements Runnable
{
	public static final int port = 59101;

	public static final String nameRegexPattern = "\\w+";

	private final Map<String, ObjectOutputStream> clients = new HashMap<>();
	public final List<IMessage> allMessages = new LinkedList<>();
	private ExecutorService pool;

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
	 * thread.start();
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

			pool = Executors.newFixedThreadPool(numberOfThreads);

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
		listener.stat("Server stopping ...", false);

		closeAndClearClients();
		allMessages.clear();
		try
		{
			pool.awaitTermination(1000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		runnning = false;

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
	 * Send a send from the server to all the clients.
	 *
	 * @param message the send to send.
	 */
	public void broadcast(String message)
	{
		if (!runnning)
		{
			listener.stat("Server has not started yet!", true);
		} else
		{
			sendMessage(new BroadcastMessage(message));
		}
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
		private ObjectInputStream in;
		private ObjectOutputStream out;

		Handler(Socket socket)
		{
			this.socket = socket;
		}

		@Override
		public void run()
		{
			try
			{
				out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
				in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));

				IMessage loginRequestMessage = new LoginRequestMessage();

				out.writeObject(loginRequestMessage);
				out.flush();

				listener.messageSent(loginRequestMessage);

				// get login info
				boolean loginAccepted = false;
				while (!loginAccepted)
				{
					Object obj = in.readObject();
					if (obj instanceof IClientMessage)
					{
						IClientMessage message = (IClientMessage) obj;
						messageReceived(message);
						if (message instanceof LoginMessage)
						{
							loginAccepted = clientConnected(message, out);

							if (loginAccepted)
							{
								name = message.getSource();
							}
						} else
						{
							loginRequestMessage = new LoginRequestMessage();

							out.writeObject(loginRequestMessage);
							out.flush();

							listener.messageSent(loginRequestMessage);
						}
					} else
					{
						listener.stat("A non-client send was received. Message dropped.", true);
					}

				}

				// receive messages from the client.
				boolean logout = false;
				while (!logout)
				{
					Object obj = in.readObject();
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
						listener.stat("A non-client send was received. Message dropped.", true);
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
	 * @param loginMessage the client's send asking to log in.
	 * @param out          the output stream to the client.
	 * @return true if the login is valid and false otherwise.
	 */
	private synchronized boolean clientConnected(IClientMessage loginMessage, ObjectOutputStream out)
	{
		boolean accepted, nameExists;

		if (loginMessage.getSource() == null || clients.containsKey(loginMessage.getSource()))
		{
			accepted = false;
			nameExists = true;
		} else
		{
			if (isValidName(loginMessage.getSource()))
			{
				clients.put(loginMessage.getSource(), out);
				accepted = true;
				nameExists = false;
			} else
			{
				accepted = false;
				nameExists = false;
			}
		}

		try
		{
			out.writeObject(new LoginResponseMessage(accepted, nameExists, loginMessage.getSource()));
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
	 * @param message the incoming send.
	 */
	private void messageReceived(IClientMessage message)
	{

		allMessages.add(message);
		listener.messageReceived(message);

		if (message instanceof NameRequestMessage)
		{
			sendMessage(new NameListMessage(message.getSource(), new HashSet<>(clients.keySet())));
		} else if (message instanceof IServerMessage)
		{
			if (message.getDest() != null)
			{
				if (!clients.containsKey(message.getDest()))
				{
					sendMessage(new InvalidUserMessage(message.getSource(), message.getDest()));
					return;
				}
			}
			sendMessage(message);
		}
	}

	/**
	 * Sends a send to whoever needs to receive it.
	 *
	 * @param message the send to send.
	 * @return true if the send was sent, and false otherwise (i.e. if there was an error).
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
	 * Send a send to a specific client.
	 *
	 * @param message the send to send.
	 * @param dest    the source of the destination client.
	 * @return true if the send was sent, and false otherwise (i.e. if there was an error).
	 */
	private boolean sendMessageTo(IMessage message, String dest)
	{
		if (dest != null && clients.containsKey(dest))
		{
			try
			{
				var out = clients.get(message.getDest());
				out.writeObject(message);
				out.flush();

			} catch (IOException e)
			{
				listener.stat("problem sending the send + " + message + " to " + message.getDest(), true);
				return false;
			}
			return true;
		}
		return false;
	}

	/**
	 * Deals with a client Logging out.
	 *
	 * @param logoutMessage The client's send asking to log out.
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
