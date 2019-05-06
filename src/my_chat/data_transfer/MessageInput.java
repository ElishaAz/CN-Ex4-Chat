package my_chat.data_transfer;


import my_chat.message_types.ChatMessage;
import my_chat.message_types.IMessage;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Elisha
 */
public class MessageInput extends ObjectInputStream
{

	/**
	 * Creates an ObjectInputStream that reads from the specified InputStream.
	 * A serialization stream header is read from the stream and verified.
	 * This constructor will block until the corresponding ObjectOutputStream
	 * has written and flushed the header.
	 *
	 * <p>The serialization filter is initialized to the value of
	 * {@linkplain ObjectInputFilter.Config#getSerialFilter() the process-wide filter}.
	 *
	 * <p>If a security manager is installed, this constructor will check for
	 * the "enableSubclassImplementation" SerializablePermission when invoked
	 * directly or indirectly by the constructor of a subclass which overrides
	 * the ObjectInputStream.readFields or ObjectInputStream.readUnshared
	 * methods.
	 *
	 * @param in input stream to read from
	 * @throws StreamCorruptedException if the stream header is incorrect
	 * @throws IOException              if an I/O error occurs while reading stream header
	 * @throws SecurityException        if untrusted subclass illegally overrides
	 *                                  security-sensitive methods
	 * @throws NullPointerException     if <code>in</code> is <code>null</code>
	 * @see ObjectInputStream#ObjectInputStream()
	 * @see ObjectInputStream#readFields()
	 * @see ObjectOutputStream#ObjectOutputStream(OutputStream)
	 */
	public MessageInput(InputStream in) throws IOException
	{
		super(in);
	}

	public IMessage readMessage() throws IOException, ClassNotFoundException
	{
		Object obj = readObject();
		if (obj instanceof IMessage)
			return (IMessage) obj;
		else throw new ClassNotFoundException("The object is not a IMessage object");
	}
}
