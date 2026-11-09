package com.discordnotifications.notifiers;

import com.discordnotifications.NotificationType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.input.KeyManager;
import net.runelite.client.util.HotkeyListener;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;

@Singleton
public class ManualScreenshotNotifier extends BaseNotifier
{
	@Inject
	private KeyManager keyManager;

	@Inject
	private ClientThread clientThread;

	private final HotkeyListener hotkey = new HotkeyListener(() -> config.screenshotHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			// Key events aren't on the client thread, and capturing has to be
			clientThread.invokeLater(() -> send(NotificationType.MANUAL_SCREENSHOT, config.screenshotMessage(), Collections.emptyMap(), true));
		}
	};

	public void start()
	{
		keyManager.registerKeyListener(hotkey);
	}

	public void stop()
	{
		keyManager.unregisterKeyListener(hotkey);
	}
}
