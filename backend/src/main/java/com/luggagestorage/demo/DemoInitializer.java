package com.luggagestorage.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 서버가 켜질 때 체험 계정을 준비하고, 오늘 아직 안 했으면 체험 데이터를 처음 상태로 되돌린다 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class DemoInitializer implements ApplicationRunner {

    private final DemoService demoService;

    @Override
    public void run(ApplicationArguments args) {
        demoService.prepareAccounts();
        demoService.resetOncePerDay();
    }
}
