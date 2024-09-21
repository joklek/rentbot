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
Once you setup bot, you should have your bot's Telegram **API key**.

2. Set up the secrets 
Add a `.env` file in the root of the project with the following content:
```
TELEGRAM_TOKEN=your_token
```

3. Build it
```shell
docker compose build
```

3. Run it
```shell
docker compose up
```