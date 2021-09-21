package com.wezara.volumetradebot.binance;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.market.OrderBook;
import com.binance.api.client.domain.market.OrderBookEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class VolumeService {
    private final BinanceApiRestClient client;

    private static final String CHANNEL_MESSAGE_URL = "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s";

    @Value("${binance.primary-min-volume}")
    private int primaryMinimalVolume;
    @Value("${binance.primary-symbol}")
    private String primarySymbol;

    @Value("${binance.secondary-min-volume}")
    private int secondaryMinimalVolume;
    @Value("${binance.secondary-symbol}")
    private String secondarySymbol;

    @Value("${binance.number-of-entries-to-calculation}")
    private int numberOfEntriesToCalculation;
    @Value("${binance.number-of-entries}")
    private int numberOfEntries;
    @Value("${telegrambot.botToken}")
    private String botToken;
    @Value("${telegrambot.channel-id}")
    private String channelId;

    private int primaryRequestCounter = 0;
    private Double maxPrimaryAskQuantity = 0d;
    private Double maxPrimaryBidQuantity = 0d;

    private int secondaryRequestCounter = 0;
    private Double maxSecondaryAskQuantity = 0d;
    private Double maxSecondaryBidQuantity = 0d;

    @Scheduled(fixedDelay = 50)
    public void analyzePrimaryVolume() throws IOException {
        final OrderBook orderBook = client.getOrderBook(primarySymbol, numberOfEntries);

        compareAndSetPrimaryQuantity(getQuantity(orderBook.getBids(), this::roundPrimaryPrice), Direction.UP);
        compareAndSetPrimaryQuantity(getQuantity(orderBook.getAsks(), this::roundPrimaryPrice), Direction.DOWN);

        ++primaryRequestCounter;
        if (primaryRequestCounter == 10) {

            log.info("Max ask -> {}, Max bid -> {} for primary symbol {}", maxPrimaryAskQuantity, maxPrimaryBidQuantity, primarySymbol);

            performOperation(maxPrimaryAskQuantity, maxPrimaryBidQuantity, primarySymbol, primaryMinimalVolume, this::roundPrimaryPrice);

            maxPrimaryAskQuantity = 0d;
            maxPrimaryBidQuantity = 0d;
            primaryRequestCounter = 0;
        }
    }

    private void compareAndSetPrimaryQuantity(Double quantity, Direction direction) {
        if (direction == Direction.UP && quantity > maxPrimaryBidQuantity) {
            maxPrimaryBidQuantity = quantity;
        }
        if (direction == Direction.DOWN && quantity > maxPrimaryAskQuantity) {
            maxPrimaryAskQuantity = quantity;
        }
    }

    private Integer roundPrimaryPrice(OrderBookEntry entry) {
        final float entryPrice = Float.parseFloat(entry.getPrice());
        final float leftover = entryPrice % 10;
        if (leftover == 0) {
            return Math.round(entryPrice);
        }
        if (leftover > 5) {
            return Math.round(entryPrice + 10 - leftover);
        }
        return Math.round(entryPrice - leftover);
    }

    /* --------------------------------------------------------------------------------------------------------------------------------
                ------------------------------------------------------------------------------------------------------
    ---------------------------------------------------------------------------------------------------------------------------------*/

    @Scheduled(fixedDelay = 50)
    public void analyzeSecondaryVolume() throws IOException {
        final OrderBook orderBook = client.getOrderBook(secondarySymbol, numberOfEntries);

        compareAndSetSecondaryQuantity(getQuantity(orderBook.getBids(), this::roundSecondaryPrice), Direction.UP);
        compareAndSetSecondaryQuantity(getQuantity(orderBook.getAsks(), this::roundSecondaryPrice), Direction.DOWN);

        ++secondaryRequestCounter;
        if (secondaryRequestCounter == 10) {
            log.info("Max ask -> {}, Max bid -> {} for secondary symbol {}", maxSecondaryAskQuantity, maxSecondaryBidQuantity, secondarySymbol);

            performOperation(maxSecondaryAskQuantity, maxSecondaryBidQuantity, secondarySymbol, secondaryMinimalVolume, this::roundSecondaryPrice);

            maxSecondaryAskQuantity = 0d;
            maxSecondaryBidQuantity = 0d;
            secondaryRequestCounter = 0;
        }
    }

    private void compareAndSetSecondaryQuantity(Double quantity, Direction direction) {
        if (direction == Direction.UP && quantity > maxSecondaryBidQuantity) {
            maxSecondaryBidQuantity = quantity;
        }
        if (direction == Direction.DOWN && quantity > maxSecondaryAskQuantity) {
            maxSecondaryAskQuantity = quantity;
        }
    }

    private Integer roundSecondaryPrice(OrderBookEntry entry) {
        return Math.round(Float.parseFloat(entry.getPrice()));
    }

    /*---------------------------------------------------------------------------------------------------------
            ------------------------------------------------------------------------------
     */

    private void performOperation(Double maxAskQuantity, Double maxBidQuantity, String symbol, Integer minimalVolume, Function<OrderBookEntry, Integer> priceFunction) throws IOException {
        if (maxAskQuantity > maxBidQuantity && maxAskQuantity > minimalVolume) {
            log.info("ВНИЗ {} -> {}", symbol, maxAskQuantity);
            boolean isProved = checkIfSignalIsProved(symbol, Direction.DOWN, maxAskQuantity, priceFunction);
            if (isProved) {
                sendSignalToChannel(symbol, Direction.DOWN.getTranslatedDirection(), maxAskQuantity);
            }
            pauseExecution(symbol, maxAskQuantity);
        }
        if (maxBidQuantity > maxAskQuantity && maxBidQuantity > minimalVolume) {
            log.info("ВВЕРХ {} -> {}", symbol, maxBidQuantity);
            boolean isProved = checkIfSignalIsProved(symbol, Direction.UP, maxBidQuantity, priceFunction);
            if (isProved) {
                sendSignalToChannel(symbol, Direction.UP.getTranslatedDirection(), maxBidQuantity);
            }
            pauseExecution(symbol, maxBidQuantity);
        }
    }

    private Double getQuantity(List<OrderBookEntry> entries, Function<OrderBookEntry, Integer> priceFunction) {
        final Map<Integer, Double> roundedEntries = entries.stream().collect(
                Collectors.groupingBy(priceFunction
                        , Collectors.summingDouble(roundedEntry -> Double.parseDouble(roundedEntry.getQty()))));

        TreeMap<Integer, Double> sortedEntries = new TreeMap<>(roundedEntries);
        while (sortedEntries.size() > numberOfEntriesToCalculation) {
            sortedEntries.pollLastEntry();
        }
        return sortedEntries.values().stream().reduce(Double::sum).orElse(0.0);
    }

    private boolean checkIfSignalIsProved(String symbol, Direction direction, Double signalAmount, Function<OrderBookEntry, Integer> priceFunction) {
        Double maxQuantity = 0d;
        int counter = 0;
        do {
            final OrderBook orderBook = client.getOrderBook(symbol, numberOfEntries);
            Double quantity;
            if (direction == Direction.UP) {
                quantity = getQuantity(orderBook.getAsks(), priceFunction);
            } else {
                quantity = getQuantity(orderBook.getBids(), priceFunction);
            }
            if (quantity > maxQuantity) {
                maxQuantity = quantity;
            }
            ++counter;
        } while (counter < 31);
        if (maxQuantity > signalAmount * 0.6) {
            log.info("Для {} сигнала {} на суму {} перекупляють на суму {}", symbol, direction.getTranslatedDirection(), signalAmount, maxQuantity);
            return false;
        }
        return true;
    }

    private void pauseExecution(String symbol, Double amount) {
        log.info("Starting pause for symbol {} and amount {}.", symbol, amount);
        boolean stopExecution = true;
        LocalDateTime stopTime = LocalDateTime.now();
        do {
            if (stopTime.plusMinutes(2).isBefore(LocalDateTime.now())) {
                stopExecution = false;
            }
        } while (stopExecution);
        log.info("Pause for symbol {} and amount {} is finished.", symbol, amount);
    }


    private void sendSignalToChannel(String symbol, String direction, Double coinQuantity) throws IOException {
        LocalTime now = LocalTime.now();
        String toTime;
        if (now.getSecond() > 30) {
            toTime = now.plusMinutes(2).plusSeconds(30).format(DateTimeFormatter.ofPattern("HH:mm"));
        } else {
            toTime = now.plusMinutes(2).format(DateTimeFormatter.ofPattern("HH:mm"));
        }
        String text = String.format("COIN IDX - %s до %s. Кількість %s - %,.2f", direction, toTime, symbol, coinQuantity);

        String urlString = String.format(CHANNEL_MESSAGE_URL, botToken, channelId, text);

        URL url = new URL(urlString);
        URLConnection conn = url.openConnection();
        InputStream is = new BufferedInputStream(conn.getInputStream());
        is.close();
        log.info("Sent message to channel with body: {}", text);
    }
}
