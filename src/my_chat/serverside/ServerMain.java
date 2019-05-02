package my_chat.serverside;

/**
 * @author Elisha
 */
public class ServerMain
{
	public static void main(String[] args)
	{
		new OldChatServer(59090).start();
	}
}
