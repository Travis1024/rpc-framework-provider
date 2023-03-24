package com.space.travis.service;

import com.space.travis.annotation.RPCServer;
import com.space.travis.api.SecondSayService;
import lombok.extern.slf4j.Slf4j;

/**
 * @ClassName SecondSayServiceImpl
 * @Description 服务接口实现
 * @Author travis-wei
 * @Version v1.0
 * @Data 2023/2/21
 */
@Slf4j
@RPCServer(interfacetype = SecondSayService.class, version = "1.0")
public class SecondSayServiceImpl implements SecondSayService {

    @Override
    public String saySecond(String name) {
        String result = "SecondSayService调用成功! 这里是服务提供者,收到的姓名为:" + name;
        log.info("｜——> " + result);
        return result;
    }

    @Override
    public String saySecondTwice(String name) {
        String result = "SecondSayService调用成功! 这里是服务提供者,收到的姓名为:" + name + " ｜ " +
                        "SecondSayService调用成功! 这里是服务提供者,收到的姓名为:" + name;
        log.info("｜——> " + result);
        return result;
    }
}
