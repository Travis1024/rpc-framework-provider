package com.space.travis.register;

import com.space.travis.pojo.ServiceInfo;

/**
 * @ClassName ServerRegisterService
 * @Description 服务注册发现接口
 * @Author travis-wei
 * @Version v1.0
 * @Data 2023/2/22
 */
public interface ServerRegisterService {

    /**
     * 服务实例注册
     * @param serviceInfo
     */
    void register(ServiceInfo serviceInfo);

    /**
     * 服务实例注销
     * @param serviceInfo
     */
    void unRegister(ServiceInfo serviceInfo);

    /**
     * 关闭服务实例构造器，关闭curator客户端的服务发现功能
     */
    void destroy();
}
