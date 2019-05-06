package my_chat.message_types;

/**
 * @author Elisha
 */
public class InvalidUserMessage implements IServerMessage
{
	private String dest, user;

	public InvalidUserMessage(String dest, String user)
	{
		this.dest = dest;
		this.user = user;
	}

	@Override
	public String getSource()
	{
		return "<server>";
	}

	@Override
	public String getDest()
	{
		return dest;
	}

	@Override
	public String toString()
	{
		return "user " + user + " does not exist";
	}
}
