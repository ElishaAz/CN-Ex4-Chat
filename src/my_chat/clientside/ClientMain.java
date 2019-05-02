package my_chat.clientside;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

/**
 * @author Elisha
 */
public class ClientMain
{
	public static void main(String[] args) throws IOException
	{
		var socket = new Socket("127.0.0.1", 59090);
		var in = new Scanner(socket.getInputStream());
		System.out.println("Server response: " + in.nextLine());
	}
}
