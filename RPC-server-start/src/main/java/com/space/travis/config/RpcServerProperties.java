package com.space.travis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @ClassName RpcServerProperties
 * @Description 存储当前服务的相关信息（初始在Provider的application.properties配置文件中）
 * 在RpcServerAutoConfig中开启的配置属性
 * @Author travis-wei
 * @Version v1.0
 * @Data 2023/2/22
 */
@Data
@ConfigurationProperties(prefix = "rpc.server")
public class RpcServerProperties {
    /**
     * 服务启动端口
     */
    private Integer port;
    /**
     * 应用名称
     */
    private String appName;
    /**
     * 注册中心地址
     */
    private String registryAddr;
}
