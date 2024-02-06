package com.zgamelogic.controllers;

import com.zgamelogic.annotations.DiscordController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@DiscordController
@Slf4j
public class DiscordTestControllerTheSecond {

    @Autowired
    public DiscordTestControllerTheSecond(){
        log.info("Test controller the second autowired");
    }
}
