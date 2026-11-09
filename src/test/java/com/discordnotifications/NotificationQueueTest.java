package com.discordnotifications;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class NotificationQueueTest
{
	private static final List<String> FIRST = List.of("first");
	private static final List<String> SECOND = List.of("second");
	private static final List<String> THIRD = List.of("third");
	private static final byte[] IMAGE = {1};

	private final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
	private final ScheduledFuture<?> sweepTask = mock(ScheduledFuture.class);
	private final NotificationQueue.Poster poster = mock(NotificationQueue.Poster.class);
	private final List<Consumer<byte[]>> captures = new ArrayList<>();
	private long time = 1_000_000;
	private NotificationQueue queue;
	private Runnable sweep;

	@Before
	public void setUp()
	{
		doReturn(sweepTask).when(executor).scheduleAtFixedRate(any(), anyLong(), anyLong(), any());
		queue = new NotificationQueue(executor)
		{
			@Override
			long now()
			{
				return time;
			}
		};
		queue.connect(captures::add, poster);
		queue.start();

		ArgumentCaptor<Runnable> scheduled = ArgumentCaptor.forClass(Runnable.class);
		verify(executor).scheduleAtFixedRate(scheduled.capture(), eq(500L), eq(500L), eq(TimeUnit.MILLISECONDS));
		sweep = scheduled.getValue();
	}

	private void sweepAfter(long millis)
	{
		time += millis;
		sweep.run();
	}

	private static Notification withScreenshot(String text)
	{
		return Notification.builder().type(NotificationType.LEVEL).text(text).screenshot(true).build();
	}

	@Test
	public void startingTwiceSchedulesOneSweep()
	{
		queue.start();
		verify(executor, times(1)).scheduleAtFixedRate(any(), anyLong(), anyLong(), any());
	}

	@Test
	public void sameWebhooksShareAPost()
	{
		queue.add(FIRST, of(NotificationType.LEVEL, "level"));
		queue.add(FIRST, of(NotificationType.DEATH, "death"));
		queue.shutdown();
		verify(poster).post(FIRST, "level\ndeath", null);
		verifyNoMoreInteractions(poster);
	}

	@Test
	public void differentWebhooksPostSeparately()
	{
		queue.add(FIRST, of(NotificationType.LEVEL, "one"));
		queue.add(SECOND, of(NotificationType.LEVEL, "two"));
		queue.shutdown();
		verify(poster).post(FIRST, "one", null);
		verify(poster).post(SECOND, "two", null);
	}

	@Test
	public void capturesOncePerBatchAndOnlyWhenAsked()
	{
		queue.add(FIRST, of(NotificationType.LEVEL, "no screenshot"));
		assertEquals(0, captures.size());

		queue.add(FIRST, withScreenshot("one"));
		queue.add(FIRST, withScreenshot("two"));
		assertEquals(1, captures.size());

		queue.add(SECOND, withScreenshot("three"));
		assertEquals(2, captures.size());
	}

	@Test
	public void postCarriesTheCapturedImage()
	{
		queue.add(FIRST, withScreenshot("level"));
		captures.get(0).accept(IMAGE);
		queue.shutdown();
		verify(poster).post(FIRST, "level", IMAGE);
	}

	@Test
	public void shutdownFlushesWithoutWaitingAndStopsTheSweep()
	{
		queue.add(FIRST, withScreenshot("capture never came back"));
		queue.shutdown();
		verify(poster).post(FIRST, "capture never came back", null);
		verify(sweepTask).cancel(false);

		queue.shutdown();
		verifyNoMoreInteractions(poster);

		queue.start();
		verify(executor, times(2)).scheduleAtFixedRate(any(), anyLong(), anyLong(), any());
	}

	@Test
	public void batchPostsWhenItsWindowEnds()
	{
		queue.add(FIRST, of(NotificationType.LEVEL, "level"));

		sweepAfter(2_999);
		verifyNoInteractions(poster);

		sweepAfter(1);
		verify(poster).post(FIRST, "level", null);

		sweepAfter(3_000);
		verifyNoMoreInteractions(poster);
	}

	@Test
	public void finishedCaptureDoesntPostBeforeTheWindowEnds()
	{
		queue.add(FIRST, withScreenshot("level"));
		captures.get(0).accept(IMAGE);

		sweepAfter(2_999);
		verifyNoInteractions(poster);

		sweepAfter(1);
		verify(poster).post(FIRST, "level", IMAGE);
	}

	@Test
	public void pendingCaptureHoldsABatchPastItsWindow()
	{
		queue.add(FIRST, withScreenshot("level"));

		sweepAfter(60_000);
		verifyNoInteractions(poster);

		captures.get(0).accept(IMAGE);
		sweepAfter(0);
		verify(poster).post(FIRST, "level", IMAGE);
	}

	@Test
	public void collectionLogExtendsTheWindow()
	{
		queue.add(FIRST, of(NotificationType.LEVEL, "level"));
		queue.add(FIRST, clog("Twisted bow"));

		sweepAfter(4_499);
		verifyNoInteractions(poster);

		sweepAfter(1);
		verify(poster).post(FIRST, "clog Twisted bow\nlevel", null);
	}

	@Test
	public void lateCollectionLogGetsItsFullWindow()
	{
		queue.add(FIRST, of(NotificationType.LEVEL, "level"));
		time += 2_000;
		queue.add(FIRST, clog("Twisted bow"));

		sweepAfter(4_499);
		verifyNoInteractions(poster);

		sweepAfter(1);
		verify(poster).post(FIRST, "clog Twisted bow\nlevel", null);
	}

	@Test
	public void laterLinesDontMoveTheDeadline()
	{
		queue.add(FIRST, of(NotificationType.LEVEL, "first"));
		time += 2_000;
		queue.add(FIRST, of(NotificationType.LEVEL, "second"));

		sweepAfter(1_000);
		verify(poster).post(FIRST, "first\nsecond", null);
	}

	@Test
	public void batchesKeepTheirOwnDeadlines()
	{
		queue.add(FIRST, of(NotificationType.LEVEL, "one"));
		time += 1_000;
		queue.add(SECOND, of(NotificationType.LEVEL, "two"));

		sweepAfter(2_000);
		verify(poster).post(FIRST, "one", null);
		verifyNoMoreInteractions(poster);
	}

	private static Notification of(NotificationType type, String text)
	{
		return Notification.builder().type(type).text(text).build();
	}

	@Test
	public void ordersByTypePriority()
	{
		assertEquals("pet\nclog\nlevel\ndeath", NotificationQueue.join(List.of(
			of(NotificationType.DEATH, "death"),
			of(NotificationType.LEVEL, "level"),
			of(NotificationType.COLLECTION_LOG, "clog"),
			of(NotificationType.PET, "pet"))));
	}

	@Test
	public void keepsArrivalOrderWithinAType()
	{
		assertEquals("first\nsecond", NotificationQueue.join(List.of(
			of(NotificationType.LEVEL, "first"),
			of(NotificationType.LEVEL, "second"))));
	}

	private static Notification clog(String entry)
	{
		return Notification.builder()
			.type(NotificationType.COLLECTION_LOG)
			.text("clog " + entry)
			.placeholders(Map.of("entry", entry))
			.build();
	}

	private static Notification namedPet(String pet)
	{
		return Notification.builder()
			.type(NotificationType.PET)
			.text("pet " + pet)
			.placeholders(Map.of("name", "Bob", "pet", pet))
			.build();
	}

	private static Notification unnamedPet(String namedText)
	{
		return Notification.builder()
			.type(NotificationType.PET)
			.text("Bob just got a pet!")
			.namedText(namedText)
			.placeholders(Map.of("name", "Bob"))
			.build();
	}

	@Test
	public void petDropsItsOwnCollectionLogLine()
	{
		assertEquals("pet Olmlet", NotificationQueue.join(List.of(clog("olmlet"), namedPet("Olmlet"))));
	}

	@Test
	public void namedPetKeepsUnrelatedCollectionLogLine()
	{
		assertEquals("pet Olmlet\nclog Twisted bow",
			NotificationQueue.join(List.of(clog("Twisted bow"), namedPet("Olmlet"))));
	}

	@Test
	public void unnamedPetTakesItsNameFromCollectionLog()
	{
		assertEquals("Bob just got a pet: Olmlet",
			NotificationQueue.join(List.of(unnamedPet("$name just got a pet: $pet$kc"), clog("Olmlet"))));
	}

	@Test
	public void mergedPetNameIsEscapedOnce()
	{
		assertEquals("Bob got Pet\\_snake",
			NotificationQueue.join(List.of(unnamedPet("$name got $pet"), clog("Pet_snake"))));
	}

	@Test
	public void unnamedPetLeavesSeveralCollectionLogLinesAlone()
	{
		assertEquals("Bob just got a pet!\nclog Olmlet\nclog Twisted bow",
			NotificationQueue.join(List.of(unnamedPet("$pet"), clog("Olmlet"), clog("Twisted bow"))));
	}

	private static Notification loot(String text, String... items)
	{
		return Notification.builder()
			.type(NotificationType.LOOT)
			.text(text)
			.itemNames(List.of(items))
			.build();
	}

	@Test
	public void lootLineAbsorbsItsCollectionLogLine()
	{
		assertEquals("loot (new collection log entry!)",
			NotificationQueue.join(List.of(loot("loot", "Twisted bow", "Coins"), clog("twisted bow"))));
	}

	@Test
	public void lootKeepsCollectionLogLineForAnItemItDoesntList()
	{
		assertEquals("clog Twisted bow\nloot",
			NotificationQueue.join(List.of(loot("loot", "Coins"), clog("Twisted bow"))));
	}

	@Test
	public void twoEntriesOnOneLootLineGetOneNote()
	{
		assertEquals("loot (new collection log entry!)",
			NotificationQueue.join(List.of(loot("loot", "Twisted bow", "Elder maul"), clog("Twisted bow"), clog("Elder maul"))));
	}

	@Test
	public void oneEntryOnlyMarksOneLootLine()
	{
		assertEquals("first (new collection log entry!)\nsecond",
			NotificationQueue.join(List.of(loot("first", "Twisted bow"), loot("second", "Twisted bow"), clog("Twisted bow"))));
	}

	@Test
	public void unnamedPetDoesNotTakeTheNameOfALootItem()
	{
		assertEquals("Bob just got a pet!\nloot (new collection log entry!)",
			NotificationQueue.join(List.of(unnamedPet("$name just got a pet: $pet"), loot("loot", "Twisted bow"), clog("Twisted bow"))));
	}

	@Test
	public void duplicatePetDoesNotTakeACollectionLogName()
	{
		assertEquals("Bob just got a pet!\nclog Twisted bow",
			NotificationQueue.join(List.of(unnamedPet(null), clog("Twisted bow"))));
	}

	@Test
	public void rankUpLineSurvivesTheMerges()
	{
		Notification rankUp = of(NotificationType.COLLECTION_LOG_RANK, "rank");
		assertEquals("loot (new collection log entry!)\nrank",
			NotificationQueue.join(List.of(clog("Twisted bow"), rankUp, loot("loot", "Twisted bow"))));
		assertEquals("pet Olmlet\nrank",
			NotificationQueue.join(List.of(clog("olmlet"), rankUp, namedPet("Olmlet"))));
		assertEquals("Bob got Olmlet\nrank",
			NotificationQueue.join(List.of(clog("Olmlet"), rankUp, unnamedPet("Bob got $pet"))));
	}
}
