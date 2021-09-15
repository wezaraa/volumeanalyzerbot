package com.wezara.volumetradebot.telegrambot;

import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;


@EqualsAndHashCode(callSuper = true)
@Slf4j
@Component
public class TelegramBot extends TelegramWebhookBot {
    private final TelegramBotProperty configuration;
    private final DefaultBotOptions options;

    @Value("${telegrambot.channel-id}")
    private String channelId;

    private static final String CHANNEL_MESSAGE_URL = "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s";

    public TelegramBot(TelegramBotProperty configuration, DefaultBotOptions options) {
        super(options);
        this.configuration = configuration;
        this.options = options;
    }

    @Override
    public String getBotUsername() {
        return configuration.getBotName();
    }

    @Override
    public String getBotToken() {
        return configuration.getBotToken();
    }

    @Override
    public String getBotPath() {
        return configuration.getWebHookPath();
    }

    @Override
    public BotApiMethod onWebhookUpdateReceived(Update update) {
        log.info("update ->" + update);
        return null;
    }

    public void sendSignalToChannel(String direction, Double coinQuantity) throws IOException {
        String toTime = LocalTime.now().plusMinutes(2).format(DateTimeFormatter.ofPattern("HH:mm"));
        String text = String.format("COIN IDX - %s до %s. Кількість - %,.2f", direction, toTime, coinQuantity);

        String urlString = String.format(CHANNEL_MESSAGE_URL, getBotToken(), channelId, text);

        URL url = new URL(urlString);
        URLConnection conn = url.openConnection();
        InputStream is = new BufferedInputStream(conn.getInputStream());
        is.close();
        log.info("Sent message to channel with body: {}", text);
    }
}
