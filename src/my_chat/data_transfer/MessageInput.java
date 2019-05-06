package my_chat.data_transfer;


import my_chat.message_types.ChatMessage;
import my_chat.message_types.IMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Elisha
 */
public class MessageInput
{
	ObjectInputStream ois;
	InputStream s;

	public MessageInput(InputStream stream) throws IOException
	{
		s = stream;
		ois = new ObjectInputStream(stream);
	}

	public boolean available() throws IOException
	{
		return ois.available() > 0;
	}

	public IMessage readMessage() throws IOException, ClassNotFoundException
	{
		Object obj = ois.readObject();
		if (obj instanceof IMessage)
			return (IMessage) obj;
		else throw new ClassNotFoundException("The object is not a IMessage object");
	}
}
