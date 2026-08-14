package com.ev.charging.tool.util;

import com.ev.charging.tool.util.login.LoginModule;
import com.ev.charging.tool.util.login.LoginResult;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 测试账号工厂。
 */
@Slf4j
public class TestAccountFactory {

    private static final String PHONE_PREFIX = "138";
    private static final AtomicLong COUNTER = new AtomicLong(System.currentTimeMillis() % 10000);

    private TestAccountFactory() {
    }

    public static TestAccount createAndLogin() {
        TestAccount account = create();
        account.login();
        return account;
    }

    public static TestAccount create() {
        String phone = generatePhone();
        return new TestAccount(phone, Config.getTestSmsCode());
    }

    private static String generatePhone() {
        long seq = COUNTER.incrementAndGet();
        return PHONE_PREFIX + String.format("%08d", seq);
    }

    public static class TestAccount {
        private final String phone;
        private final String smsCode;
        private String token;
        private boolean isRegistered;

        public TestAccount(String phone, String smsCode) {
            this.phone = phone;
            this.smsCode = smsCode;
        }

        public String getPhone() {
            return phone;
        }

        public String getSmsCode() {
            return smsCode;
        }

        public String getToken() {
            return token;
        }

        public boolean isRegistered() {
            return isRegistered;
        }

        public void login() {
            LoginResult result = LoginModule.login(phone);
            this.token = result.getToken();
            this.isRegistered = result.isSuccess() && "200".equals(result.getCode());
            log.info("[TestAccount] phone={}, registered={}, tokenLen={}",
                    phone, isRegistered, token != null ? token.length() : 0);
        }

        public void logout() {
            if (token != null) {
                LoginModule.logout();
                this.token = null;
            }
        }
    }
}
