## 使用说明

1、配置rocketmq，包括topic ： com.demo.springeventstore.core.SimpleApplicationEventMulticaster
2、启动测试


## 代码说明

1、因为需要对事件进行JSON序列化和反序列化，所以 Event的构造函数需要加上jackson的序列化注解：
@JsonCreator
@JsonProperty("source")