package com.space.travis.service;

import com.space.travis.annotation.RPCServer;
import com.space.travis.api.FirstSayService;
import lombok.extern.slf4j.Slf4j;

/**
 * @ClassName FirstSayServiceImpl
 * @Description 服务接口实现
 * @Author travis-wei
 * @Version v1.0
 * @Data 2023/2/21
 */
@Slf4j
@RPCServer(interfacetype = FirstSayService.class, version = "1.0")
public class FirstSayServiceImpl implements FirstSayService {

    @Override
    public String sayFirst(String name) {
        String result = "FirstSayService调用成功! 这里是服务提供者,收到的姓名为:" + name;
        log.info("｜——> " + result);
        return result;
    }

    @Override
    public String sayFirstTwice(String name) {
        String result = "FirstSayService调用成功! 这里是服务提供者,收到的姓名为:" + name + " ｜ " +
                        "FirstSayService调用成功! 这里是服务提供者,收到的姓名为:" + name;
        log.info("｜——> " + result);
        return result;
    }
}
