package my_chat.data_transfer;

import my_chat.message_types.ChatMessage;
import my_chat.message_types.IMessage;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Elisha
 */
public class MessageOutput extends ObjectOutputStream
{

	/**
	 * Creates an ObjectOutputStream that writes to the specified OutputStream.
	 * This constructor writes the serialization stream header to the
	 * underlying stream; callers may wish to flush the stream immediately to
	 * ensure that constructors for receiving ObjectInputStreams will not block
	 * when reading the header.
	 *
	 * <p>If a security manager is installed, this constructor will check for
	 * the "enableSubclassImplementation" SerializablePermission when invoked
	 * directly or indirectly by the constructor of a subclass which overrides
	 * the ObjectOutputStream.putFields or ObjectOutputStream.writeUnshared
	 * methods.
	 *
	 * @param out output stream to write to
	 * @throws IOException          if an I/O error occurs while writing stream header
	 * @throws SecurityException    if untrusted subclass illegally overrides
	 *                              security-sensitive methods
	 * @throws NullPointerException if <code>out</code> is <code>null</code>
	 * @see ObjectOutputStream#ObjectOutputStream()
	 * @see ObjectOutputStream#putFields()
	 * @see ObjectInputStream#ObjectInputStream(InputStream)
	 * @since 1.4
	 */
	public MessageOutput(OutputStream out) throws IOException
	{
		super(out);
	}

	public void writeMessage(IMessage message) throws IOException
	{
		writeObject(message);
	}
}