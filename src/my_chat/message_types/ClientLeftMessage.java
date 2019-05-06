package my_chat.message_types;

/**
 * @author Elisha
 */
public class ClientLeftMessage extends BroadcastMessage
{
	public ClientLeftMessage(String name)
	{
		super(name + " has left");
	}
}
