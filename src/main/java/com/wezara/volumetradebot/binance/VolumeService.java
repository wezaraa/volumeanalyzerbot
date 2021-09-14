package com.wezara.volumetradebot.binance;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.market.OrderBook;
import com.binance.api.client.domain.market.OrderBookEntry;
import com.wezara.volumetradebot.telegrambot.TelegramBot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class VolumeService
{
    private final BinanceApiRestClient client;
    private final TelegramBot bot;

    @Value( "${binance.min-volume}" )
    private int minimalVolume;

    @Value( "${binance.number-of-entries-to-calculation}" )
    private int numberOfEntriesToCalculation;

    @Value( "${binance.number-of-entries}" )
    private int numberOfEntries;

    @Value( "${binance.symbol}" )
    private String symbol;

    @Scheduled( fixedDelay = 100 )
    public void analyzeVolume() throws IOException {
        final OrderBook orderBook = client.getOrderBook( symbol, numberOfEntries );

        //Collect data by 10 points

        final Map<Integer, Double> bids = orderBook.getBids().stream().collect(
            Collectors.groupingBy( this::roundPrice
                , Collectors.summingDouble( bid -> Math.round( Double.parseDouble( bid.getQty() ) * 100.0 ) / 100.0 ) ) );

        final Map<Integer, Double> asks = orderBook.getAsks().stream().collect(
            Collectors.groupingBy( this::roundPrice
                , Collectors.summingDouble( bid -> Double.parseDouble( bid.getQty() ) ) ) );

        TreeMap<Integer, Double> sortedBids = new TreeMap<>( bids );
        while( sortedBids.size() > numberOfEntriesToCalculation )
        {
            sortedBids.pollLastEntry();
        }

        TreeMap<Integer, Double> sortedAsks = new TreeMap<>( asks );
        while( sortedAsks.size() > numberOfEntriesToCalculation )
        {
            sortedAsks.pollLastEntry();
        }

        //check if it has enough amount to action

        final Double askAmount = sortedAsks.values().stream().reduce( Double::sum ).orElse( 0.0 );
        final Double bidAmount = sortedBids.values().stream().reduce( Double::sum ).orElse( 0.0 );

        if( askAmount > minimalVolume )
        {
            log.info( "ASK ВНИЗ -> " + askAmount );
            bot.sendSignalToChannel("ВНИЗ", String.valueOf(askAmount));
            pauseExecution(askAmount);
        }
        if( bidAmount > minimalVolume )
        {
            log.info( "BID ВВЕРХ -> " + bidAmount );
            bot.sendSignalToChannel("ВВЕРХ", String.valueOf(askAmount));
            pauseExecution(bidAmount);
        }

    }

    private void pauseExecution(Double amount)
    {
        boolean stopExecution = true;
        LocalDateTime stopTime = LocalDateTime.now();
        do
        {
            if( stopTime.plusMinutes( 2 ).isBefore( LocalDateTime.now() ) )
            {
                stopExecution = false;
            }
        } while( stopExecution );
        log.info("Pause for amount {} is finished.", amount);
    }

    private Integer roundPrice( OrderBookEntry entry )
    {
        final float entryPrice = Float.parseFloat( entry.getPrice() );
        final float leftover = entryPrice % 10;
        if( leftover == 0 )
        {
            return Math.round( entryPrice );
        }
        if( leftover > 5 )
        {
            return Math.round( entryPrice + 10 - leftover );
        }
        return Math.round( entryPrice - leftover );
    }
}
