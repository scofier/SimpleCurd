package com.demo.springeventstore.core;

import com.aliyun.openservices.ons.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * 增强applicationEvent事件，对接mq，实现事件持久化，实现listener的顺序实时读取（非缓存读取）
 * Event 需要加上 @JsonCreator ，@JsonProperty("source") 注解来实现消息反序列化注入
 * {@link org.springframework.context.event.SimpleApplicationEventMulticaster}
 * @author sk
 */
@Component("applicationEventMulticaster")
public class SimpleApplicationEventMulticaster extends org.springframework.context.event.SimpleApplicationEventMulticaster implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(SimpleApplicationEventMulticaster.class);

	private Producer producer;

	private String topic = "XIAO_TOPIC_TEMP_TEST";

	private Map<String, Class<? extends BaseEvent<?,?>>> events = new ConcurrentHashMap<>(250);

	public Properties getPro() {
		Properties properties = new Properties();

		properties.setProperty(PropertyKeyConst.GROUP_ID, "x");
		properties.setProperty(PropertyKeyConst.AccessKey, "x");
		properties.setProperty(PropertyKeyConst.SecretKey, "x");
		properties.setProperty(PropertyKeyConst.NAMESRV_ADDR, "x");
		properties.setProperty(PropertyKeyConst.ConsumeTimeout, "3");
		properties.setProperty(PropertyKeyConst.InstanceName, UUID.randomUUID().toString());
		properties.put(PropertyKeyConst.MessageModel, PropertyValueConst.CLUSTERING);
		return properties;
	}

	/**
	 * spring容器初始化之后，再执行
	 */
	@Override
	public void run(String[] args) {
		Consumer consumer = ONSFactory.createConsumer(getPro());
		producer = ONSFactory.createProducer(getPro());
		producer.start();
		// 注册消息监听
		consumer.subscribe(topic, "*", new MessageListener() {
			@Override
			public Action consume(Message message, ConsumeContext consumeContext) {
				String tag = message.getTag();
				String body = new String(message.getBody());
				BaseEvent<?,?> event = JSON.toObject(body, getEventClass(tag));

				exchangeEvent(event, null);
				return Action.CommitMessage;
			}
		});
		consumer.start();
	}

	@Override
	public void multicastEvent(@NonNull final ApplicationEvent event, @Nullable ResolvableType eventType) {

		if(event instanceof BaseEvent) {

			BaseEvent<?,?> baseEvent = (BaseEvent<?,?>)event;
			// 判断持久化消息，执行mq投递
			if(baseEvent.isStore()) {
				Message msg = new Message();
				msg.setKey(baseEvent.getUuid());
				msg.setTag(event.getClass().getName());
				msg.setMsgID(baseEvent.getUuid());
				msg.setTopic(topic);
				String msgString = JSON.toJSONString(event);
				// 空消息事件也同样投递
				if(Objects.nonNull(msgString)) {
					msg.setBody(msgString.getBytes());
				}
				// 延时投递
				if(baseEvent.getStartDeliverTime() > 0) {
					msg.setStartDeliverTime(baseEvent.getStartDeliverTime());
				}
				producer.send(msg);

				return;
			}
		}

		exchangeEvent(event, eventType);
	}

	@SuppressWarnings("all")
	private Class<? extends BaseEvent<?,?>> getEventClass(String event) {
		if(!events.containsKey(event)) {
			try {
				// 强制类型转换
				Class<? extends BaseEvent<?,?>> clazz = Class.forName(event)
						.asSubclass((Class<BaseEvent<?,?>>)(Class)BaseEvent.class);
				events.put(event, clazz);
			} catch (ClassNotFoundException e) {
				log.error("get event class error: {}", e.getMessage());
			}
		}
		return events.get(event);
	}

	private ResolvableType resolveDefaultEventType(ApplicationEvent event) {
		return ResolvableType.forInstance(event);
	}

	protected void exchangeEvent(final ApplicationEvent event, @Nullable ResolvableType eventType) {
		ResolvableType type = (eventType != null ? eventType : resolveDefaultEventType(event));
		Executor executor = getTaskExecutor();

		Collection<ApplicationListener<?>> listeners = getApplicationListeners(event, type);
		// 执行前再排序，默认是本地cached，会调用实现的 getOrder()方法
		AnnotationAwareOrderComparator.sort((List<?>)listeners);

		for (ApplicationListener<?> listener : listeners) {
			if (executor != null) {
				executor.execute(() -> invokeListener(listener, event));
			}
			else {
				invokeListener(listener, event);
			}
		}
	}


}