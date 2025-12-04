import com.ttet.t2s.sabr.config.SwiftConfiguration;
import com.ttet.t2s.sabr.config.SwiftConfiguration.SagEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.List;
import java.util.Map;

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

        // RetryTemplate : 3 tentatives, pas de vraie attente en test
        retryTemplate = new RetryTemplate();
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
                3,
                Map.of(NoSagAvailableException.class, true)
        );
        retryTemplate.setRetryPolicy(retryPolicy);
        FixedBackOffPolicy backOff = new FixedBackOffPolicy();
        backOff.setBackOffPeriod(0L); // 0 ms pour les tests
        retryTemplate.setBackOffPolicy(backOff);

        // spy pour stubber connectSingleSag(...)
        service = Mockito.spy(new SagConnectionService(config, retryTemplate));

        // 4 endpoints dans la conf
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

        // Peu importe la SAG, on réussit
        doReturn(handle).when(service).connectSingleSag(any());

        SagHandle result = service.connectWithRetry();

        assertThat(result).isSameAs(handle);
        // connectSingleSag appelé une seule fois (première SAG du 1er cycle)
        verify(service, times(1)).connectSingleSag(any());
    }

    // ===== TEST 2 : échec sur tous les endpoints au 1er cycle,
    //                succès sur le début du 2ème cycle =====

    @Test
    void connectWithRetry_should_retry_cycle_when_all_sags_fail_once_then_succeed() throws Exception {
        SagHandle handle = mock(SagHandle.class);

        // 4 premiers appels -> KO (cycle 1)
        // 5ème appel -> OK (première SAG du cycle 2)
        doAnswer(invocation -> {
            int callIndex = invocation.getInvocationCount(); // 1..N
            if (callIndex <= 4) {
                throw new RuntimeException("SAG down");
            }
            return handle;
        }).when(service).connectSingleSag(any());

        SagHandle result = service.connectWithRetry();

        // On obtient bien le handle
        assertThat(result).isSameAs(handle);

        // 4 appels (cycle1 KO) + 1 appel (cycle2 OK) = 5
        verify(service, times(5)).connectSingleSag(any());
    }

    // ===== TEST 3 : échec sur tous les endpoints pour les 3 tentatives =====

    @Test
    void connectWithRetry_should_throw_NoSagAvailableException_after_all_attempts_fail() throws Exception {
        // Tous les appels à connectSingleSag jettent une exception technique
        doThrow(new RuntimeException("SAG down"))
                .when(service).connectSingleSag(any());

        assertThatThrownBy(() -> service.connectWithRetry())
                .isInstanceOf(NoSagAvailableException.class);

        // 4 SAG * 3 tentatives = 12 appels
        verify(service, times(12)).connectSingleSag(any());
    }
}
