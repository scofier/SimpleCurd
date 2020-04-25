package com.demo.springeventstore.core;

import org.springframework.context.ApplicationEvent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * 限定String：方便序列化存储，方便日志排查问题
 * 复合类型需要自己序列化， 比如序列化为json   S入参泛型  R出参泛型
 */
public abstract class BaseEvent<S, R> extends ApplicationEvent {

    private boolean store = false;
    /**
     * 延时投递，绝对时间，单位毫秒
     **/
    private long startDeliverTime;

    private String uuid = UUID.randomUUID().toString();

    /**
     * 事件返回结果
     */
    private R result;

    @JsonCreator
    public BaseEvent(@JsonProperty("source") S source) {
        super(source);
    }


    public R getResult() {
        return result;
    }

    public void setResult(R o) {
        this.result = o;
    }

    public void setSource(S data) {
        this.source = data;
    }

    @SuppressWarnings("unchecked")
    @Override
    public S getSource() {
        if (super.getSource() == null) {
            return null;
        }
        return (S) super.getSource();
    }

    public boolean isStore() {
        return store;
    }

    public void setStore(boolean store) {
        this.store = store;
    }

    public long getStartDeliverTime() {
        return startDeliverTime;
    }

    public void setStartDeliverTime(long startDeliverTime) {
        this.startDeliverTime = startDeliverTime;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}