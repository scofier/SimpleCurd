package com.demo.springeventstore.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JSON {
    private static final Logger log = LoggerFactory.getLogger(JSON.class);


    private ObjectMapper objectMapper;

    private JSON(){}

    private static class Holder {
        private static final JSON instance = new JSON();
        static {
            instance.objectMapper = new ObjectMapper();
            instance.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }
    }


    public static ObjectMapper getObjectMapper() {
        return Holder.instance.objectMapper;
    }

    public static String toJSONString(Object obj) {
        try {
            return getObjectMapper().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            log.error("JsonProcessingException: {}", e.getMessage());
        }
        return null;
    }

    public static <T> T toObject(String str, Class<T> clzz) {
        try {
            return getObjectMapper().readValue(str, clzz);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }
}
