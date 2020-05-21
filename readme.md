## 前言

使用Mybatis，通常需要写Entity，Mapper（java、xml），对于小项目很麻烦，有没有只写Entity就可以进行CURD呢？答案是肯定的

## 我们的需求
```
1、只写少量代码，实现CURD
2、少量配置，完成类似ActiveRecord
3、无缝兼容原有的Mapper
4、再包含通用Mapper？
```
## 实现结果

上述需求可以完全满足，而且只需要2个类就搞定上面的需求

1、只需要定义一个 pojo ，就可以完成CURD
```java
@Entity
@Table(name = "user")
public class User {
    String id;
    String name;
    String status;
    String phone;
    String password;
}
```
2、类似baseMapper使用，直接注入（无需写UserMapper）
```java
@Resource
BaseMapper<User> userBaseMapper;
```
3、类似ActiveRecord
```java
User user = new User();
Dal.with(User.class).select(user);
```
4、实现动态SQL查询
```java
User user = new User();
Dal.with(User.class).query(sql -> sql.SELECT("id,name").WHERE("name=#{name}"), user);
```


## 代码

https://github.com/scofier/SimpleCurd
