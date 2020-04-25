package com.demo.springeventstore.web;

import com.demo.springeventstore.core.BaseEvent;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author sk
 */
public class UserEvent1 extends BaseEvent<User, Object> {

    @JsonCreator
    public UserEvent1(@JsonProperty("source")User source) {
        super(source);
    }
}
