package com.ev.charging.tool.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 充电接口配置（从 application.yml 注入）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "charging")
public class ChargingProperties {

    private String baseUrl = "https://charge-dev.jieyoucloud.com/api/charge";
    private String smsSendPath = "/auth/sendSmsCode/{phone}";
    private String loginPath = "/auth/verifySmsCode";
    private String realnameInfoPath = "/chargeUser/getRealNameInfo";
    private String uploadPath = "/common/upload";
    private String obsSignPath = "/chargeUser/getObsSign";

    private String smsSalt1;
    private String smsSalt2;
    private String testSmsCode = "666666";
}
