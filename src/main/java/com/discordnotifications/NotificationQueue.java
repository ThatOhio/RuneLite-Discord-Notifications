package com.discordnotifications;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Singleton
class NotificationQueue
{
	private static final long WINDOW_MS = 3_000;
	private static final long SWEEP_INTERVAL_MS = 500;
	// PetNotifier can hold a pet for 6 game ticks, the clog line has to still be here when it lands
	private static final long COLLECTION_LOG_WINDOW_MS = 4_500;
	private static final String NEW_ENTRY_NOTE = " (new collection log entry!)";

	interface Poster
	{
		void post(List<String> urls, String text, byte[] image);
	}

	private final ScheduledExecutorService executor;
	private final Map<List<String>, Batch> batches = new LinkedHashMap<>();

	private Consumer<Consumer<byte[]>> capture;
	private Poster poster;
	private ScheduledFuture<?> sweepTask;

	@Inject
	NotificationQueue(ScheduledExecutorService executor)
	{
		this.executor = executor;
	}

	void connect(Consumer<Consumer<byte[]>> capture, Poster poster)
	{
		this.capture = capture;
		this.poster = poster;
	}

	synchronized void start()
	{
		if (sweepTask != null)
		{
			return;
		}

		sweepTask = executor.scheduleAtFixedRate(() ->
		{
			// scheduleAtFixedRate cancels the task for good if it throws
			try
			{
				sweep();
			}
			catch (Exception e)
			{
				log.warn("Notification sweep failed", e);
			}
		}, SWEEP_INTERVAL_MS, SWEEP_INTERVAL_MS, TimeUnit.MILLISECONDS);
	}

	synchronized void shutdown()
	{
		if (sweepTask != null)
		{
			sweepTask.cancel(false);
			sweepTask = null;
		}

		batches.values().forEach(this::post);
		batches.clear();
	}

	synchronized void add(List<String> urls, Notification notification)
	{
		long now = now();
		Batch batch = batches.computeIfAbsent(urls, u -> new Batch(u, now + WINDOW_MS));
		batch.notifications.add(notification);

		if (notification.getType() == NotificationType.COLLECTION_LOG)
		{
			batch.deadline = Math.max(batch.deadline, now + COLLECTION_LOG_WINDOW_MS);
		}

		if (notification.isScreenshot() && !batch.captureRequested)
		{
			batch.captureRequested = true;
			capture.accept(image -> onCaptured(batch, image));
		}
	}

	private synchronized void onCaptured(Batch batch, byte[] image)
	{
		batch.image = image;
		batch.captureDone = true;
	}

	private synchronized void sweep()
	{
		long now = now();
		for (Iterator<Batch> it = batches.values().iterator(); it.hasNext(); )
		{
			Batch batch = it.next();
			// The capture has its own timeout so this can't hold a batch forever
			if (now >= batch.deadline && (!batch.captureRequested || batch.captureDone))
			{
				it.remove();
				post(batch);
			}
		}
	}

	// Tests override this so the windows don't have to really pass
	long now()
	{
		return System.currentTimeMillis();
	}

	private void post(Batch batch)
	{
		poster.post(batch.urls, join(batch.notifications), batch.image);
	}

	static String join(List<Notification> notifications)
	{
		return mergePet(mergeLoot(notifications)).stream()
			.sorted(Comparator.comparing(Notification::getType))
			.map(Notification::getText)
			.collect(Collectors.joining("\n"));
	}

	// Runs before mergePet so a pet with no name can't take the name of a real drop
	private static List<Notification> mergeLoot(List<Notification> notifications)
	{
		List<Notification> merged = new ArrayList<>(notifications);
		for (Notification loot : notifications)
		{
			if (loot.getType() == NotificationType.LOOT
				&& merged.removeIf(n -> n.getType() == NotificationType.COLLECTION_LOG && listsItem(loot, n.getPlaceholders().get("entry"))))
			{
				merged.set(merged.indexOf(loot), loot.toBuilder().text(loot.getText() + NEW_ENTRY_NOTE).build());
			}
		}
		return merged;
	}

	private static boolean listsItem(Notification loot, String item)
	{
		return loot.getItemNames().stream().anyMatch(name -> name.equalsIgnoreCase(item));
	}

	private static List<Notification> mergePet(List<Notification> notifications)
	{
		Notification pet = notifications.stream()
			.filter(n -> n.getType() == NotificationType.PET)
			.findFirst()
			.orElse(null);
		if (pet == null)
		{
			return notifications;
		}

		String name = pet.getPlaceholders().get("pet");
		if (name != null)
		{
			return notifications.stream()
				.filter(n -> n.getType() != NotificationType.COLLECTION_LOG
					|| !name.equalsIgnoreCase(n.getPlaceholders().get("entry")))
				.collect(Collectors.toList());
		}

		List<Notification> logs = notifications.stream()
			.filter(n -> n.getType() == NotificationType.COLLECTION_LOG)
			.collect(Collectors.toList());
		if (pet.getNamedText() == null || logs.size() != 1)
		{
			return notifications;
		}

		Map<String, String> values = new HashMap<>(pet.getPlaceholders());
		values.put("pet", logs.get(0).getPlaceholders().get("entry"));

		List<Notification> merged = new ArrayList<>(notifications);
		merged.remove(logs.get(0));
		merged.set(merged.indexOf(pet), pet.toBuilder().text(Template.render(pet.getNamedText(), values)).build());
		return merged;
	}

	private static class Batch
	{
		final List<String> urls;
		long deadline;
		final List<Notification> notifications = new ArrayList<>();
		boolean captureRequested;
		boolean captureDone;
		byte[] image;

		Batch(List<String> urls, long deadline)
		{
			this.urls = urls;
			this.deadline = deadline;
		}
	}
}
