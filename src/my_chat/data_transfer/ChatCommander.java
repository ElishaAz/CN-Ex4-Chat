package my_chat.data_transfer;

import my_chat.message_types.IMessage;

import java.util.EventListener;

/**
 * @author Elisha
 */
public interface ChatCommander extends EventListener
{
	Object start();

	boolean send(IMessage message);

	void stop();

	boolean isRunning();
}
