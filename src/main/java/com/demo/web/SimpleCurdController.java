package com.demo.web;

import com.demo.core.BaseMapper;
import com.demo.core.Dal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author sk
 */
@RequestMapping("/simpleCurd")
@RestController
public class SimpleCurdController {

    @Resource
    BaseMapper<User> userBaseMapper;

    @GetMapping("/test1")
    public Object test1(User user) {
        return userBaseMapper.select(user);
    }

    @GetMapping("/test2")
    public Object test2(User user) {
        return Dal.with(User.class).query(sql -> sql.SELECT("id,name").WHERE("name=#{name}"), user);
    }

    @GetMapping("/test3")
    public Object test3(User user) {
        return Dal.with(User.class).query(sql -> {

            sql.WHERE(user.getPhone() != null, "phone=#{phone}");

            return sql.SELECT("id,name").WHERE("name=#{name}");

            }, user);
    }

    @GetMapping("/test4")
    public Object test4(String[] ids) {
        return Dal.with(User.class).selectByColumn("name",ids);
    }

}
