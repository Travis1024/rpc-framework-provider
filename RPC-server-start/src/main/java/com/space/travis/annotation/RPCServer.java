package com.space.travis.annotation;

import org.springframework.stereotype.Service;

import java.lang.annotation.*;

/**
 * @ClassName RPCServer
 * @Description 定义RPCServer注解
 * @Author travis-wei
 * @Version v1.0
 * @Data 2023/2/21
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Service
public @interface RPCServer {

    /**
     * 通过注解暴露服务接口
     * @return
     */
    Class<?> interfacetype() default Object.class;

    /**
     * 定义服务版本
     * @return
     */
    String version() default "1.0";
}
