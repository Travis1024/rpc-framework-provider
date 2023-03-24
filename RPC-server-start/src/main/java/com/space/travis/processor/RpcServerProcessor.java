package com.space.travis.processor;

import com.space.travis.annotation.RPCServer;
import com.space.travis.cache.LocalRpcServerCache;
import com.space.travis.config.RpcServerProperties;
import com.space.travis.pojo.ServiceInfo;
import com.space.travis.register.ServerRegisterService;
import com.space.travis.transport.RpcServerTransport;
import com.sun.xml.internal.bind.v2.TODO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.CommandLineRunner;

import java.net.InetAddress;

/**
 * @ClassName RpcServerProcessor
 * @Description 实现bean后置处理器
 * @Author travis-wei
 * @Version v1.0
 * @Data 2023/2/22
 */
@Slf4j
public class RpcServerProcessor implements BeanPostProcessor, CommandLineRunner {

    @Autowired
    private RpcServerProperties rpcServerProperties;

    @Autowired
    private ServerRegisterService serverRegisterService;

    @Autowired
    private RpcServerTransport rpcServerTransport;

    /**
     * 一个service实例.由name, id, address, port/ssl port, payload(可选)构成, 在Zookeeper中的数据组织结构如下:
     * base path
     *        |_______ service A name (FirstSayService-1.0)
     *                     |__________ instance 1 id --> (serialized ServiceInstance)
     *                                 ---IP(127.0.0.1) + 服务启动端口(rpc.server.port=9091)
     *                     |__________ instance 2 id --> (serialized ServiceInstance)
     *                                 ---IP(127.0.0.1) + 服务启动端口(rpc.server.port=9092)
     *
     *        |_______ service B name (SecondSayService-1.0)
     *                     |__________ instance 1 id --> (serialized ServiceInstance)
     *                                 ---IP(127.0.0.1) + 服务启动端口(rpc.server.port=9091)
     *                     |__________ instance 2 id --> (serialized ServiceInstance)
     *                                 ---IP(127.0.0.1) + 服务启动端口(rpc.server.port=9092)
     */

    /**
     * @MethodName postProcessAfterInitialization
     * @Description 所有bean实例化之后进行处理，获取@RPCServer注解的类
     * 并将其注册到《服务发现构造器》中，成为一个服务实例
     * @Author travis-wei
     * @Data 2023/2/22
     * @param bean
     * @param beanName
     * @Return java.lang.Object
     **/
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

        // 获取bean中被RPCServer注解的信息
        RPCServer annotation = bean.getClass().getAnnotation(RPCServer.class);
        if (annotation != null) {
            try {
                // 创建存储服务实例信息的pojo对象
                ServiceInfo serviceInfo = new ServiceInfo();

                /**
                 * 通过注解获取信息
                 */
                // 获取RPCServer标注的interfacetype实体类的名称
                // FirstSayService
                String serviceName = annotation.interfacetype().getName();
                // 1.0
                String version = annotation.version();
                // FirstSayService-1.0
                String newServiceName = serviceName + "-" + version;

                /**
                 * 服务实例属性赋值
                 */
                // 将即将暴露的服务及其对应的bean在本地进行缓存
                LocalRpcServerCache.store(newServiceName, bean);
                // FirstSayService-1.0
                serviceInfo.setServiceName(newServiceName);
                // 1.0
                serviceInfo.setVersion(version);
                // 通过本机名获取本机ip，一般为127.0.0.1
                serviceInfo.setAddress(InetAddress.getLocalHost().getHostAddress());
                // netty服务启动端口 ｜ rpc.server.port=9091
                serviceInfo.setPort(rpcServerProperties.getPort());
                // 应用名称 ｜ rpc.server.app-name=rpcServerInstance_1
                serviceInfo.setAppName(rpcServerProperties.getAppName());
                // 服务注册
                serverRegisterService.register(serviceInfo);

            } catch (Exception e) {
                log.error("错误：bean后置处理器处理失败！服务注册失败！");
                e.printStackTrace();
            }
        }
        return bean;
    }


    /**
     * @MethodName run
     * @Description 该类继承了CommandLineRunner，其目的是项目启动之前，会预先加载数据（运行run方法）
     *              CommandLineRunner是一个接口，用户可以自定义实现该接口，具体实现run方法
     *              启动RPC服务，处理请求
     * @Author travis-wei
     * @Data 2023/3/1
     * @param args
     * @Return void
     **/
    @Override
    public void run(String... args) throws Exception {
//        Thread thread = new Thread(() -> System.out.println("使用匿名内部类创建 Thread 子类对象"));
//        thread.start();
//        rpcServerTransport.start(rpcServerProperties.getPort());
        new Thread(() -> rpcServerTransport.start(rpcServerProperties.getPort())).start();
        log.info("|--- rpc server :{} start, appName :{} , port :{}", rpcServerTransport, rpcServerProperties.getAppName(), rpcServerProperties.getPort());

        /**
         * 当jvm实例关闭的时候，会执行系统中已经设置的所有通过方法addShutdownHook添加的钩子，当系统执行完这些钩子后，jvm实例才会关闭。
         * 所以这些钩子可以在jvm实例关闭的时候进行内存清理、对象销毁等操作。
         * 每个application对应一个独立的jvm实例，addShutdownHook是此jvm实例的钩子
         * 每个application都对应一个独立的curator客户端，对该独立的curator客户端进行关闭无影响
         * 描述：在当前服务提供者关闭前，清除zookeper中有关当前服务提供者的相关信息
         */
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                // 服务实例注销
                // 一般为127.0.0.1
                String registryAddr = rpcServerProperties.getRegistryAddr();
                // netty服务启动端口 ｜ rpc.server.port=9091
                Integer port = rpcServerProperties.getPort();

                ServiceInfo serviceInfo = new ServiceInfo();
                serviceInfo.setAddress(registryAddr);
                serviceInfo.setPort(port);
                serverRegisterService.unRegister(serviceInfo);

                // 将curator客户端的服务发现功能资源关闭
                serverRegisterService.destroy();

                //关闭Netty服务
                rpcServerTransport.destory();

            } catch (Exception e) {
                log.error(String.valueOf(e));
            }
        }));
    }
}
