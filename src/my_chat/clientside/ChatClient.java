package my_chat.clientside;

import my_chat.data_transfer.MessageInput;
import my_chat.data_transfer.MessageOutput;
import my_chat.message_types.*;
import my_chat.data_transfer.ChatListener;
import my_chat.serverside.ChatServer;

import java.io.*;
import java.net.Socket;
import java.util.regex.Pattern;

/**
 * @author Elisha
 */
public class ChatClient implements Runnable
{
	private String serverAddress;
	@SuppressWarnings("FieldCanBeLocal")
	private MessageInput in;
	private MessageOutput out;

	private String name = "";

	private final ChatListener listener;

	public ChatClient(ChatListener listener, String serverAddress)
	{
		this.listener = listener;
		this.serverAddress = serverAddress;
	}

	public void updateServerAddress(String serverAddress)
	{
		this.serverAddress = serverAddress;
	}

	/**
	 * @param name a new name.
	 */
	public void updateName(String name)
	{
		this.name = name;
		nameUpdated = true;
	}

	/**
	 * Checks if a name is valid. Note: this doe not check if the name is already taken!
	 */
	public boolean isValidName(String name)
	{
		return Pattern.matches(ChatServer.nameRegexPattern, name);
	}

	private boolean nameUpdated = true;

	private boolean running = false;
	private final Object clientRunningLock = new Object();

	/**
	 * Start the Client.
	 * You should run this from a separate thread.
	 * Because this class implements Runnable, you can run:
	 * <pre>
	 * {@code
	 * Thread thread = new Thread(client);
	 * thread.run();
	 * }
	 * </pre>
	 * where 'client' is an instance of this class.
	 */
	@Override
	public void run()
	{
		synchronized (clientRunningLock)
		{
			this.serverAddress = serverAddress;
			try
			{
				var socket = new Socket(serverAddress, ChatServer.port);
				in = new MessageInput(new BufferedInputStream(socket.getInputStream()));
				out = new MessageOutput(new BufferedOutputStream(socket.getOutputStream()));

				running = true;

				// log in
				boolean loggedIn = false;
				while (!loggedIn)
				{
					if (in.available())
					{
						IMessage m = in.readMessage();
						if (m instanceof IServerMessage)
						{
							IServerMessage sm = (IServerMessage) m;
							messageReceived(sm);

							if (sm instanceof LoginResponseMessage)
							{
								LoginResponseMessage am = (LoginResponseMessage) sm;
								if (am.accepted)
								{
									loggedIn = true;
									continue;
								}
							}

							if (nameUpdated && name != null && !name.trim().equals("")) // if the name was updated, try sighing in again
							{
								sendMessage(new LoginMessage(name, true));
								nameUpdated = false;
							}
						} else
						{
							listener.stat("A non-server message was received. Message dropped.", true);
						}
					}
				}


				// send / receive messages
				while (running)
				{
					if (in.available())
					{
						Object obj = in.readMessage();
						if (obj instanceof IServerMessage)
						{
							IServerMessage sm = (IServerMessage) obj;
							messageReceived(sm);
						} else
						{
							listener.stat("A non-server message was received. Message dropped.", true);
						}
					}
				}

			} catch (IOException e)
			{
				listener.stat("IOException. Exiting.", true);
			} catch (ClassNotFoundException e)
			{
				listener.stat("Problem reading from stream. Exiting.", true);
			} finally
			{
				stop();
			}
		}
	}

	/**
	 * Stops the client and logs out.
	 */
	public synchronized void stop()
	{
		if (out != null)
			try
			{
				out.close();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
	}

	/**
	 * @return true if the client is running.
	 */
	public boolean isRunning()
	{
		return running;
	}

	/**
	 * Send a message to all the other users. You can only call this after the client has started.
	 *
	 * @param message the message to send.
	 */
	public synchronized void messageAll(String message)
	{
		sendMessage(new ChatMessage(name, null, message));
	}

	/**
	 * Send a private message to one user. You can only call this after the client has started.
	 *
	 * @param dest    the user to send this message to.
	 * @param message the message to send.
	 */
	public synchronized void messageOne(String dest, String message)
	{
		sendMessage(new ChatMessage(name, dest, message));
	}

	/**
	 * Asks the server to send a list of all the users.
	 */
	public synchronized void listNames()
	{
		sendMessage(new NameRequestMessage(name));
	}

	/**
	 * Deals with outgoing messages.
	 */
	private synchronized void sendMessage(IClientMessage message)
	{
		if (!running)
		{
			listener.stat("Client is not running!", true);
			return;
		}
		try
		{
			out.writeMessage(message);
			out.flush();
			listener.messageSent(message);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Deals with incoming messages.
	 */
	private synchronized void messageReceived(IServerMessage message)
	{
		listener.messageReceived(message);
	}
}
