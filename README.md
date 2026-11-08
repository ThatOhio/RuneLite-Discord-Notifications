# Discord Notifications
This is a plugin for RuneLite which sends a message to your discord channel when you level up, complete a quest or clue scroll, die, get a pet, get a valuable drop, add an item to your collection log, complete a combat task or finish an achievement diary. Each one can be turned on or off separately, the message text can be edited, and you can optionally attach a screenshot as proof.

In order to use this plugin you will need to create a webhook on your discord server. Below are instructions for how to do so:
1. Open your Server Settings and head into the Integrations tab
2. Click the "Create Webhook" button to create a new webhook
3. Once created, copy the webhook URL and insert into the "Webhook URL" box in the plugin's configuration in RuneLite. If you want to post to more than one channel, put each URL on its own line.

Discord has a more detailed guide here: https://support.discord.com/hc/en-us/articles/228383668-Intro-to-Webhooks

![image](https://user-images.githubusercontent.com/13265450/109745517-9b42ef80-7b99-11eb-82f9-63f8ea590e8c.png)

![image](https://i.imgur.com/ACGx8eB.png)

## Notes
- Collection log notifications need the new addition message turned on in your game settings, chat or popup both work (Settings > All Settings > Chat).
- Combat task notifications come from the chat message, so the popup setting doesn't matter. If you have the repeat completion setting on for combat tasks you'll get a notification every time you redo one.
- Loot from raids, chests and pickpocketing comes through RuneLite's Loot Tracker plugin, so leave that turned on if you want those.
- Screenshots are of the whole game window, chatbox included. If you don't want that posted to the channel, turn off the screenshot option for that event.
- Keep your webhook URL to yourself, anyone with it can post to your channel.

Found a bug or want something added? Open an issue: https://github.com/ThatOhio/RuneLite-Discord-Notifications/issues

## Contribution

Initially forked from: https://github.com/ATremonte/Discord-Level-Notifications
