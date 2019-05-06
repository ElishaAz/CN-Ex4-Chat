package my_chat.data_transfer;

import my_chat.message_types.ChatMessage;
import my_chat.message_types.IMessage;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Elisha
 */
public class MessageOutput implements AutoCloseable
{
	ObjectOutputStream oos;
	OutputStream s;

	public MessageOutput(OutputStream stream) throws IOException
	{
		s = stream;
		oos = new ObjectOutputStream(stream);
	}

	public void  writeMessage (IMessage message) throws IOException
	{
		oos.writeObject(message);
	}

	public void flush() throws IOException
	{
		oos.flush();
	}

	public void close() throws IOException
	{
		oos.close();
	}
}