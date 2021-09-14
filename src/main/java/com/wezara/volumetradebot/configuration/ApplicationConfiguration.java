package com.wezara.volumetradebot.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.bots.DefaultBotOptions;

@Configuration
public class ApplicationConfiguration
{
    @Bean
    RestTemplate restTemplate()
    {
        return new RestTemplate();
    }

    @Bean
    DefaultBotOptions defaultBotOptions()
    {
        DefaultBotOptions options = new DefaultBotOptions();
        options.setMaxThreads( 4 );
        return options;
    }
}
