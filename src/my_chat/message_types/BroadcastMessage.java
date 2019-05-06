package my_chat.message_types;

/**
 * @author Elisha
 */
public class BroadcastMessage implements IMessage
{
	public String message;

	public BroadcastMessage(String message)
	{
		this.message = message;
	}

	@Override
	public String toString()
	{
		return "<" + message + ">";
	}

	@Override
	public String getSource()
	{
		return "<server>";
	}

	@Override
	public String getDest()
	{
		return "<all>";
	}
}
