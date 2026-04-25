package com.xingchen.backend;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("CI环境无Redis等外部服务，跳过")
class BackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
