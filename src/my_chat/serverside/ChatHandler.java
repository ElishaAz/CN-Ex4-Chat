package my_chat.serverside;

import my_chat.data_transfer.ChatCommander;
import my_chat.message_types.IClientMessage;
import my_chat.message_types.LoginMessage;
import my_chat.message_types.LoginRequestMessage;
import my_chat.message_types.LoginResponseMessage;

import java.io.*;
import java.net.Socket;

/**
 * @author Elisha
 */
public class ChatHandler implements Runnable
{
	private String name;
	private final Socket socket;
	private final NewChatServer server;
	@SuppressWarnings("FieldCanBeLocal")
	private ObjectInputStream in;
	@SuppressWarnings("FieldCanBeLocal")
	private ObjectOutputStream out;
	private boolean loggedIn = false;

	ChatHandler(Socket socket, NewChatServer server)
	{
		this.socket = socket;
		this.server = server;
	}

	/**
	 * When an object implementing interface <code>Runnable</code> is used
	 * to create a thread, starting the thread causes the object's
	 * <code>run</code> method to be called in that separately executing
	 * thread.
	 * <p>
	 * The general contract of the method <code>run</code> is that it may
	 * take any action whatsoever.
	 *
	 * @see Thread#run()
	 */
	@Override
	public void run()
	{
		try
		{
			// Important! first call 'socket.getOutputStream()', and only then call 'socket.getInputStream()'!
			out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
			in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));

			while (!loggedIn)
			{
				server.sendOn(new LoginRequestMessage(), out);
				Object obj = in.readObject();
				if (IClientMessage.class.isAssignableFrom(obj.getClass()))
				{
					IClientMessage message = (IClientMessage) obj;

					if (LoginMessage.class.isAssignableFrom(message.getClass()))
					{
						LoginMessage loginMessage = (LoginMessage) message;
						if (loginMessage.login)
						{
							LoginResponseMessage loginResponseMessage = server.login(loginMessage.name);
							server.sendOn(loginResponseMessage, out);
							if (loginResponseMessage.accepted)
							{
								loggedIn = true;
							}
						}
					}
				} else
				{
					server.listener.stat("A non-client message was received. Message dropped. " +
							"Object: " + obj.toString(), true);
				}
			}

			while (loggedIn)
			{
				Object obj = in.readObject();
				if (IClientMessage.class.isAssignableFrom(obj.getClass()))
				{
					IClientMessage message = (IClientMessage) obj;

					boolean loggedOut = server.messageReceived(message);
					if (loggedOut)
						loggedIn = false;
				} else
				{
					server.listener.stat("A non-client message was received. Message dropped. " +
							"Object: " + obj.toString(), true);
				}
			}

		} catch (IOException e)
		{
			server.listener.stat("IOException. Thread dropped. " +
					"Name: '" + (name == null ? "null" : name) + '\'', true);
		} catch (ClassNotFoundException e)
		{
			server.listener.stat("Problem reading from the stream. Thread dropped. " +
					"Name: '" + (name == null ? "null" : name) + '\'', true);
		}
	}
}