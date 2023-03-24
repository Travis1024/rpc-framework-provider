package com.space.travis.transport;

import com.space.travis.cache.LocalRpcServerCache;
import com.space.travis.enumList.MessageStatusEnum;
import com.space.travis.enumList.MessageTypeEnum;
import com.space.travis.pojo.MessageHeader;
import com.space.travis.pojo.MessageProtocol;
import com.space.travis.pojo.RpcRequestBody;
import com.space.travis.pojo.RpcResponseBody;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName NettyRpcServerHandler
 * @Description 服务端处理消息解码后的消息请求，处理请求
 * @Author travis-wei
 * @Version v1.0
 * @Data 2023/2/28
 */
@Slf4j
public class NettyRpcServerHandler extends SimpleChannelInboundHandler<MessageProtocol<RpcRequestBody>> {

    private final ThreadPoolExecutor threadPoolExecutor =  new ThreadPoolExecutor(10, 20, 5, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100), new ThreadPoolExecutor.AbortPolicy());

    /**
     * 接收到客户端信息时执行的方法
     * @param channelHandlerContext
     * @param rpcRequestBodyMessageProtocol
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, MessageProtocol<RpcRequestBody> rpcRequestBodyMessageProtocol) throws Exception {
        // 线程执行处理请求任务
        threadPoolExecutor.submit(() -> {
            log.info("========接收到请求消息,且解码成功 -> 正在处理请求========");
            /**
             * 创建消息响应（响应体、请求和响应共用一个响应头）
             */
            MessageProtocol<RpcResponseBody> responseMessageProtocol = new MessageProtocol<>();
            RpcResponseBody responseMessageBody = new RpcResponseBody();
            MessageHeader messageHeader = rpcRequestBodyMessageProtocol.getMessageHeader();

            // 设置响应头的消息类型为response
            messageHeader.setMessageType(MessageTypeEnum.RESPONSE.getCode());
            // 设置消息状态为成功接受
            messageHeader.setStatus(MessageStatusEnum.SUCCESS.getCode());

            try {
                // 反射调用方法获取返回结果
                RpcRequestBody messageBody = rpcRequestBodyMessageProtocol.getMessageBody();
                Object result = handle(messageBody);
                responseMessageBody.setData(result);
            } catch (Throwable throwable) {
                log.error(String.valueOf(throwable));
                messageHeader.setStatus(MessageStatusEnum.FAIL.getCode());
                responseMessageBody.setMessage(String.valueOf(throwable));
            } finally {
                responseMessageProtocol.setMessageBody(responseMessageBody);
                responseMessageProtocol.setMessageHeader(messageHeader);
            }
            log.info("请求处理成功！");
            // 把数据写回去
            channelHandlerContext.writeAndFlush(responseMessageProtocol);

        });
    }

    /**
     * @MethodName handle
     * @Description 反射获取数据
     * @Author travis-wei
     * @Data 2023/3/2
     * @param requestBody
     * @Return java.lang.Object 返回反射调用的方法返回的结果
     **/
    private Object handle(RpcRequestBody requestBody) {
        Object bean = LocalRpcServerCache.getBeanByServiceName(requestBody.getServiceName());
        if (bean == null) {
            throw new RuntimeException("错误：反射调用的服务不存在！服务名称：" + requestBody.getServiceName());
        }
        try {
            Method method = bean.getClass().getMethod(requestBody.getMethod(), requestBody.getParameterTypes());
            return method.invoke(bean, requestBody.getParameters());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.error("错误：反射调用方法出现错误！" + e);
            throw new RuntimeException(e);
        }
    }
}
