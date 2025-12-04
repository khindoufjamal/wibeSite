@Data
public class SagEndpoint {
    private String hostname;
    private int port;
    private String dn;
    private String certPath;
    private long timeout = 30L;
}

@Data
@Configuration
@ConfigurationProperties(prefix = "swift")
public class SwiftConfiguration {
    private List<SagEndpoint> sagList; // au lieu d’un seul endpoint
}


public Tuple2<String, String> call(String request) {

    for (SagEndpoint sag : config.getSagList()) {

        log.info("Trying SAG: {}:{}", sag.getHostname(), sag.getPort());

        try {
            HandleParameters params = getHandleParameter(sag);
            SagHandle handle = connect(params);

            log.info("Connected to SAG {}", sag.getHostname());

            // === Build message
            Message msg = buildSwiftMessage(request);

            // === Compute LMAC
            LMAC lmac = getLmac(msg).get();

            // === Send request
            Message response = getMessage(handle, msg).get();

            // === Verify signature
            verifyLmac(lmac, response);

            disconnect(handle);

            return Tuple.of(msg.getLetter(), response.getLetter());
        }
        catch (Exception e) {
            log.error("SAG {} is unavailable: {}", sag.getHostname(), e.getMessage());
            // on essaye la SAG suivante
        }
    }

    // Aucune SAG accessible → fallback file QD
    log.error("All SAGs are down → pushing request to QD");
    pushToQD(request);

    throw new IllegalStateException("No SAG available");
}


private HandleParameters getHandleParameter(SagEndpoint sag) {
    X509Certificate cert = loadCertificate(sag.getCertPath());
    return new HandleParameters(
            sag.getHostname(),
            sag.getPort(),
            cert,
            sag.getDn()
    );
}


sagHandle.connect(config.getTimeout() > 0 ? config.getTimeout() : 30L, TimeUnit.SECONDS);







@ExtendWith(MockitoExtension.class)
class SwiftServiceTest {

    @Mock
    private SwiftConfiguration config;

    private TestSwiftService service;  // sous-classe de test

    @BeforeEach
    void setUp() {
        service = new TestSwiftService(config);
    }

    // Sous-classe pour pouvoir stubber le comportement interne
    static class TestSwiftService extends SwiftService {

        // flags & stubs
        List<SagEndpoint> sagList;
        Map<SagEndpoint, RuntimeException> connectError = new HashMap<>();
        Map<SagEndpoint, RuntimeException> getMessageError = new HashMap<>();
        Map<SagEndpoint, SagHandle> handles = new HashMap<>();
        List<SagHandle> disconnected = new ArrayList<>();
        boolean pushToQdCalled = false;

        TestSwiftService(SwiftConfiguration config) {
            super(config);
        }

        @Override
        protected SagHandle connect(HandleParameters params) throws Exception {
            SagEndpoint sag = findSagFromParams(params);
            if (connectError.containsKey(sag)) {
                throw connectError.get(sag);
            }
            SagHandle handle = Mockito.mock(SagHandle.class);
            handles.put(sag, handle);
            return handle;
        }

        @Override
        protected void disconnect(SagHandle handle) {
            disconnected.add(handle);
        }

        @Override
        protected LMAC getLmac(Message message) {
            // simplification : retourner un mock
            return Mockito.mock(LMAC.class);
        }

        @Override
        protected Message getMessage(SagHandle handle, Message request) {
            SagEndpoint sag = findSagFromHandle(handle);
            if (getMessageError.containsKey(sag)) {
                throw getMessageError.get(sag);
            }
            Message response = new Message();
            response.setLetter("<response/>");
            return response;
        }

        @Override
        protected void verifyLmac(LMAC lmac, Message response) {
            // rien, on suppose valid
        }

        @Override
        protected void pushToQD(String request) {
            this.pushToQdCalled = true;
        }

        // Helpers pour mapper HandleParameters/SagHandle vers SagEndpoint
        private SagEndpoint findSagFromParams(HandleParameters params) {
            return sagList.stream()
                    .filter(s -> s.getHostname().equals(params.getHostname())
                            && s.getPort() == params.getPort())
                    .findFirst()
                    .orElseThrow();
        }

