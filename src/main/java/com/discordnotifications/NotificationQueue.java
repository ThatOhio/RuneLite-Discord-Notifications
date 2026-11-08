package com.discordnotifications;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Comparator;
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
		Batch batch = batches.computeIfAbsent(urls, u -> new Batch(u, System.currentTimeMillis() + WINDOW_MS));
		batch.notifications.add(notification);

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
		long now = System.currentTimeMillis();
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

	private void post(Batch batch)
	{
		poster.post(batch.urls, join(batch.notifications), batch.image);
	}

	static String join(List<Notification> notifications)
	{
		return notifications.stream()
			.sorted(Comparator.comparing(Notification::getType))
			.map(Notification::getText)
			.collect(Collectors.joining("\n"));
	}

	private static class Batch
	{
		final List<String> urls;
		final long deadline;
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
