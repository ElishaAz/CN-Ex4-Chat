package my_chat.message_types;

/**
 * @author Elisha
 */
public class ChatMessage implements IServerMessage,IClientMessage
{
	public String source,dest,message;

	public ChatMessage(String source, String dest, String message)
	{
		if (source == null)
		{
			source = "<server>";
		}
		if (dest == null)
		{
			dest = "<all>";
		}

		this.source = source;
		this.dest = dest;
		this.message = message;
	}

	@Override
	public String toString()
	{
		return source + " to " + dest + ": " + message;
	}

	@Override
	public String getSource()
	{
		return source;
	}

	@Override
	public String getDest()
	{
		return dest;
	}
}
