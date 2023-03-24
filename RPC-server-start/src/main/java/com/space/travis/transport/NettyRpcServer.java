package com.space.travis.transport;

import com.space.travis.coder.RpcDecoder;
import com.space.travis.coder.RpcEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @ClassName NettyRpcServer
 * @Description Netty整合websocket 服务端
 *
 * 运行流程:
 * 1.创建一个ServerBootstrap的实例引导和绑定服务器
 * 2.创建并分配一个NioEventLoopGroup实例以进行事件的处理,比如接收连接和读写数据
 * 3.指定服务器绑定的本地的InetSocketAddress
 * 4.使用一个NettyServerHandler的实例初始化每一个新的Channel
 * 5.调用ServerBootstrap.bind()方法以绑定服务器
 *
 * @Author travis-wei
 * @Version v1.0
 * @Data 2023/2/23
 */
@Slf4j
public class NettyRpcServer implements RpcServerTransport{

    /**
     * EventLoop接口
     * NioEventLoop中维护了一个线程和任务队列,支持异步提交任务,线程启动时会调用NioEventLoop的run方法,执行I/O任务和非I/O任务
     * I/O任务即selectionKey中的ready的事件,如accept,connect,read,write等,由processSelectedKeys方法触发
     * 非I/O任务添加到taskQueue中的任务,如register0,bind0等任务,由runAllTasks方法触发
     * 两种任务的执行时间比由变量ioRatio控制,默认为50,则表示允许非IO任务执行的事件与IO任务的执行时间相等
     */

    /**
     * （1）NioEventLoopGroup是一个处理 I/O 操作的多线程事件循环。NettyEventLoopGroup为不同类型的传输提供了不同的实现,在此案例中实现了一个服务器端应用程序，
     *      因此NioEventLoopGroup将使用两个。第一个，通常称为“boss”，接受传入连接。第二个，通常称为“worker”，一旦boss接受连接并向 worker 注册接受的连接，就会处理已接受连接的流量。
     *      使用了多少线程以及它们如何映射到创建的Channel线程取决于EventLoopGroup实现，甚至可以通过构造函数进行配置。
     * （2）ServerBootstrap是一个设置服务器的辅助类。您可以直接使用 a 来设置服务器Channel。但是请注意，这是一个繁琐的过程，在大多数情况下您不需要这样做。
     * （3）在这里，我们指定使用NioServerSocketChannel用于实例化新Channel接受传入连接的类。
     * （4）此处指定的处理程序将始终由新接受的Channel. 是ChannelInitializer一个特殊的处理程序，旨在帮助用户配置一个新的Channel.
     *      您很可能希望通过添加一些处理程序来配置ChannelPipeline新的Channel，例如DiscardServerHandler实现您的网络应用程序。
     *      随着应用程序变得复杂，您可能会向管道添加更多处理程序，并最终将这个匿名类提取到顶级类中。
     * （5）您还可以设置特定于实现的参数Channel。我们正在编写一个 TCP/IP 服务器，因此我们可以设置套接字选项，例如tcpNoDelay和keepAlive。
     *      请参阅 apidocsChannelOption和具体ChannelConfig实现以获得有关支持的 s 的概述ChannelOption。
     * （6）你注意到option()了吗childOption()？option()适用于NioServerSocketChannel接受传入连接的。childOption()是为Channelparent 接受的 s ServerChannel，NioSocketChannel在这种情况下。
     * （7）我们准备好了。剩下的就是绑定到端口并启动服务器。8080在这里，我们绑定到机器中所有NIC（网络接口卡）的端口。bind()您现在可以根据需要多次调用该方法（使用不同的绑定地址。）
     */
    private final EventLoopGroup bossGroup = new NioEventLoopGroup();           //（1）
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();


    @Override
    public void start(int port) {
        log.info("-----------Netty 端口启动: {} -----------", port);
        try {
            /**
             * Bootstrap为netty客户端的启动器，ServerBootstrap是netty服务端的启动器
             * ServerBootStrap作为服务端，主要用于接收客户端连接请求，以及客户端发送的信息，并返回响应给客户端。
             */
            ServerBootstrap serverBootstrap = new ServerBootstrap();                //（2）
            serverBootstrap
                    // 设置线程组
                    .group(bossGroup, workerGroup)
                    // 非阻塞异步服务端TCP Socket连接
                    // 设置执行channel(客户端为NioSocketChannel，服务端为NioServerSocketChannel)
                    .channel(NioServerSocketChannel.class)                          //（3）
                    .childHandler(new ChannelInitializer<SocketChannel>() {         //（4）
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline()
                                    .addLast(new RpcEncoder<>())            // outbound 协议编码
                                    .addLast(new RpcDecoder())              // inbound 协议解码
                                    .addLast(new NettyRpcServerHandler());   // inbound 请求处理器
                        }
                    })
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            // 通过本机名去获取本机ip
            String serverAddress = InetAddress.getLocalHost().getHostAddress();
            log.info("-------------Server is ready------------");
            log.info("server addr {} started on port {}", serverAddress, port);
            // bind and start to accept incoming connections
            ChannelFuture channelFuture = serverBootstrap.bind(serverAddress, port).sync();
            log.info("-----------Server is starting-----------");


            // 阻塞main函数继续往下执行，防止finally的语句块被触发.这里调起netty服务的线程就是main函数
            // 而main函数使用了finally，不管程序发生什么只要运行结束了，finally中的语句一定会执行，也就关闭了netty服务。
            // 等待服务端socket结束,关闭netty服务，和finally搭配使用
            channelFuture.channel().closeFuture().sync();

        } catch (Exception e) {
            log.error(String.valueOf(e));
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    @Override
    public void destory() {
        if (!bossGroup.isShutdown()) {
            bossGroup.shutdownGracefully();
        }
        if (!workerGroup.isShutdown()) {
            workerGroup.shutdownGracefully();
        }
    }
}
