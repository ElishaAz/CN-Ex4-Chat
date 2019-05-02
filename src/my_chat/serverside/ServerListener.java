package my_chat.serverside;


import my_chat.message_types.ClientMessage;
import my_chat.message_types.ServerMessage;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventListener;

/**
 * @author Elisha
 */
public interface ServerListener extends ChatListener
{
	void messageReceived (ClientMessage message);
	void messageSent(ServerMessage message);

	void stat(String message, boolean err);
}
