package com.wezara.volumetradebot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ApplicationController {

    @GetMapping(value = "/hello")
    public @ResponseBody String hello() {
        return "Hello, everything is fine. I'm working.";
    }
}
