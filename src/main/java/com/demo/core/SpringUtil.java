package com.demo.core;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

@Component
public class SpringUtil implements ApplicationContextAware {
    private static ApplicationContext applicationContext;
    //= WebApplicationContextUtils.getWebApplicationContext(RequestUtil.getSession().getServletContext());

    public SpringUtil() {}

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        if (applicationContext == null) {
            applicationContext = context;
        }
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public static Object getBean(String name) {
        return getApplicationContext().getBean(name);
    }

    public static <T> T getBean(Class<T> clazz) {
        return getApplicationContext().getBean(clazz);
    }

    /**
     * 按照spring默认的bean名称加载bean
     */
    @SuppressWarnings("unchecked")
    public static <T> T getBeanIdentity(Class<T> clazz) {
        return (T)getApplicationContext().getBean(ClassUtils.getShortNameAsProperty(clazz));
    }

    public static <T> T getBean(String beanName, Class<T> clazz) {
        return getApplicationContext().getBean(beanName, clazz);
    }
}