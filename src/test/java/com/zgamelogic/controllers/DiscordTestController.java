package com.zgamelogic.autoconfigure;

import com.zgamelogic.annotations.DiscordController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@DiscordController
@Slf4j
public class DiscordTestController {

    @Autowired
    public DiscordTestController(){
        log.info("Test controller autowired");
    }
}
