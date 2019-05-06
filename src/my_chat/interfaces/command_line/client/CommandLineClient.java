package my_chat.interfaces.command_line.client;

import my_chat.clientside.ChatClient;
import my_chat.message_types.IMessage;
import my_chat.message_types.LoginRequestMessage;
import my_chat.message_types.LoginResponseMessage;
import my_chat.data_transfer.ChatListener;

import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * @author Elisha
 */
public class CommandLineClient implements ChatListener
{
	private static final String ipRegex = "\\b" +
			"(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\." +
			"(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\." +
			"(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\." +
			"(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\b";

	private static boolean hasName = false;

	public static void main(String[] args)
	{
		if (args.length < 1)
		{
			System.err.println("You must enter the IP address of the server as the first argument.");
			return;
		}
		if (!(Pattern.matches(ipRegex, args[0]) || args[0].equals("localhost")))
		{
			System.err.println("The first argument must be an IP address (IPv4 or 'localhost').");
			return;
		}

		ChatClient client = new ChatClient(new CommandLineClient(), args[0]);

		Thread thread = new Thread(client);
		thread.start();
		client.updateName("Name");

		Scanner scanner = new Scanner(System.in);
//		System.out.println("System: Enter name:");
//		while (!hasName)
//		{
//			if (!thread.isAlive())
//				return;
//
//			String input = scanner.nextLine().trim();
//			if (client.isValidName(input))
//			{
//				System.out.println("System: Name '" + input + "' is valid");
//				client.updateName(input);
//			} else
//			{
//				System.out.println("System: Enter name:");
//			}
//		}

		while (client.isRunning())
		{
			String input = scanner.nextLine().trim();
			if (input.toLowerCase().startsWith("message all"))
			{
				int i = input.indexOf(':');
				String message = input.substring(i + 1).trim();
				client.messageAll(message);
			} else if (input.toLowerCase().startsWith("message"))
			{
				int i = input.indexOf(':');
				String dest = input.substring(7, i).trim();
				String message = input.substring(i + 1).trim();
				client.messageOne(dest, message);
			} else if (input.toLowerCase().startsWith("list"))
			{
				client.listNames();
			} else if (input.toLowerCase().startsWith("logout"))
			{
				client.stop();
			} else
			{
				System.err.println("System: Invalid Syntax");
			}
		}

	}

	@Override
	public synchronized void messageReceived(IMessage message)
	{
		if (message instanceof LoginResponseMessage)
		{
			LoginResponseMessage lrm = (LoginResponseMessage) message;
			if (lrm.accepted)
			{
				System.out.println("System: Logged in as " + lrm.name);
				hasName = true;
			} else
			{
				System.out.println("System: " + lrm.toString());
				hasName = false;
			}
		} else if (message instanceof LoginRequestMessage)
		{
//			hasName = false;
//			System.out.println("System: Enter name: ");
		} else
		{
			System.out.println("Received: " + message.toString());
		}
	}

	@Override
	public synchronized void messageSent(IMessage message)
	{
		System.out.println("Sent: " + message.toString());
	}

	@Override
	public synchronized void stat(String message, boolean err)
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
