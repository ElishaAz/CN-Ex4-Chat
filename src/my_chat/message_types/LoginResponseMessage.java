package my_chat.message_types;

/**
 * @author Elisha
 */
public class LoginResponseMessage implements IServerMessage
{
	public boolean accepted;
	public boolean nameExists;
	public String name;

	public LoginResponseMessage(boolean accepted, boolean nameExists, String name)
	{
		this.accepted = accepted;
		this.nameExists = nameExists;
		this.name = name;
	}

	@Override
	public String toString()
	{
		if (accepted)
		{
			return "Login accepted as " + name;
		} else
		{
			if (nameExists)
			{
				return "Login was not accepted as " + name + ". That name already exists";
			}else
			{
				return "Login was not accepted as " + name + ". That name is invalid";
			}
		}
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
