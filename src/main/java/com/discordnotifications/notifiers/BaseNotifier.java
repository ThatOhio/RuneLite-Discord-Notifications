package com.discordnotifications.notifiers;

import com.discordnotifications.DiscordNotificationsConfig;
import com.discordnotifications.DiscordWebhookService;
import com.discordnotifications.Notification;
import com.discordnotifications.Template;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
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

	public void reset()
	{
	}

	// The local player exists but has no name for the first ticks after login.
	protected final void send(Notification notification)
	{
		String localName = playerName();
		if (Strings.isNullOrEmpty(localName))
		{
			log.debug("Dropping notification, local player name not available yet");
			return;
		}

		Map<String, String> values = new HashMap<>(notification.getPlaceholders());
		values.put("name", localName);

		webhookService.send(notification.toBuilder()
			.text(Template.render(notification.getText(), values))
			.placeholders(values)
			.build());
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
