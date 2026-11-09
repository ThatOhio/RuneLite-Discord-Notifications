# Discord Notifications
This is a plugin for RuneLite which sends a message to your discord channel when you level up, complete a quest or clue scroll, die, get a pet, get a valuable drop, add an item to your collection log, complete a combat task, finish an achievement diary, hit a boss kill count milestone or personal best, or unlock something on a Leagues world. Each one can be turned on or off separately, the message text can be edited, and they come with a screenshot as proof unless you turn that off.

In order to use this plugin you will need to create a webhook on your discord server. Below are instructions for how to do so:
1. Open your Server Settings and head into the Integrations tab
2. Click the "Create Webhook" button to create a new webhook
3. Once created, copy the webhook URL and insert into the "Webhook URL" box in the plugin's configuration in RuneLite. If you want to post to more than one channel, separate the URLs with commas.

Discord has a more detailed guide here: https://support.discord.com/hc/en-us/articles/228383668-Intro-to-Webhooks

![image](https://user-images.githubusercontent.com/13265450/109745517-9b42ef80-7b99-11eb-82f9-63f8ea590e8c.png)

## Defaults
Nothing gets posted until you turn it on, so after adding your webhook open the sections you want and tick the send option. Screenshots are on for all of them. Once an event is on, this is what gets sent:

| Event | What gets sent |
| --- | --- |
| Level ups | Every 10 levels from 50 and every level from 90 up. Levels past 99 and combat level are their own options and start off |
| Quests | Every quest |
| Deaths | Skips safe deaths like minigames and your house |
| Clue scrolls | Every tier, rewards worth 100k or more |
| Pets | Includes pets you already have |
| Collection log | Every new slot |
| Combat tasks | Every tier |
| Loot | Stacks worth 100k or more |
| Achievement diaries | Every difficulty |
| Boss kills | Every 50 kills, first kills and personal bests |
| Leagues | Areas, relics, masteries and tasks |

## Notes
- Collection log notifications need the new addition message turned on in your game settings, chat or popup both work (Settings > All Settings > Chat).
- Combat task notifications come from the chat message, so the popup setting doesn't matter. If you have the repeat completion setting on for combat tasks you'll get a notification every time you redo one.
- Loot from raids, chests and pickpocketing comes through RuneLite's Loot Tracker plugin, so leave that turned on if you want those.
- Boss kill notifications come from the kill count chat message, so Filter out boss kill-count with spam-filter needs to be off in your game settings.
- Leagues notifications only get sent on a Leagues world. The settings are near the bottom of the list.
- Screenshots are of the whole game window, chatbox included. If you don't want that posted to the channel, turn off the screenshot option for that event.
- If something happens that the plugin doesn't catch, set the Screenshot hotkey in Webhook Settings and press it to post a screenshot yourself. It does nothing until you pick a key.
- Keep your webhook URL to yourself, anyone with it can post to your channel.

Found a bug or want something added? Open an issue: https://github.com/ThatOhio/RuneLite-Discord-Notifications/issues

## Contribution

Initially forked from: https://github.com/ATremonte/Discord-Level-Notifications
