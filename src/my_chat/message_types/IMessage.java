package my_chat.message_types;

import java.io.Serializable;

/**
 * @author Elisha
 */
public interface IMessage extends Serializable
{
	String toString();

	String getSource();

	String getDest();
}
