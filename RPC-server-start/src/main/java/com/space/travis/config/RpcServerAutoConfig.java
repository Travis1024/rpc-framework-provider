package com.space.travis.config;

import com.space.travis.processor.RpcServerProcessor;
import com.space.travis.register.ServerRegisterService;
import com.space.travis.register.ZookRegisterService;
import com.space.travis.transport.NettyRpcServer;
import com.space.travis.transport.RpcServerTransport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @ClassName RpcServerAutoConfig
 * @Description RpcServer自动配置类，已在spring.factories中开启自动配置
 * @Author travis-wei
 * @Version v1.0
 * @Data 2023/2/22
 */
@EnableConfigurationProperties({RpcServerProperties.class, RpcCuratorProperties.class})
@Configuration
@Slf4j
public class RpcServerAutoConfig {

    @Autowired
    private RpcServerProperties rpcServerProperties;

    @Autowired
    private RpcCuratorProperties rpcCuratorProperties;

    /**
     * @MethodName getServerRegisterObject
     * @Description 创建zookeeper注册中心，并将其注入到spring容器中
     *              当bean被注册之后,如果而注册相同类型的bean,就不会成功,它会保证你的bean只有一个，所以zookeeper注册中心的bean只有一个
     * @Author travis-wei
     * @Data 2023/3/1
     * @param
     * @Return com.space.travis.register.ServerRegisterService
     **/
    @Bean
    @ConditionalOnMissingBean
    public ServerRegisterService getServerRegisterObject() {
        // 配置注册中心
        // rpcServerProperties.getRegistryAddr()为注册中心地址：140.246.171.8:2181
        log.info("----------zookeeper >>> " + rpcServerProperties.getRegistryAddr() + "----------");
        return new ZookRegisterService(rpcServerProperties.getRegistryAddr(), rpcCuratorProperties);
    }

    /**
     * @MethodName rpcServerTransport
     * @Description 创建netty服务端，并将其注入到spring容器中
     * @Author travis-wei
     * @Data 2023/3/1
     * @param
     * @Return com.space.travis.transport.RpcServerTransport
     **/
    @Bean
    @ConditionalOnMissingBean(RpcServerTransport.class)
    public RpcServerTransport rpcServerTransport(){
        // 该类中包含netty服务开启方法和销毁方法
        return new NettyRpcServer();
    }


    /**
     * @MethodName rpcServerProcessor
     * @Description 创建bean后置处理器，并将其注入到spring容器中
     * @Author travis-wei
     * @Data 2023/3/1
     * @param
     * @Return com.space.travis.processor.RpcServerProcessor
     **/
    @Bean
    @ConditionalOnMissingBean(RpcServerProcessor.class)
    public RpcServerProcessor rpcServerProcessor() {
        // 该类中包含注解的解析、服务实例的注册
        // 实现CommandLineRunner接口的run方法、调用netty服务开启方法
        // 添加钩子，在jvm关闭之前，执行钩子（钩子的任务为：在jvm关闭之前，把服务实例从zookeeper中清除）
        return new RpcServerProcessor();
    }
}
