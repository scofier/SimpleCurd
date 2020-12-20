package com.demo.web;


import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author sk
 */

@Table(name = "user")
public class User {

    String id;

    String name;

    String phone;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }


}