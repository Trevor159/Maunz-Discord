package com.vauff.maunzdiscord.commands;

import com.vauff.maunzdiscord.commands.templates.AbstractCommand;
import com.vauff.maunzdiscord.commands.templates.CommandHelp;
import com.vauff.maunzdiscord.core.Main;
import com.vauff.maunzdiscord.core.Util;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.util.Snowflake;

public class Say extends AbstractCommand<MessageCreateEvent>
{
	@Override
	public void exe(MessageCreateEvent event, MessageChannel channel, User author) throws Exception
	{
		String[] args = event.getMessage().getContent().split(" ");

		if (channel instanceof PrivateChannel)
		{
			Util.msg(channel, author, "This command can't be done in a PM, only in a guild in which you have the administrator permission in");
			return;
		}

		if (args.length == 1)
		{
			Util.msg(channel, author, "I need a message to send! **Usage: *say [channel] <message>**");
			return;
		}

		if (args[1].startsWith("<#"))
		{
			if (args.length == 2)
			{
				Util.msg(channel, author, "I need a message to send! **Usage: *say [channel] <message>**");
				return;
			}

			GuildChannel sendChannel;

			try
			{
				sendChannel = (GuildChannel) Main.gateway.getChannelById(Snowflake.of(args[1].replaceAll("[^\\d.]", ""))).block();
			}
			catch (ClientException e)
			{
				Util.msg(channel, author, "Failed to send message, that channel is in another guild!");
				return;
			}

			if (!sendChannel.getGuild().block().equals(event.getGuild().block()))
			{
				Util.msg(channel, author, "Failed to send message, that channel is in another guild!");
				return;
			}

			if (Util.msg((MessageChannel) sendChannel, Util.addArgs(args, 2)) != null)
			{
				if (!sendChannel.equals(channel))
					Util.msg(channel, author, "Successfully sent message!");
			}
			else
			{
				Util.msg(channel, author, "Failed to send message, the bot doesn't have permissions for this channel");
			}
		}
		else
		{
			Util.msg(channel, Util.addArgs(args, 1));
		}
	}

	@Override
	public String[] getAliases()
	{
		return new String[] { "*say" };
	}

	@Override
	public BotPermission getPermissionLevel()
	{
		return BotPermission.GUILD_ADMIN;
	}

	@Override
	public CommandHelp[] getHelp()
	{
		return new CommandHelp[] { new CommandHelp("[channel] <message>", "Makes Maunz say whatever you want her to.") };
	}
}