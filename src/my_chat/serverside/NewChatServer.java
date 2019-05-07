package my_chat.serverside;

import my_chat.data_transfer.ChatListener;
import my_chat.data_transfer.ChatCommander;
import my_chat.message_types.*;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * @author Elisha
 */
public class NewChatServer
{
	// ****************************** Static final fields ******************************
	/**
	 * The server's port
	 */
	@SuppressWarnings("WeakerAccess")
	public static final int port = 56101;
	/**
	 * A string pattern all the names have to follow.
	 * <p>
	 * Examples:
	 * <ul>
	 * <li>{@code "\\w+"} for a word (a string with upper- / lower-case characters, and underscores('_'))
	 * <li>{@code "\\w+@\\w+\.\\w+"} for an EMail address (i.e. name@example.com)
	 * </ul>
	 */
	@SuppressWarnings("WeakerAccess")
	public static final String nameRegex = "\\w+";

	// ****************************** Non-static final fields ******************************

	/**
	 * The number of online clients the server can hold
	 */
	private final int threadCount;
	/**
	 * All the users and their output streams for sending messages.
	 */
	private final Map<String, ObjectOutputStream> users = new HashMap<>();

	/**
	 * The listener given by the user interface. This field is package-private because it is used in the handlers.
	 */
	final ChatListener listener; //used in the handler too

	/**
	 * A lock so that the server will not be started again while it is running
	 */
	private final Object serverRunningLock = new Object();
	/**
	 * A lock so that the server will not be started again while it is running
	 */
	private final Object serverStoppingLock = new Object();

	// ****************************** Non-static non-final fields ******************************

	/**
	 * The thread pool of the handler threads.
	 */
	@SuppressWarnings("FieldCanBeLocal")
	private ExecutorService pool;
	/**
	 * The server's running status.
	 */
	private boolean running = false;


	public NewChatServer(ChatListener listener, int threadCount)
	{
		this.threadCount = threadCount;
		this.listener = listener;
	}

	public final ChatCommander chatCommander = new ChatCommander()
	{
		/**
		 * Starts the server in a separate thread.
		 * @return the thread the server was started in.
		 */
		@Override
		public synchronized Thread start()
		{
			Thread thread = new Thread(NewChatServer.this::start);
			thread.start();
			return thread;
		}

		/**
		 * Send a send.
		 * @param message the send to send.
		 * @return false if the send is not one the server can send.
		 */
		@Override
		public synchronized boolean send(IMessage message)
		{
			if (message.getClass().equals(IServerMessage.class))
			{
				NewChatServer.this.send(message);
				return true;
			}
			return false;
		}

		/**
		 * Stops the server.
		 */
		@Override
		public synchronized void stop()
		{
			NewChatServer.this.stop();
		}

		/**
		 * @return true if the server is running, i.e. returns true if {@link NewChatServer#chatCommander#start}
		 * was called, but {@link NewChatServer#chatCommander#stop} was not called yet.
		 */
		@Override
		public synchronized boolean isRunning()
		{
			return NewChatServer.this.isRunning();
		}
	};

	// ****************************** Called by the ChatCommander ******************************

	/**
	 * Start the server. call this from a new thread.
	 */
	private void start()
	{
		synchronized (serverRunningLock)
		{
			running = true;
			listener.stat("Server is running...", false);

			pool = Executors.newFixedThreadPool(threadCount);

			try (var listener = new ServerSocket(port))
			{
				while (running)
				{
					pool.execute(new ChatHandler(listener.accept(), this));
				}
			} catch (IOException e)
			{
				e.printStackTrace();
			} finally
			{
				stop();
			}

			synchronized (serverStoppingLock)
			{
				try
				{
					serverRunningLock.wait();
				} catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Send a send.
	 */
	private void send(IMessage message)
	{

		if (message.getDest() == null || message.getDest().equals("<all>"))
		{
			// destination is everyone, send to every user
			for (var user : users.values())
			{
				sendOn(message, user);
			}
		} else
		{
			if (users.containsKey(message.getDest()))
			{
				// destination found, send message
				sendOn(message, users.get(message.getDest()));
			} else
			{
				// destination not found, tell the sender
				sendOn(new InvalidUserMessage(message.getSource(), message.getDest()),
						users.get(message.getSource()));
			}
		}

	}

	/**
	 * Stop the server.
	 */
	private void stop()
	{
		listener.stat("Server stopping ...", false);
		running = false;
		synchronized (serverStoppingLock)
		{
			for (var user : users.keySet())
			{
				removeUser(user);
			}

			users.clear();

			pool.shutdownNow();
			try
			{
				pool.awaitTermination(1, TimeUnit.SECONDS);
			} catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			serverStoppingLock.notifyAll();
			listener.stat("Server stopped", false);
		}
	}

	/**
	 * @return true if the server is running, i.e. returns true if {@link NewChatServer#chatCommander#start}
	 * was called, but {@link NewChatServer#chatCommander#stop} was not called yet.
	 */
	private boolean isRunning()
	{
		return running;
	}

	// ****************************** Called by the ChatHandler ******************************

	void sendOn(IMessage message, ObjectOutputStream stream)
	{
		try
		{
			stream.writeObject(message);
			stream.flush();
		} catch (IOException e)
		{
			listener.stat("Problem sending message. Message: " + message, true);
		}
	}

	/**
	 * A client logging in.
	 * This function is called from a handler.
	 *
	 * @param name the name the client wants to log in as
	 * @return a {@link LoginResponseMessage} object with the login info (i.e. if it was rejected, and why).
	 * Send this to the client
	 */
	synchronized LoginResponseMessage login(String name, ObjectOutputStream out)
	{
		if (!isValidName(name))
		{
			return new LoginResponseMessage(false, false, name);
		} else if (users.containsKey(name))
		{
			return new LoginResponseMessage(false, true, name);
		} else
		{
			users.put(name, out);
			return new LoginResponseMessage(true, false, name);
		}
	}

	/**
	 * Tells the server that a message was received.
	 *
	 * @param message the message that was received.
	 * @return true if the message was a logout message, and the client was logged out.
	 */
	synchronized boolean messageReceived(IMessage message)
	{
		listener.messageReceived(message);
		if (message.getClass().equals(LoginMessage.class))
		{
			LoginMessage loginMessage = (LoginMessage) message;
			if (!loginMessage.login)
			{
				removeUser(loginMessage.getSource());
				return true;
			}
		}
		return false;
	}

	/**
	 * Tells the server that a client was disconnected from the handler.
	 *
	 * @param name the name of the client that was disconnected.
	 */
	void disconnected(String name)
	{
		if (running)
		{
			listener.stat("User '" + name + "' has been disconnected", true);
			removeUser(name);
		}
	}

	// ****************************** Other methods ******************************

	/**
	 * Completely removes a client from the messaging list.
	 *
	 * @param name the name of the client to be removed.
	 */
	private void removeUser(String name)
	{
		if (users.containsKey(name))
		{
			ObjectOutputStream stream = users.get(name);
			users.remove(name);
			try
			{
				stream.close();
			} catch (IOException e)
			{
				listener.stat("Problem closing stream of user '" + name + '\'', true);
			}
		} else
		{
			listener.stat("Cannot remove user '" + name + "'. user does not exit.", true);
		}
	}

	/**
	 * Checks if name is a valid name for a client. Note: this does not check if the name is taken!
	 *
	 * @param name the name to be checked
	 * @return true if the name is a valid name for a client
	 */
	@SuppressWarnings("WeakerAccess")
	public static boolean isValidName(String name)
	{
		return Pattern.matches(nameRegex, name);
	}
}
