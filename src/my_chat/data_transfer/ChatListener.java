package my_chat.data_transfer;

import my_chat.message_types.IMessage;

import java.util.EventListener;

/**
 * @author Elisha
 */
public interface ChatListener extends EventListener
{
	void messageReceived (IMessage message);
	void messageSent(IMessage message);
	void stat(String message, boolean err);
}
