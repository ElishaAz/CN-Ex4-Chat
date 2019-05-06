package my_chat.message_types;

/**
 * @author Elisha
 */
public class LoginMessage implements IClientMessage
{
	public String name;
	public boolean login;

	public LoginMessage(String name, boolean login)
	{
		this.name = name;
		this.login = login;
	}

	@Override
	public String toString()
	{
		if (login)
		{
			return "Logging in as " + name;
		} else
		{
			return name + " is logging out";
		}
	}

	@Override
	public String getSource()
	{
		return name;
	}

	@Override
	public String getDest()
	{
		return "<server>";
	}
}
