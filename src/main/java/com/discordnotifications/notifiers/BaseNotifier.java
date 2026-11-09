package com.discordnotifications.notifiers;

import com.discordnotifications.DiscordNotificationsConfig;
import com.discordnotifications.DiscordWebhookService;
import com.discordnotifications.Notification;
import com.discordnotifications.NotificationType;
import com.discordnotifications.SeasonalWorlds;
import com.discordnotifications.Template;
import com.discordnotifications.WorldUtils;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.WorldType;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Every notifier extends this. The webhook service is private on purpose: subclasses build message
 * text and call {@link #send}, and nothing else in the plugin can reach the network.
 */
@Slf4j
public abstract class BaseNotifier
{
	private static final Pattern TAG_REGEXP = Pattern.compile("(<[^>]*>)|(@[^@\\s]+@)");

	@Inject
	protected Client client;

	@Inject
	protected DiscordNotificationsConfig config;

	@Inject
	private DiscordWebhookService webhookService;

	@Inject
	private HeldNotifications heldNotifications;

	public void reset()
	{
	}

	protected final void send(NotificationType type, String template, Map<String, String> placeholders, boolean screenshot)
	{
		send(Notification.builder()
			.type(type)
			.text(template)
			.placeholders(placeholders)
			.screenshot(screenshot)
			.build());
	}

	// The local player exists but has no name for the first ticks after login.
	protected final void send(Notification notification)
	{
		if (!deliver(notification))
		{
			heldNotifications.add(() -> deliver(notification));
		}
	}

	private boolean deliver(Notification notification)
	{
		String localName = playerName();
		if (Strings.isNullOrEmpty(localName))
		{
			return false;
		}

		Set<WorldType> worldTypes = client.getWorldType();
		boolean gridMaster = WorldUtils.isGridMaster(client);
		String prefix = WorldUtils.gameModePrefix(worldTypes, gridMaster);
		if (WorldUtils.isIgnoredWorld(worldTypes, gridMaster) || (!prefix.isEmpty() && config.seasonalWorlds() == SeasonalWorlds.IGNORE))
		{
			log.debug("Dropping notification, ignored world {}", worldTypes);
			return true;
		}

		Map<String, String> values = new HashMap<>(notification.getPlaceholders());
		values.put("name", localName);

		webhookService.send(notification.toBuilder()
			.text(prefix + Template.render(notification.getText(), values))
			// The queue renders this later, when it works out the pet's name
			.namedText(notification.getNamedText() == null ? null : prefix + notification.getNamedText())
			.placeholders(values)
			.build());
		return true;
	}

	protected String playerName()
	{
		Player player = client.getLocalPlayer();
		return player != null ? player.getName() : null;
	}

	public static String sanitize(String text)
	{
		if (text == null)
		{
			return "";
		}

		return TAG_REGEXP.matcher(text.replace("<br>", " "))
			.replaceAll("")
			.replace(' ', ' ')
			.trim();
	}

	// Popup body is "New item: <col=..>Item</col>" or "Task Completed: <col=..>Task</col>".
	static String popupSubject(String bottomText)
	{
		String text = sanitize(bottomText);
		int colon = text.indexOf(':');
		return colon < 0 ? text : text.substring(colon + 1).trim();
	}
}
