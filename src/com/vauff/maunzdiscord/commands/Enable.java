package com.vauff.maunzdiscord.commands;

import com.vauff.maunzdiscord.commands.templates.AbstractCommand;
import com.vauff.maunzdiscord.commands.templates.CommandHelp;
import com.vauff.maunzdiscord.core.Main;
import com.vauff.maunzdiscord.core.Util;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.rest.util.Snowflake;
import org.apache.commons.io.FileUtils;
import org.bson.Document;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Filters.eq;

public class Enable extends AbstractCommand<MessageCreateEvent>
{
	private static HashMap<Snowflake, Snowflake> menuMessages = new HashMap<>();

	@Override
	public void exe(MessageCreateEvent event, MessageChannel channel, User author) throws Exception
	{
		if (!(channel instanceof PrivateChannel))
		{
			boolean guildEnabled = Main.mongoDatabase.getCollection("guilds").find(eq("guildId", event.getGuild().block().getId().asLong())).first().getBoolean("enabled");

			if (Util.hasPermission(author))
			{
				Message m = Util.msg(channel, author, "Please select whether you'd like to enable the bot globally or only in this guild" + System.lineSeparator() + System.lineSeparator() + "**`[1]`**  |  Enable globally" + System.lineSeparator() + "**`[2]`**  |  Enable in guild only");

				waitForReaction(m.getId(), author.getId());
				menuMessages.put(author.getId(), m.getId());
				Util.addNumberedReactions(m, true, 2);

				ScheduledExecutorService msgDeleterPool = Executors.newScheduledThreadPool(1);

				msgDeleterPool.schedule(() ->
				{
					m.delete().block();
					msgDeleterPool.shutdown();
				}, 120, TimeUnit.SECONDS);
			}
			else
			{
				if (!guildEnabled)
				{
					Util.msg(channel, author, "Maunz is now enabled in this guild");
					Main.mongoDatabase.getCollection("guilds").updateOne(eq("guildId", event.getGuild().block().getId().asLong()), new Document("$set", new Document("enabled", true)));
				}
				else
				{
					Util.msg(channel, author, "You silly, I was already enabled in this guild!");
				}
			}
		}
		else if (channel instanceof PrivateChannel && Util.hasPermission(author))
		{
			File file = new File(Util.getJarLocation() + "config.json");
			JSONObject json = new JSONObject(Util.getFileContents(file));

			if (!json.getBoolean("enabled"))
			{
				Util.msg(channel, author, "Maunz is now enabled globally");
				json.put("enabled", true);
				FileUtils.writeStringToFile(file, json.toString(4), "UTF-8");
			}
			else
			{
				Util.msg(channel, author, "You silly, I was already enabled globally!");
			}
		}
		else
		{
			Util.msg(channel, author, "You do not have permission to use that command");
		}
	}

	@Override
	public void onReactionAdd(ReactionAddEvent event, Message message) throws Exception
	{
		if (menuMessages.containsKey(event.getUser().block().getId()) && message.getId().equals(menuMessages.get(event.getUser().block().getId())))
		{
			File file = new File(Util.getJarLocation() + "config.json");
			JSONObject json = new JSONObject(Util.getFileContents(file));
			boolean guildEnabled = Main.mongoDatabase.getCollection("guilds").find(eq("guildId", event.getGuild().block().getId().asLong())).first().getBoolean("enabled");

			if (event.getEmoji().asUnicodeEmoji().get().getRaw().equals("1⃣"))
			{
				if (!json.getBoolean("enabled"))
				{
					Util.msg(event.getChannel().block(), event.getUser().block(), "Maunz is now enabled globally");
					json.put("enabled", true);
					FileUtils.writeStringToFile(file, json.toString(4), "UTF-8");
				}
				else
				{
					Util.msg(event.getChannel().block(), event.getUser().block(), "You silly, I was already enabled globally!");
				}
			}
			else if (event.getEmoji().asUnicodeEmoji().get().getRaw().equals("2⃣"))
			{
				if (!guildEnabled)
				{
					Util.msg(event.getChannel().block(), event.getUser().block(), "Maunz is now enabled in this guild");
					Main.mongoDatabase.getCollection("guilds").updateOne(eq("guildId", event.getGuild().block().getId().asLong()), new Document("$set", new Document("enabled", true)));
				}
				else
				{
					Util.msg(event.getChannel().block(), event.getUser().block(), "You silly, I was already enabled in this guild!");
				}
			}
		}
	}

	@Override
	public String[] getAliases()
	{
		return new String[] { "*enable" };
	}

	@Override
	public BotPermission getPermissionLevel()
	{
		return BotPermission.GUILD_ADMIN;
	}

	@Override
	public CommandHelp[] getHelp()
	{
		return new CommandHelp[] { new CommandHelp("", "Enables Maunz either in a specific guild or globally.") };
	}
}