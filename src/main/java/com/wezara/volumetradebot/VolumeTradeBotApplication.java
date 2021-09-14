package com.wezara.volumetradebot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VolumeTradeBotApplication
{
    public static void main( String[] args )
    {
        SpringApplication.run( VolumeTradeBotApplication.class, args );
    }
}
