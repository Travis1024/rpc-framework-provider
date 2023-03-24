package com.space.travis.transport;

/**
 * @ClassName RpcServerTransport
 * @Description 开启Netty服务接口
 * @Author travis-wei
 * @Version v1.0
 * @Data 2023/2/22
 */
public interface RpcServerTransport {
    /**
     * 开启Netty服务接口
     * @param port
     */
    void start(int port);

    /**
     * 关闭Netty服务
     */
    void destory();
}
