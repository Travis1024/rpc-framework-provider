package com.space.travis.cache;

import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName LocalRpcServerCache
 * @Description 将已经暴露的服务及其对应的bean在本地进行缓存
 * @Author travis-wei
 * @Version v1.0
 * @Data 2023/2/22
 */
public class LocalRpcServerCache {

    private static final Map<String, Object> map = new HashMap<>();

    /**
     * @MethodName store
     * @Description 将已经暴露的服务在本地进行缓存，KEY = "FirstSayService-1.0"， Value为有@RPCServer注解的bean，此操作的目的
     *              主要是为了在进行反射调用请求的方法时，能够获取到bean的class信息，所以value值可以覆盖掉。
     * @Author travis-wei
     * @Data 2023/2/22
     * @param serviceName
     * @param bean
     * @Return void
     **/
    public static void store(String serviceName, Object bean) {
        map.put(serviceName, bean);
    }

    /**
     * @MethodName getBeanByServiceName
     * @Description 通过服务名获取bean对象
     * @Author travis-wei
     * @Data 2023/2/22
     * @param serviceName	
     * @Return java.lang.Object
     **/
    public static Object getBeanByServiceName(String serviceName) {
        return map.get(serviceName);
    }
}
