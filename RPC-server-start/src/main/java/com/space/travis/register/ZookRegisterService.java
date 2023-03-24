package com.space.travis.register;

import com.space.travis.config.RpcCuratorProperties;
import com.space.travis.pojo.ServiceInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

/**
 * @ClassName ZookRegisterService
 * @Description Zookeeper服务注册
 * @Author travis-wei
 * @Version v1.0
 * @Data 2023/2/22
 */
@Slf4j
public class ZookRegisterService implements ServerRegisterService{

    private ServiceDiscovery<ServiceInfo> serviceDiscovery;

    private RpcCuratorProperties rpcCuratorProperties;

    /**
     * @MethodName ZookRegisterService
     * @Description 构造方法，进行curator客户端和《服务发现构造器》的创建及初始化工作
     *
     * curator相当于原生zookeeper(较难使用)的客户端，对zookeeper进行了包装
     * Curator Service Discovery就是为了解决服务发现问题而生的，它对此抽象出了ServiceInstance、ServiceProvider、ServiceDiscovery三个接口，通过它我们可以很轻易的实现服务发现。
     *
     * @Author travis-wei
     * @Data 2023/2/22
     * @param registerAddr
     * @Return
     **/
    public ZookRegisterService(String registerAddr, RpcCuratorProperties rpcCuratorProperties) {
        this.rpcCuratorProperties = rpcCuratorProperties;
        // 初始化、创建、运行curator客户端
        ExponentialBackoffRetry exponentialBackoffRetry = new ExponentialBackoffRetry(rpcCuratorProperties.getElapsedTimeMs(), rpcCuratorProperties.getRetryCount());
        CuratorFramework curatorFramework = CuratorFrameworkFactory.newClient(registerAddr, exponentialBackoffRetry);
        curatorFramework.start();

        /**
         * 一个service实例.由name, id, address, port/ssl port, payload(可选)构成, 在Zookeeper中的数据组织结构如下:
         * base path
         *        |_______ service A name (FirstSayService-1.0)
         *                     |__________ instance 1 id --> (serialized ServiceInstance)
         *                                 ---本机IP(127.0.0.1) + 服务启动端口(rpc.server.port=9091)
         *                     |__________ instance 2 id --> (serialized ServiceInstance)
         *                                 ---本机IP(127.0.0.1) + 服务启动端口(rpc.server.port=9092)
         *
         *        |_______ service B name (SecondSayService-1.0)
         *                     |__________ instance 1 id --> (serialized ServiceInstance)
         *                                 ---本机IP(127.0.0.1) + 服务启动端口(rpc.server.port=9091)
         *                     |__________ instance 2 id --> (serialized ServiceInstance)
         *                                 ---本机IP(127.0.0.1) + 服务启动端口(rpc.server.port=9092)
         *        |_______ ...
         *
         * === 解释.builder(ServiceInfo.class)及.serializer(serviceInfoJsonInstanceSerializer)：
         * builder()的参数定义了payloadClass, 也就是上面数据结构图里的(serialized ServiceInstance)中的内容.
         * 我们也可以自定义一个复杂的class用来承载更多的信息; 比如 ServiceInfo.class, 不过需要告诉系统,
         * 怎么才能把我们自定义的类"变成"Zookeeper里的payload字符串, 方法就是提供serializer，通过json的序列化来完成payload
         */

        // 初始化、创建、运行《服务发现构造器》
        try {
            JsonInstanceSerializer<ServiceInfo> serviceInfoJsonInstanceSerializer = new JsonInstanceSerializer<>(ServiceInfo.class);
            // 使用ServiceDiscoveryBuilder对ServiceDiscovery进行创建
            serviceDiscovery = ServiceDiscoveryBuilder
                    .builder(ServiceInfo.class)
                    .client(curatorFramework)
                    .serializer(serviceInfoJsonInstanceSerializer)
                    .basePath(rpcCuratorProperties.getZookBasePath())
                    .build();
            serviceDiscovery.start();
        } catch (Exception e) {
            log.error("错误：ZookRegisterService构造函数，创建《服务发现构造器》ServiceDiscovery时出错！");
            e.printStackTrace();
        }
    }

    /**
     * @MethodName register
     * @Description 创建服务实例
     * @Author travis-wei
     * @Data 2023/2/22
     * @param serviceInfo
     * @Return void
     **/
    @Override
    public void register(ServiceInfo serviceInfo) {
        try {
            ServiceInstance<ServiceInfo> serviceInstance = ServiceInstance.<ServiceInfo>builder()
                    // FirstSayService-1.0
                    .name(serviceInfo.getServiceName())
                    // 一般为127.0.0.1
                    .address(serviceInfo.getAddress())
                    // netty服务启动端口 ｜ rpc.server.port=9091
                    .port(serviceInfo.getPort())
                    .payload(serviceInfo)
                    .build();
            serviceDiscovery.registerService(serviceInstance);
        } catch (Exception e) {
            log.error("错误：ZookRegisterService类注册方法register，创建服务实例时出错！");
            e.printStackTrace();
        }
    }

    @Override
    public void unRegister(ServiceInfo serviceInfo) {
        try {
            // 目标是将地址和端口能匹配上的服务都进行注销，例如: 127.0.0.1:9091
            ServiceInstance<ServiceInfo> serviceInstance = ServiceInstance.<ServiceInfo>builder()
                    .address(serviceInfo.getAddress())
                    .port(serviceInfo.getPort())
                    .payload(serviceInfo)
                    .build();
            serviceDiscovery.unregisterService(serviceInstance);
        } catch (Exception e) {
            log.error("错误：ZookRegisterService类注册方法unRegister，销毁服务实例时出错！");
            e.printStackTrace();
        }
    }

    /**
     * @MethodName destroy
     * @Description 服务发现销毁，关闭curator客户端的服务发现功能
     * @Author travis-wei
     * @Data 2023/3/1
     * @param
     * @Return void
     **/
    @Override
    public void destroy() {
        try {
            serviceDiscovery.close();
        } catch (IOException e) {
            log.error("错误：ZookRegisterService类destroy，关闭《服务发现构造器》时出错！");
            e.printStackTrace();
        }

    }
}
