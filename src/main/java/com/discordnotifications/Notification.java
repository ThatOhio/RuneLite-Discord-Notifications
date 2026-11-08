package com.discordnotifications;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

@Value
@Builder(toBuilder = true)
public class Notification
{
	NotificationType type;
	String text;
	boolean screenshot;

	@Builder.Default
	Map<String, String> placeholders = Collections.emptyMap();

	@Builder.Default
	Instant createdAt = Instant.now();
}
