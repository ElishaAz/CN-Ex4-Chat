package my_chat.message_types;

/**
 * @author Elisha
 */
public class NameRequestMessage implements IClientMessage
{
	public String source;

	public NameRequestMessage(String source)
	{
		this.source = source;
	}

	@Override
	public String toString()
	{
		return source + " is requesting a list of all clients";
	}

	@Override
	public String getSource()
	{
		return source;
	}

	@Override
	public String getDest()
	{
		return "<server>";
	}
}
