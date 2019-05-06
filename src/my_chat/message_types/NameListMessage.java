package my_chat.message_types;

import java.util.HashSet;

/**
 * @author Elisha
 */
public class NameListMessage implements IServerMessage
{
	String dest;

	HashSet<String> names;

	public NameListMessage(String dest, HashSet<String> names)
	{
		this.dest = dest;
		this.names = names;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder("All clients: ");

		for (var name : names)
		{
			sb.append(name);
			sb.append(' ');
		}

		return sb.toString();
	}

	@Override
	public String getSource()
	{
		return "<server>";
	}

	@Override
	public String getDest()
	{
		return dest;
	}
}