        private SagEndpoint findSagFromHandle(SagHandle handle) {
            // dans un vrai test on stockerait le mapping handle → SAG
            // pour simplifier ici on retourne le premier
            return sagList.get(0);
        }
    }

    // ---------- TESTS -----------

    @Test
    void should_use_first_sag_when_ok() {
        SagEndpoint sag1 = new SagEndpoint();
        sag1.setHostname("sag1");
        sag1.setPort(48002);

        when(config.getSagList()).thenReturn(List.of(sag1));
        service.sagList = List.of(sag1);

        Tuple2<String, String> result = service.call("<request/>");

        // call OK → pas de mise en QD
        assertThat(service.pushToQdCalled).isFalse();
        // une seule connexion
        assertThat(service.handles).hasSize(1);
        assertThat(service.disconnected).hasSize(1);
        // la réponse contient bien ce qu'on attend (simplifié)
        assertThat(result._2).contains("<response");
    }

    @Test
    void should_fallback_on_second_sag_when_first_connect_fails() {
        SagEndpoint sag1 = new SagEndpoint();
        sag1.setHostname("sag1");
        sag1.setPort(48002);

        SagEndpoint sag2 = new SagEndpoint();
        sag2.setHostname("sag2");
        sag2.setPort(48003);

        when(config.getSagList()).thenReturn(List.of(sag1, sag2));
        service.sagList = List.of(sag1, sag2);

        // SAG1 KO à la connexion
        service.connectError.put(sag1, new RuntimeException("SAG1 down"));

        Tuple2<String, String> result = service.call("<request/>");

        // On a bien tenté les 2 SAG
        assertThat(service.handles.keySet()).containsExactlyInAnyOrder(sag2); // sag1 n'a pas de handle
        // SAG2 a permis de répondre
        assertThat(result._2).contains("<response");
        assertThat(service.pushToQdCalled).isFalse();
    }

    @Test
    void should_push_to_qd_when_all_sags_fail() {
        SagEndpoint sag1 = new SagEndpoint();
        sag1.setHostname("sag1");
        sag1.setPort(48002);

        SagEndpoint sag2 = new SagEndpoint();
        sag2.setHostname("sag2");
        sag2.setPort(48003);

        when(config.getSagList()).thenReturn(List.of(sag1, sag2));
        service.sagList = List.of(sag1, sag2);

        service.connectError.put(sag1, new RuntimeException("SAG1 down"));
        service.connectError.put(sag2, new RuntimeException("SAG2 down"));

        assertThatThrownBy(() -> service.call("<request/>"))
                .isInstanceOf(IllegalStateException.class);

        assertThat(service.pushToQdCalled).isTrue();
    }

    @Test
    void should_disconnect_handle_when_error_after_connect() {
        SagEndpoint sag1 = new SagEndpoint();
        sag1.setHostname("sag1");
        sag1.setPort(48002);

        when(config.getSagList()).thenReturn(List.of(sag1));
        service.sagList = List.of(sag1);

        // forcer une erreur au moment de getMessage
        service.getMessageError.put(sag1, new RuntimeException("Transport error"));

        assertThatThrownBy(() -> service.call("<request/>"))
                .isInstanceOf(IllegalStateException.class);

        // Handle créé puis bien déconnecté
        assertThat(service.handles).hasSize(1);
        assertThat(service.disconnected).hasSize(1);
    }
          }




@Data
@Configuration
@ConfigurationProperties(value = "swift")
public class SwiftConfiguration {

    // === configuration actuelle (single SAG) ===
    private String hostname;
    private Integer port;
    private String partner;
    private String requestSignatureDN;
    private String requestVerifySignatureDN;
    private String key;
    private String certPath;
    private String dn;
    private long timeout;
    private boolean verifySignature;
    private Boolean flattenVerifyRequest;
    private Boolean flattenSignRequest;

    // === nouvelle configuration multi-SAG ===
    private List<SagEndpoint> sagList;

