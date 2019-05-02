package my_chat.message_types;

import java.util.HashSet;

/**
 * A message that the server sends.
 *
 * @author Elisha
 */
public class ServerMessage implements IMessage
{
	public enum Type
	{
		/**
		 * Server is asking the client to send a 'Login' message.
		 */
		LoginRequest,

		/**
		 * Login was accepted by the server
		 */
		LoginResponse,
		/**
		 * a client is sending a message to everyone
		 */
		Message,
		/**
		 * response to a 'ListNames' request
		 */
		Names,
		/**
		 * a broadcast form the server that a client has clientJoined
		 */
		ClientJoined,
		/**
		 * a broadcast form the server that a client has clientLeft
		 */
		ClientLeft
	}

	private Type type;

	/**
	 * The source of this message. 'null' for server
	 */
	public String source;
	/**
	 * The destination of this message. 'null' for everyone.
	 */
	public String dest;

	/**
	 * The message. Null if not in use.
	 */
	public String message;

	/**
	 * A set of the names of all the clients, given in response to a 'ListNames' request. Null if not in use.
	 */
	public HashSet<String> names;

	/**
	 * The source of the client that has joined / left. Null if not in use.
	 */
	public String name;

	/**
	 * The main part of a 'LoginResponse'. Null if not in use.
	 */
	public Boolean loginAccepted;

	/**
	 * Constructor for all.
	 *
	 * @param type          the type of this message.
	 * @param source        the client sending the message.
	 * @param message       the message that is sent.
	 * @param names         a set of all the names.
	 * @param name          source of the client that clientJoined / clientLeft.
	 * @param loginAccepted
	 */
	private ServerMessage(Type type, String source, String dest, String message, HashSet<String> names, String name,
						  Boolean loginAccepted)
	{
		this.type = type;
		this.source = source;
		this.dest = dest;
		this.message = message;
		this.names = names;
		this.name = name;
		this.loginAccepted = loginAccepted;
	}

	public Type getType()
	{
		return type;
	}

	/**
	 * Server is asking the client to send a 'Login' message.
	 */
	public static ServerMessage loginRequest()
	{
		return new ServerMessage(Type.LoginRequest, null, null, null, null, null, null);
	}

	/**
	 * Server's response to a login.
	 */
	public static ServerMessage loginResponse(boolean loginAccepted, String message, String dest)
	{
		return new ServerMessage(Type.LoginResponse, null, dest, message, null, null, loginAccepted);
	}

	/**
	 * A client is sending a message.
	 *
	 * @param source  the source of the client that sent this message.
	 * @param dest    the destination of the message. 'null' for everyone.
	 * @param message the message itself.
	 */
	public static ServerMessage message(String source, String dest, String message)
	{
		return new ServerMessage(Type.Message, source, dest, message, null, null, null);
	}

	/**
	 * Response to a 'ListNames' request
	 *
	 * @param names the set of names
	 */
	public static ServerMessage names(HashSet<String> names, String dest)
	{
		return new ServerMessage(Type.Names, null, dest, null, names, null, null);
	}

	/**
	 * A broadcast from the server that a client has joined.
	 *
	 * @param name the source of the client that has joined
	 */
	public static ServerMessage clientJoined(String name)
	{
		return new ServerMessage(Type.ClientJoined, null, null, null, null, name, null);
	}

	/**
	 * A broadcast from the server that a client has left.
	 *
	 * @param name the source of the client that has left
	 */
	public static ServerMessage clientLeft(String name)
	{
		return new ServerMessage(Type.ClientLeft, null, null, null, null, name, null);
	}

	@Override
	public String toString()
	{
		return "ServerMessage{" + "type: " + type +
				(source == null? "Server" : ", from: '" + source + '\'') +
				(dest == null? "Server" :", to: '" + dest + '\'') +
				(message == null ? "" : ", message: '" + message +'\'') +
				(names == null? "" : ", names: " + names) +
				(name == null ? "" : ", source: " + name) +
				(loginAccepted == null? "" : ", loginAccepted=" + loginAccepted) +
				'}';
	}
}
