package my_chat.message_types;

/**
 * @author Elisha
 */
public class ClientMessage implements IMessage
{
	public enum Type
	{
		Login, Logout, Message, ListNames
	}

	private Type type;

	/**
	 * The source of this message.
	 */
	public String source;

	/**
	 * Destination of this message. 'null' for everyone.
	 */
	public String dest;

	/**
	 * The message.
	 */
	public String message;

	private ClientMessage(Type type, String source, String dest, String message)
	{
		this.type = type;
		this.source = source;
		this.dest = dest;
		this.message = message;
	}

	public static ClientMessage login(String source)
	{
		return new ClientMessage(Type.Login, source, null, null);
	}

	public static ClientMessage logout(String source)
	{
		return new ClientMessage(Type.Logout, source, null, null);
	}

	public static ClientMessage messageAll(String source, String message)
	{
		return new ClientMessage(Type.Message, source, null, message);
	}

	public static ClientMessage messageOne(String source, String dest, String message)
	{
		return new ClientMessage(Type.Message, source, dest, message);
	}

	public static ClientMessage listNames(String source)
	{
		return new ClientMessage(Type.ListNames, source, null, null);
	}

	public Type getType()
	{
		return type;
	}


	@Override
	public String toString()
	{
		return "ClientMessage{" +
				"type=" + type +
				", source='" + source + '\'' +
				", dest='" + dest + '\'' +
				(message == null ? "" : ", message='" + message + '\'') +
				'}';
	}

	@Override
	public String stringDetail()
	{
		if (type == Type.Login)
			return source + " is logging in";
		else if (type == Type.Logout)
			return source + " is logging out";
		String from = source, to = dest;

		if (source == null)
		{
			from = "Server";
		}

		if (dest == null)
		{
			to = "Everyone";
		}

		return from + " to " + to + ": " + message;
	}
}
