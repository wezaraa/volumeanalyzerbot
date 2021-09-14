package com.wezara.volumetradebot.telegrambot;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties( prefix = "telegrambot" )
@Data
public class TelegramBotProperty
{
    private String webHookPath;
    private String botName;
    private String botToken;
}

