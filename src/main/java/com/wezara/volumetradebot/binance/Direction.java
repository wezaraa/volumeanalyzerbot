package com.wezara.volumetradebot.binance;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Direction {
    UP("ВВЕРХ"),
    DOWN("ВНИЗ");

    private final String translatedDirection;
}
