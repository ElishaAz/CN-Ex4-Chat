package my_chat.clientside;

import my_chat.message_types.ClientMessage;
import my_chat.message_types.ServerMessage;
import my_chat.serverside.ChatListener;
import my_chat.serverside.ChatServer;

import java.io.*;
import java.net.Socket;

/**
 * @author Elisha
 */
public class ChatClient implements Runnable
{
	private String serverAddress;
	@SuppressWarnings("FieldCanBeLocal")
	private ObjectInputStream in;
	private ObjectOutputStream out;

	private String name = "";

	private final ChatListener listener;

	public ChatClient(ChatListener listener, String serverAddress, String name)
	{
		this.listener = listener;
		this.serverAddress = serverAddress;
		this.name = name;
	}

	public void updateServerAddress(String serverAddress)
	{
		this.serverAddress = serverAddress;
	}

	public void updateName(String name)
	{
		this.name = name;
	}

	private boolean isRunning = false;
	private final Object clientRunningLock = new Object();

	public void run()
	{
		synchronized (clientRunningLock)
		{
			this.serverAddress = serverAddress;
			try
			{
				var socket = new Socket(serverAddress, ChatServer.port);
				in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
				out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));

				isRunning = true;

				boolean loggedIn = false;
				while (!loggedIn)
				{
					if (in.available() > 0)
					{
						ServerMessage sm = (ServerMessage) in.readObject();
						listener.messageReceived(sm);
						if (sm.getType() == ServerMessage.Type.LoginResponse)
						{
							if (sm.loginAccepted)
							{
								loggedIn = true;
							}
							// TODO add a check if we logged in already
						} else
						{
							sendMessage(ClientMessage.login(name));
						}
					}
				}

				while (isRunning)
				{
					if (in.available() > 0)
					{
						ServerMessage sm = (ServerMessage) in.readObject();
						listener.messageReceived(sm);
					}
				}

			} catch (IOException e)
			{
				System.err.println("IOException. Exiting.");
				listener.stat("IOException. Exiting.", true);
			} catch (ClassNotFoundException e)
			{
				System.err.println("Problem reading from stream. Exiting.");
				listener.stat("Problem reading from stream. Exiting.", true);
			} finally
			{
				stop();
			}
		}
	}

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

	public synchronized void messageAll(String message)
	{
		sendMessage(ClientMessage.messageAll(name, message));
	}

	public synchronized void messageOne(String dest, String message)
	{
		sendMessage(ClientMessage.messageOne(name, dest, message));
	}

	public synchronized void listNames()
	{
		sendMessage(ClientMessage.listNames(name));
	}

	private synchronized void sendMessage(ClientMessage message)
	{
		if (!isRunning)
		{
			System.err.println("Client is not running!");
			listener.stat("Client is not running!", true);
			return;
		}
		try
		{
			out.writeObject(message);
			out.flush();
			listener.messageSent(message);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private synchronized void messageReceived(ServerMessage message)
	{
		listener.messageReceived(message);
	}
}
