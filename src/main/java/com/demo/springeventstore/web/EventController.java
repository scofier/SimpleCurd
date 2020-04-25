package com.demo.springeventstore.web;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author sk
 */
@RequestMapping("/event")
@RestController
public class EventController {

    @Resource
    ApplicationEventPublisher applicationEventPublisher;

    @GetMapping("/test1")
    public void block(User user) {
        UserEvent1 u = new UserEvent1(user);
        //设置消息持久化
        u.setStore(true);

        applicationEventPublisher.publishEvent(u);
    }


}
