package com.ev.charging.tool.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

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
    private String dbPassword = "__OVERRIDE_IN_LOCAL__";

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String v) { baseUrl = v; }
    public String getSmsSendPath() { return smsSendPath; }
    public void setSmsSendPath(String v) { smsSendPath = v; }
    public String getLoginPath() { return loginPath; }
    public void setLoginPath(String v) { loginPath = v; }
    public String getRealnameInfoPath() { return realnameInfoPath; }
    public void setRealnameInfoPath(String v) { realnameInfoPath = v; }
    public String getUploadPath() { return uploadPath; }
    public void setUploadPath(String v) { uploadPath = v; }
    public String getObsSignPath() { return obsSignPath; }
    public void setObsSignPath(String v) { obsSignPath = v; }
    public String getSmsSalt1() { return smsSalt1; }
    public void setSmsSalt1(String v) { smsSalt1 = v; }
    public String getSmsSalt2() { return smsSalt2; }
    public void setSmsSalt2(String v) { smsSalt2 = v; }
    public String getTestSmsCode() { return testSmsCode; }
    public void setTestSmsCode(String v) { testSmsCode = v; }
    public String getDbPassword() { return dbPassword; }
    public void setDbPassword(String v) { dbPassword = v; }
}
