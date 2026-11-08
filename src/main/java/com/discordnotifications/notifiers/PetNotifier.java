package com.discordnotifications.notifiers;

import com.google.common.collect.ImmutableList;

import javax.inject.Singleton;

@Singleton
public class PetNotifier extends BaseNotifier
{
	private static final ImmutableList<String> PET_MESSAGES =
		ImmutableList.of(
			"You have a funny feeling like you're being followed",
			"You feel something weird sneaking into your backpack",
			"You have a funny feeling like you would have been followed"
		);

	public void onGameMessage(String chatMessage)
	{
		if (config.includePets() && PET_MESSAGES.stream().anyMatch(chatMessage::contains))
		{
			send(config.petMessage(), config.sendPetScreenshot());
		}
	}
}
