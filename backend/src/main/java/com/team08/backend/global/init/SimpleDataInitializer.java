package com.team08.backend.global.init;

import com.team08.backend.global.init.DataSeeder.SeedConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 일반 개발/시연용 데이터.
 * <p>
 * 1) {@link DataSeeder#seed} 로 데이터 그래프를 소~중량으로 만들고,
 * 2) {@link DataSeeder#seedDemoScenarios()} 로 강좌 생명주기·환불·학습 흐름·영상 수정요청을 다양한 상태로 펼친다.
 * <p>
 * 엣지/예외 케이스를 "의미있는 이름"으로 또렷이 보여주는 쪽은 {@link DemoDataInitializer}(demo 모드)다.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class SimpleDataInitializer {

    /** 강사 6 · 강좌 18(6x3) · 챕터 4 · 영상 3 · 수강생 80 (강좌당 수강생 다수 → 대시보드 가시성) */
    private static final SeedConfig CONFIG = new SeedConfig(6, 3, 4, 3, 80, 500);

    private final DataSeeder dataSeeder;

    public void init() {
        // 이미 데이터가 있으면 seed() 가 건너뛰고 false 를 반환한다.
        // 그 경우 비멱등인 시나리오 보강도 함께 건너뛴다.
        if (!dataSeeder.seed(CONFIG)) {
            log.info("[DataInit] 이미 데이터가 존재해 시나리오 보강을 건너뜀");
            return;
        }
        dataSeeder.seedDemoScenarios();
    }
}
