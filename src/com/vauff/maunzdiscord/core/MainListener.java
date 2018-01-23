package com.vauff.maunzdiscord.core;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;

import com.vauff.maunzdiscord.commands.*;
import com.vauff.maunzdiscord.features.UptimeTimer;
import com.vauff.maunzdiscord.features.Intelligence;
import com.vauff.maunzdiscord.features.ServerTimer;
import com.vauff.maunzdiscord.features.StatsTimer;

import org.json.JSONArray;
import org.json.JSONObject;

import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionAddEvent;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.obj.IGuild;

public class MainListener
{
	/**
	 * Holds all commands
	 */
	private LinkedList<AbstractCommand<MessageReceivedEvent>> commands = new LinkedList<AbstractCommand<MessageReceivedEvent>>();
	/**
	 * A watch to keep track of the uptime of the bot
	 */
	public static StopWatch uptime = new StopWatch();

	/**
	 * Sets up all commands
	 */
	public MainListener()
	{
		try
		{
			JSONObject json = new JSONObject(Util.getFileContents("config.json"));

			commands.add(new About());
			commands.add(new AccInfo());
			commands.add(new Benchmark());
			commands.add(new Changelog());
			commands.add(new Disable());
			commands.add(new Enable());
			commands.add(new Help());
			commands.add(new IsItDown());
			commands.add(new Map());
			commands.add(new Notify());
			commands.add(new Ping());
			commands.add(new Players());
			commands.add(new Reddit());
			commands.add(new Restart());
			commands.add(new Say());
			commands.add(new Services());
			commands.add(new Source());
			commands.add(new Steam());
			commands.add(new Stop());

			if (!json.getString("databasePassword").equals(""))
			{
				commands.add(new Quote());
			}
			else
			{
				Main.log.warn("The quote command is disabled due to databasePassword not being supplied in the config.json");
			}

			if (!json.getString("cleverbotAPIKey").equals(""))
			{
				commands.add(new Intelligence());
			}
			else
			{
				Main.log.warn("Maunz intelligence is disabled due to cleverbotAPIKey not being supplied in the config.json");
			}
		}
		catch (Exception e)
		{
			Main.log.error("", e);
		}
	}

	@EventSubscriber
	public void onReady(ReadyEvent event)
	{
		try
		{
			List<File> folderList = new ArrayList<File>();

			folderList.add(new File(Util.getJarLocation() + "data/"));
			folderList.add(new File(Util.getJarLocation() + "data/services/"));
			folderList.add(new File(Util.getJarLocation() + "data/guilds/"));
			folderList.add(new File(Util.getJarLocation() + "data/services/server-tracking/"));
			folderList.add(new File(Util.getJarLocation() + "data/services/csgo-updates/"));

			for (File folder : folderList)
			{
				if (!folder.isDirectory())
				{
					folder.mkdir();
				}
			}

			for (IGuild guild : Main.client.getGuilds())
			{
				File file = new File(Util.getJarLocation() + "data/guilds/" + guild.getStringID() + ".json");

				if (!file.exists())
				{
					JSONObject json = new JSONObject();

					file.createNewFile();
					json.put("lastGuildName", guild.getName());
					json.put("blacklist", new JSONArray());
					FileUtils.writeStringToFile(file, json.toString(2), "UTF-8");
				}
			}

			uptime.start();
			Executors.newScheduledThreadPool(1).scheduleAtFixedRate(ServerTimer.timer, 0, 60, TimeUnit.SECONDS);
			Executors.newScheduledThreadPool(1).scheduleWithFixedDelay(UptimeTimer.timer, 600, 60, TimeUnit.SECONDS);
			Executors.newScheduledThreadPool(1).scheduleWithFixedDelay(StatsTimer.timer, 0, 300, TimeUnit.SECONDS);
		}
		catch (Exception e)
		{
			Main.log.error("", e);
		}
	}

	@EventSubscriber
	public void onMessageReceived(MessageReceivedEvent event)
	{
		try
		{
			String cmdName = event.getMessage().getContent().split(" ")[0];

			for (AbstractCommand<MessageReceivedEvent> cmd : commands)
			{
				if (Util.isEnabled || cmd instanceof Enable || cmd instanceof Disable)
				{
					for (String s : cmd.getAliases())
					{
						if (cmdName.equalsIgnoreCase(s))
						{
							JSONObject json = new JSONObject(Util.getFileContents(new File(Util.getJarLocation() + "data/guilds/" + event.getGuild().getStringID() + ".json")));
							boolean blacklisted = false;

							if (!Util.hasPermission(event.getAuthor(), event.getGuild()))
							{
								for (int i = 0; i < json.getJSONArray("blacklist").length(); i++)
								{
									String entry = json.getJSONArray("blacklist").getString(i);

									if (entry.split(":")[0].equalsIgnoreCase(event.getChannel().getStringID()) && (entry.split(":")[1].equalsIgnoreCase(cmdName.replace("*", "")) || entry.split(":")[1].equalsIgnoreCase("all")))
									{
										blacklisted = true;
										break;
									}
								}
							}

							if (!blacklisted)
							{
								cmd.exe(event);
							}
						}
					}

				}
			}

			try
			{
				if (AbstractCommand.AWAITED.containsKey(event.getAuthor().getStringID()) && event.getChannel().equals(Main.client.getMessageByID(Long.parseLong(AbstractCommand.AWAITED.get(event.getAuthor().getStringID()).getID())).getChannel()))
				{
					Main.client.getMessageByID(Long.parseLong(AbstractCommand.AWAITED.get(event.getAuthor().getStringID()).getID())).delete();
					AbstractCommand.AWAITED.get(event.getAuthor().getStringID()).getCommand().onMessageReceived(event);
				}
			}
			catch (NullPointerException e)
			{
				// This means that the message ID in AbstractCommand#AWAITED for the given user ID has already been deleted, we can safely just stop executing
			}
		}
		catch (Exception e)
		{
			Main.log.error("", e);
		}
	}

	@EventSubscriber
	public void onReactionAdd(ReactionAddEvent event)
	{
		try
		{
			if (AbstractCommand.AWAITED.containsKey(event.getMessage().getStringID()) && event.getUser().getStringID().equals(AbstractCommand.AWAITED.get(event.getMessage().getStringID()).getID()))
			{
				event.getMessage().delete();
				AbstractCommand.AWAITED.get(event.getMessage().getStringID()).getCommand().onReactionAdd(event);
			}
		}
		catch (Exception e)
		{
			Main.log.error("", e);
		}
	}
}
