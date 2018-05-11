package com.vauff.maunzdiscord.commands;

import com.vauff.maunzdiscord.core.AbstractCommand;
import com.vauff.maunzdiscord.core.Logger;
import com.vauff.maunzdiscord.core.Util;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

public class Stop extends AbstractCommand<MessageReceivedEvent>
{
	@Override
	public void exe(MessageReceivedEvent event) throws Exception
	{
		if (Util.hasPermission(event.getAuthor()))
		{
			Util.msg(event.getChannel(), event.getAuthor(), "Maunz is stopping...");
			Logger.log.info("Maunz is stopping...");
			System.exit(0);
		}
		else
		{
			Util.msg(event.getChannel(), event.getAuthor(), "You do not have permission to use that command");
		}
	}

	@Override
	public String[] getAliases()
	{
		return new String[] { "*stop" };
	}
}