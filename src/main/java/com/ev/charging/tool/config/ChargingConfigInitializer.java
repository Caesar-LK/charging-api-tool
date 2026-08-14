package com.ev.charging.tool.config;

import com.ev.charging.tool.util.Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Spring Boot 启动后，将配置注入到 Config 静态类。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChargingConfigInitializer {

    private final ChargingProperties properties;

    @EventListener(ApplicationReadyEvent.class)
    public void initConfig() {
        Config.init(properties);
        log.info("[ChargingConfig] 配置初始化完成");
    }
}