    @Data
    public static class SagEndpoint {
        private String hostname;
        private Integer port;
        private String dn;
        private String certPath;
        private long timeout;
    }
}


_______


    @Slf4j
@Service
@RequiredArgsConstructor
public class SagConnectionService {

    private final SwiftConfiguration config;
    private final RetryTemplate sagRetryTemplate;

    /**
     * Shuffle UNE FOIS la liste de 4 SAG,
     * puis Spring Retry exécute la même séquence jusqu'à 3 fois.
     */
    public SagHandle connectWithRetry() {

        List<SwiftConfiguration.SagEndpoint> orderedEndpoints =
                new ArrayList<>(config.getSagList());

        if (orderedEndpoints.isEmpty()) {
            throw new IllegalStateException("No SAG configured");
        }

        // Shuffle UNE fois
        Collections.shuffle(orderedEndpoints);
        log.info("SAG order for this message: {}", orderedEndpoints.stream()
                .map(SwiftConfiguration.SagEndpoint::getHostname)
                .toList());

        // Retry sur la même liste ordonnée
        return sagRetryTemplate.execute(context -> {
            int attempt = context.getRetryCount() + 1;
            log.info("SAG connection attempt {}...", attempt);

            for (SwiftConfiguration.SagEndpoint sag : orderedEndpoints) {
                try {
                    return connectSingleSag(sag);
                } catch (Exception e) {
                    log.warn("[KO] SAG {}:{} - {} (EVIT/ONC Warning)",
                            sag.getHostname(), sag.getPort(), e.getMessage());
                }
            }

            // Aucune SAG connectée pendant CETTE tentative ⇒ on déclenche un retry
            log.warn("No SAG reachable in attempt {}", attempt);
            throw new NoSagAvailableException("No SAG reachable in attempt " + attempt);
        });
    }

    private SagHandle connectSingleSag(SwiftConfiguration.SagEndpoint sag) throws Exception {
        HandleParameters params = buildHandleParameters(sag);
        SagHandle handle = new SagHandle(params);

        long timeout = sag.getTimeout() > 0 ? sag.getTimeout() : config.getTimeout();
        handle.connect(timeout, TimeUnit.SECONDS);

        log.info("[OK] Connected to SAG {}:{}", sag.getHostname(), sag.getPort());
        return handle;
    }

    public void disconnectQuietly(SagHandle handle) {
        if (handle == null) return;
        try {
            if (handle.isConnected()) handle.disconnect();
        } catch (Exception e) {
            log.error("Error while disconnecting SAG handle", e);
        }
    }

    private HandleParameters buildHandleParameters(SwiftConfiguration.SagEndpoint sag) {
        X509Certificate cert = loadCertificate(sag.getCertPath());
        return new HandleParameters(
                sag.getHostname(),
                sag.getPort(),
                cert,
                sag.getDn()
        );
    }

    private X509Certificate loadCertificate(String path) {
        try (InputStream is = new FileInputStream(path)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(is);
        } catch (IOException | CertificateException e) {
            log.error("[Error] Failed to load certificate {}", path, e);
            return null;
        }
    }
}




@Slf4j
@Service
@RequiredArgsConstructor
public class SwiftService {

    private final SwiftConfiguration config;
    private final SagConnectionService sagConnectionService;

    public Tuple2<String, String> call(String request) {

        SagHandle handle;
        try {
            handle = sagConnectionService.connectWithRetry();
        } catch (NoSagAvailableException e) {
            log.error("All SAG attempts failed, pushing message to QD (EVIT/ONC Sévère)", e);
            pushToQD(request);
            throw new IllegalStateException("No SAG available after retries", e);
        }

        try {
            // à partir d’ici: AUCUN retry SAG
            Message msg = buildMessage(request);
            LMAC lmac = getLmac(msg);
            Message response = getMessage(handle, msg);
            verifyLmac(lmac, response);
            return Tuple.of(msg.getLetter(), response.getLetter());
        } finally {
            sagConnectionService.disconnectQuietly(handle);
        }
    }
}

