package my_chat.message_types;

/**
 * @author Elisha
 */
public class LoginRequestMessage implements IServerMessage
{
	@Override
	public String toString()
	{
		return "Server is asking you to log in";
	}

	@Override
	public String getSource()
	{
		return "<server>";
	}

	@Override
	public String getDest()
	{
		return "<new_client>";
	}
}
