package my_chat.message_types;

/**
 * @author Elisha
 */
public class ClientJoinedMessage extends BroadcastMessage
{
	public ClientJoinedMessage(String name)
	{
		super(name + " has logged in");
	}
}
