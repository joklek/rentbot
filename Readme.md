# rentbot

This bot scans the most popular flat rent portals for latest posts in Vilnius, which will be sent to subscribed users using Telegram app.

Original idea taken from https://github.com/erkexzcx/bbtmvbot

# Usage

Below are the steps on how to host it yourself.

1. Setup Telegram bot

Using [BotFather](https://t.me/BotFather) create your own Telegram bot.

Also using BotFather use command `/setcommands` and update your bot commands:
```
info - Information about the Rent Bot
enable - Enable notifications
disable - Disable notifications
config - Configure bot settings
districts - Configure district settings
replay - replay posts from last 2 days
```
Once you set-up bot, you should have your bot's Telegram **API key**.

2. [Install Java](https://adoptium.net/download/).

3. Build it
```
git clone https://github.com/joklek/rentbot.git
cd rentbot
mvnw -DskipTests=true package
```

4. Run it as you prefer, I have script like this

```sh
@echo off
java -D"TELEGRAM_TOKEN=API_TOKEN" -jar rentbot-0.0.1-SNAPSHOT.jar >> OUTPUT.txt 2>&1
```
