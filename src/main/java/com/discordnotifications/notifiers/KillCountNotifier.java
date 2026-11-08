package com.discordnotifications.notifiers;

import com.discordnotifications.KillCountService;
import com.discordnotifications.Notification;
import com.discordnotifications.NotificationType;
import com.google.common.collect.ImmutableMap;
import lombok.Value;

import javax.inject.Singleton;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class KillCountNotifier extends BaseNotifier
{
	private static final Pattern TIME_REGEX = Pattern.compile("(?:Duration|time|Subdued in):? (?<time>[\\d:]+(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE);
	private static final Pattern LAP_REGEX = Pattern.compile(" (?:lap|total ticket) ?count is:", Pattern.CASE_INSENSITIVE);
	private static final String NEW_BEST = "(new personal best)";
	private static final String RAID_COMPLETE = "Congratulations - your raid is complete!";
	private static final String TOB = "Theatre of Blood";
	private static final int MAX_WAIT_TICKS = 10;

	private Kill pending;
	private int ticksWaited;

	@Override
	public void reset()
	{
		pending = null;
		ticksWaited = 0;
	}

	public void onGameMessage(String message)
	{
		Kill parsed = parse(message);
		if (parsed != null)
		{
			pending = pending == null ? parsed : pending.merge(parsed);
		}
	}

	// CoX sends its time as a friends chat notification
	public void onFriendsChatNotification(String message)
	{
		if (message.startsWith(RAID_COMPLETE))
		{
			onGameMessage(message);
		}
	}

	public void onGameTick()
	{
		if (pending == null)
		{
			return;
		}

		// A milestone doesn't wait for a time, plenty of bosses never send one
		if (pending.getCount() != null && (pending.getTime() != null || isMilestone(pending.getCount())))
		{
			post(pending);
			reset();
		}
		else if (++ticksWaited > MAX_WAIT_TICKS)
		{
			reset();
		}
	}

	private void post(Kill kill)
	{
		boolean best = kill.isPersonalBest() && config.notifyPersonalBests();
		if (!config.includeKillCounts() || (!best && !isMilestone(kill.getCount())))
		{
			return;
		}

		send(Notification.builder()
			.type(NotificationType.KILL_COUNT)
			.text(best ? config.personalBestMessage() : config.killCountMessage())
			.placeholders(placeholders(kill))
			.screenshot(config.sendKillCountScreenshot())
			.build());
	}

	private boolean isMilestone(int count)
	{
		return isMilestone(count, config.killCountInterval(), config.notifyFirstKill());
	}

	static boolean isMilestone(int count, int interval, boolean notifyFirst)
	{
		return (count == 1 && notifyFirst) || (interval > 0 && count % interval == 0);
	}

	static Kill parse(String message)
	{
		if (message.startsWith("Preparation"))
		{
			return null;
		}

		KillCountService.BossKc kc = KillCountService.parseBoss(message);
		if (kc != null)
		{
			return LAP_REGEX.matcher(message).find() ? null : new Kill(kc.getBoss(), kc.getCount(), null, false);
		}

		// TOB puts the last wave's duration before the raid's time in the same message
		int tob = message.startsWith("Wave") ? message.indexOf(TOB) : -1;
		String text = tob < 0 ? message : message.substring(tob);
		Matcher matcher = TIME_REGEX.matcher(text);
		if (!matcher.find())
		{
			return null;
		}

		return new Kill(tob < 0 ? null : TOB, null, matcher.group("time"), text.toLowerCase(Locale.ROOT).contains(NEW_BEST));
	}

	static Map<String, String> placeholders(Kill kill)
	{
		return ImmutableMap.of(
			"boss", kill.getBoss(),
			"kc", KillCountService.kcText(kill.getCount()),
			"time", kill.getTime() == null ? "" : " in " + kill.getTime());
	}

	@Value
	static class Kill
	{
		String boss;
		Integer count;
		String time;
		boolean personalBest;

		Kill merge(Kill next)
		{
			String mergedBoss = next.boss != null ? next.boss : boss;
			// TOB sends the challenge time first and then a total time, the first is the one that counts
			boolean keepTime = next.time == null || (time != null && mergedBoss != null && mergedBoss.startsWith(TOB));
			return new Kill(mergedBoss,
				next.count != null ? next.count : count,
				keepTime ? time : next.time,
				keepTime ? personalBest : next.personalBest);
		}
	}
}
