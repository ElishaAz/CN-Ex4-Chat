package my_chat.clientside;

import my_chat.message_types.ClientMessage;
import my_chat.message_types.ServerMessage;
import my_chat.serverside.ChatListener;

/**
 * @author Elisha
 */
public interface ClientListener extends ChatListener
{
	void messageReceived (ServerMessage message);
	void messageSent(ClientMessage message);
}
