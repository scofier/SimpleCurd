package com.demo.springeventstore.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * @author sk
 */
@Component
public class UserEventListener {

    private static final Logger log = LoggerFactory.getLogger(UserEventListener.class);


    @EventListener
    public void userEvent1(UserEvent1 u) {
        log.info("UserEvent1---: {}", u);
    }

}
