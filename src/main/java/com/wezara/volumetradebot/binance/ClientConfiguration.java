package com.wezara.volumetradebot.binance;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClientConfiguration
{
    @Value( "${binance.api-key}" )
    private String apiKey;

    @Value( "${binance.secret-key}" )
    private String secretKey;

    @Bean
    public BinanceApiRestClient binanceApiRestClient(){
        BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance( apiKey, secretKey );

        return factory.newRestClient();
    }
}
