package my_chat.interfaces.command_line.server;

import my_chat.message_types.IMessage;
import my_chat.data_transfer.ChatListener;
import my_chat.serverside.ChatServer;

import java.util.Scanner;

/**
 * @author Elisha
 */
public class CommandLineServer implements ChatListener
{
	public static void main(String[] args)
	{
		ChatServer server = new ChatServer(new CommandLineServer(), 100);

		Thread thread = new Thread(server);
		thread.start();

		Scanner scanner = new Scanner(System.in);

		while (server.isRunning())
		{
			String input = scanner.nextLine();

			if (input.toLowerCase().startsWith("stop"))
			{
				server.stop();
			} else if (input.toLowerCase().startsWith("broadcast"))
			{
				int i = input.indexOf(':');
				String message = input.substring(i);
				server.broadcast(message);
			}
		}
	}

	@Override
	public void messageReceived(IMessage message)
	{
		System.out.println("Received: " + message.toString());
	}

	@Override
	public void messageSent(IMessage message)
	{
		System.out.println("Sent: " + message.toString());
	}

	@Override
	public void stat(String message, boolean err)
	{
		if (err)
		{
			System.err.println(message);
		} else
		{
			System.out.println("Info: " + message);
		}
	}
}
