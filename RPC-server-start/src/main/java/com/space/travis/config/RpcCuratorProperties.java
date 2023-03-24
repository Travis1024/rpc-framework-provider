package com.space.travis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @ClassName RpcCuratorProperties
 * @Description Curator配置类
 * @Author travis-wei
 * @Version v1.0
 * @Data 2023/2/22
 */
@Data
@ConfigurationProperties(prefix = "curator")
public class RpcCuratorProperties {
    /**
     * 定义重试次数
     */
    private Integer retryCount;
    /**
     * 定义间隔时间、以毫秒为单位
     */
    private Integer elapsedTimeMs;
    /**
     * 会话超时时间设置、以毫秒为单位
     */
    private Integer sessionTimeMs;
    /**
     * 连接超时时间设置、以毫秒为单位
     */
    private Integer connectionTimeMs;
    /**
     * 设置zookeeper basepath
     */
    private String zookBasePath;
}
