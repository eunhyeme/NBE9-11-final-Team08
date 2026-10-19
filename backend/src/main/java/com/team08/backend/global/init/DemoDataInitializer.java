package com.team08.backend.global.init;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * demo 모드 데이터 — "이름만 봐도 어떤 케이스인지 보이는" 페르소나 중심의 엣지/예외 쇼케이스.
 * <p>
 * {@link DataSeeder#seedPersonaShowcase()} 로 의미있는 이름의 소수 계정(판매자 5·수강생 5·관리자)을
 * 각자 한 가지 상태/흐름을 또렷이 보여주도록 손으로 구성한다.
 * <p>
 * 풍부한 일반 데이터는 {@link SimpleDataInitializer}(simple 모드)가 담당한다.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DemoDataInitializer {

    private final DataSeeder dataSeeder;

    public void init() {
        dataSeeder.seedPersonaShowcase();
    }
}
