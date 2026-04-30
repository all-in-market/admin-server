package com.example.allinmarket;

import com.example.allinmarket.common.config.TestRedisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
class AllInMarketApplicationTests {

    @Test
    void contextLoads() {
    }

}
