package my_chat.message_types;

/**
 * @author Elisha
 */
public interface IMessage
{
	String toString();

	String getSource();

	String getDest();
}
