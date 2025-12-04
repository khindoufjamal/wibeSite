package com.stet.t2s.sabr.domain.swift;

import com.stet.t2s.sabr.config.SwiftConfiguration;
import com.stet.t2s.sabr.config.SwiftConfiguration.SagEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SagConnectionServiceTest {

    private SwiftConfiguration config;
    private RetryTemplate retryTemplate;
    private SagConnectionService service; // spy

    @BeforeEach
    void setUp() {
        config = mock(SwiftConfiguration.class);

        // RetryTemplate : 3 tentatives, pas d’attente en test
        retryTemplate = new RetryTemplate();

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
                3,
                Map.of(NoSagAvailableException.class, true) // si tu es en Java 8 => utilise une HashMap
        );
        retryTemplate.setRetryPolicy(retryPolicy);

        FixedBackOffPolicy backOff = new FixedBackOffPolicy();
        backOff.setBackOffPeriod(0L); // pas de pause entre tentatives en TU
        retryTemplate.setBackOffPolicy(backOff);

        // spy sur l’implémentation réelle
        SagConnectionService realService = new SagConnectionService(config, retryTemplate);
        service = Mockito.spy(realService);

        // 4 SAG dans la conf
        when(config.getSagList()).thenReturn(List.of(
                sag("sag1", 48001),
                sag("sag2", 48002),
                sag("sag3", 48003),
                sag("sag4", 48004)
        ));
    }

    private SagEndpoint sag(String host, int port) {
        SagEndpoint s = new SagEndpoint();
        s.setHostname(host);
        s.setPort(port);
        s.setTimeout(30L);
        return s;
    }

    // ===== TEST 1 : succès dès la première tentative =====

    @Test
    void connectWithRetry_should_return_handle_on_first_attempt() throws Exception {
        SagHandle handle = mock(SagHandle.class);

        // Peu importe la SAG, la connexion réussit
        doReturn(handle).when(service).connectSingleSag(any());

        SagHandle result = service.connectWithRetry();

        assertThat(result).isSameAs(handle);
        // Une seule connexion (première SAG du premier cycle)
        verify(service, times(1)).connectSingleSag(any());
    }

    // ===== TEST 2 : 1er cycle KO sur les 4 SAG, 2ème cycle OK dès la 1ère SAG =====

    @Test
    void connectWithRetry_should_retry_cycle_when_all_sags_fail_once_then_succeed() throws Exception {
        SagHandle handle = mock(SagHandle.class);
        AtomicInteger counter = new AtomicInteger(0);

        // 4 premiers appels -> KO (cycle 1), 5e appel -> OK (début cycle 2)
        doAnswer(invocation -> {
            int callIndex = counter.incrementAndGet();
            if (callIndex <= 4) {       // 4 SAG du 1er cycle
                throw new RuntimeException("SAG down");
            }
            return handle;              // 1ère SAG du 2e cycle
        }).when(service).connectSingleSag(any());

        SagHandle result = service.connectWithRetry();

        assertThat(result).isSameAs(handle);
        // 4 KO + 1 OK = 5 appels au total
        verify(service, times(5)).connectSingleSag(any());
    }

    // ===== TEST 3 : 3 cycles complets KO (4 SAG * 3) => exception =====

    @Test
    void connectWithRetry_should_throw_NoSagAvailableException_after_all_attempts_fail() throws Exception {
        // Tous les appels à connectSingleSag jettent une exception
        doThrow(new RuntimeException("SAG down"))
                .when(service).connectSingleSag(any());

        assertThatThrownBy(() -> service.connectWithRetry())
                .isInstanceOf(NoSagAvailableException.class);

        // 4 SAG * 3 tentatives = 12 appels
        verify(service, times(12)).connectSingleSag(any());
    }
            }
