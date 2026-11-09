package com.discordnotifications.notifiers;

import com.discordnotifications.KillCountService;
import com.discordnotifications.Notification;
import com.discordnotifications.NotificationType;
import com.google.common.collect.ImmutableMap;
import lombok.Value;
import net.runelite.api.gameval.VarbitID;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class KillCountNotifier extends BaseNotifier
{
	private static final Pattern TIME_REGEX = Pattern.compile("(?:Duration|time|Subdued in):? (?<time>[\\d:]+(?:\\.\\d+)?)\\.?(?: Personal best: (?<pbtime>[\\d:+]+(?:\\.\\d+)?))?", Pattern.CASE_INSENSITIVE);
	private static final String NEW_BEST = "(new personal best)";
	private static final String RAID_COMPLETE = "Congratulations - your raid is complete!";
	private static final String TOB = "Theatre of Blood";
	private static final int MAX_WAIT_TICKS = 10;

	@Inject
	private KillCountService killCountService;

	private Kill pending;
	private int ticksWaited;

	@Override
	public void reset()
	{
		pending = null;
		ticksWaited = 0;
	}

	public void onGameMessage(String message, KillCountService.BossKc kc)
	{
		Kill parsed = parse(message, kc);
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
			onGameMessage(message, null);
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
			.placeholders(placeholders(kill, resolvePb(kill)))
			.screenshot(config.sendKillCountScreenshot())
			.build());
	}

	private String resolvePb(Kill kill)
	{
		if (kill.getPb() != null || kill.isPersonalBest())
		{
			return kill.getPb();
		}
		return storedPbText(killCountService.getPb(kill.getBoss()), kill.getTime(), client.getVarbitValue(VarbitID.OPTION_PRECISE_TIMING) > 0);
	}

	private boolean isMilestone(int count)
	{
		return isMilestone(count, config.killCountInterval(), config.notifyFirstKill());
	}

	static boolean isMilestone(int count, int interval, boolean notifyFirst)
	{
		return (count == 1 && notifyFirst) || (interval > 0 && count % interval == 0);
	}

	static Kill parse(String message, KillCountService.BossKc kc)
	{
		if (message.startsWith("Preparation"))
		{
			return null;
		}

		if (kc != null)
		{
			return new Kill(kc.getBoss(), kc.getCount(), null, false, null);
		}

		// TOB puts the last wave's duration before the raid's time in the same message
		int tob = message.startsWith("Wave") ? message.indexOf(TOB) : -1;
		String text = tob < 0 ? message : message.substring(tob);
		Matcher matcher = TIME_REGEX.matcher(text);
		if (!matcher.find())
		{
			return null;
		}

		return new Kill(tob < 0 ? null : TOB, null, matcher.group("time"), text.toLowerCase(Locale.ROOT).contains(NEW_BEST), matcher.group("pbtime"));
	}

	// A stored best that doesn't beat this kill is stale or from another team size
	static String storedPbText(Double stored, String time, boolean precise)
	{
		if (stored == null || (time != null && stored >= toSeconds(time)))
		{
			return null;
		}
		return formatTime(stored, precise);
	}

	private static double toSeconds(String time)
	{
		double seconds = 0;
		for (String part : time.split(":"))
		{
			seconds = seconds * 60 + Double.parseDouble(part);
		}
		return seconds;
	}

	static String formatTime(double seconds, boolean precise)
	{
		long hundredths = Math.round(seconds * 100);
		long whole = hundredths / 100;
		String time = whole >= 3600
			? String.format(Locale.ROOT, "%d:%02d:%02d", whole / 3600, whole / 60 % 60, whole % 60)
			: String.format(Locale.ROOT, "%d:%02d", whole / 60, whole % 60);
		return precise ? time + String.format(Locale.ROOT, ".%02d", hundredths % 100) : time;
	}

	static Map<String, String> placeholders(Kill kill, String pb)
	{
		return ImmutableMap.of(
			"boss", kill.getBoss(),
			"kc", KillCountService.kcText(kill.getCount()),
			"time", kill.getTime() == null ? "" : " in " + kill.getTime(),
			"pb", pb == null ? "" : " (PB " + pb + ")");
	}

	@Value
	static class Kill
	{
		String boss;
		Integer count;
		String time;
		boolean personalBest;
		String pb;

		Kill merge(Kill next)
		{
			String mergedBoss = next.boss != null ? next.boss : boss;
			// TOB sends the challenge time first and then a total time, the first is the one that counts
			boolean keepTime = next.time == null || (time != null && mergedBoss != null && mergedBoss.startsWith(TOB));
			return new Kill(mergedBoss,
				next.count != null ? next.count : count,
				keepTime ? time : next.time,
				keepTime ? personalBest : next.personalBest,
				keepTime ? pb : next.pb);
		}
	}
}
