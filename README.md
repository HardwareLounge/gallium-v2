# Gallium Discord Bot

This is the moderation bot and ticket system for the 
[Hardwarelounge Discord guild](https://hardwarelounge.net/go/discord)

### Build and run

You will need to install Docker first in order to run this bot as intended.

(Re)starting the Bot:
```shell
docker build -t gallium:v2 -t gallium:latest .
docker-compose up -d
```

Stopping the bot:
```shell
docker-compose down
```
