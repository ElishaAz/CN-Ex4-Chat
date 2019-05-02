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
				(message == null? "" : ", message='" + message + '\'') +
				'}';
	}
}
