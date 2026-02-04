
     {% for sag in sagList %}
swift.sagList[{{ loop.index0 }}].hostname={{ sag.hostname }}
swift.sagList[{{ loop.index0 }}].port={{ sag.port }}
swift.sagList[{{ loop.index0 }}].certPath={{ sag.certPath }}
swift.sagList[{{ loop.index0 }}].dn={{ sag.dn }}
{% endfor %}                   
                        
                        
                        
                        "swift.partner=SABR-KAL",
                        "swift.requestSignatureDN=dn-sign",
                        "swift.requestVerifySignatureDN=dn-verify",
                        "swift.key=secret",
                        "swift.verify-signature=true",
                        "swift.attemptNumber=3",
                        "swift.backOff=1000",

                        "swift.sagList[0].hostname=10.0.0.1",
                        "swift.sagList[0].port=58002",
                        "swift.sagList[0].certPath=/tmp/sag1.crt",
                        "swift.sagList[0].dn=cn=SAG1"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    SwiftConfiguration config =
                            context.getBean(SwiftConfiguration.class);

                    assertThat(config.getSagList()).hasSize(1);
                    assertThat(config.getAttemptNumber()).isEqualTo(3);
                });
    }

    // =====================================================
    // ❌ LISTE SAG VIDE
    // =====================================================
    @Test
    void should_fail_when_sag_list_is_empty() {
        contextRunner
                .withPropertyValues(
                        "swift.partner=SABR-KAL",
                        "swift.requestSignatureDN=dn-sign",
                        "swift.requestVerifySignatureDN=dn-verify",
                        "swift.key=secret",
                        "swift.attemptNumber=3",
                        "swift.backOff=1000"
                        // ⚠️ aucune sagList
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .isInstanceOf(BindValidationException.class)
                            .hasMessageContaining("sagList");
                });
    }

    // =====================================================
    // ❌ SAG INCOMPLET (hostname manquant)
    // =====================================================
    @Test
    void should_fail_when_sag_is_invalid() {
        contextRunner
                .withPropertyValues(
                        "swift.partner=SABR-KAL",
                        "swift.requestSignatureDN=dn-sign",
                        "swift.requestVerifySignatureDN=dn-verify",
                        "swift.key=secret",
                        "swift.attemptNumber=3",
                        "swift.backOff=1000",

                        "swift.sagList[0].port=58002",
                        "swift.sagList[0].certPath=/tmp/sag1.crt",
                        "swift.sagList[0].dn=cn=SAG1"
                        // ❌ hostname manquant
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .isInstanceOf(BindValidationException.class)
                            .hasMessageContaining("hostname");
                });
    }

    // =====================================================
    // ❌ attemptNumber invalide
    // =====================================================
    @Test
    void should_fail_when_attempt_number_is_invalid() {
        contextRunner
                .withPropertyValues(
                        "swift.partner=SABR-KAL",
                        "swift.requestSignatureDN=dn-sign",
                        "swift.requestVerifySignatureDN=dn-verify",
                        "swift.key=secret",
                        "swift.attemptNumber=0", // ❌ invalide
                        "swift.backOff=1000",

                        "swift.sagList[0].hostname=10.0.0.1",
                        "swift.sagList[0].port=58002",
                        "swift.sagList[0].certPath=/tmp/sag1.crt",
                        "swift.sagList[0].dn=cn=SAG1"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .isInstanceOf(BindValidationException.class)
                            .hasMessageContaining("attemptNumber");
                });
    }

    // =====================================================
    // CONFIG TEST
    // =====================================================
    @EnableConfigurationProperties(SwiftConfiguration.class)
    static class TestConfig {
    }
}


---

3️⃣ Ce que ce test garantit (important)

✔ Impossible de démarrer si :

sagList absente

sagList vide

SagEndpoint incomplet

retry mal configuré


✔ La validation est testée sans lancer l’application complète

✔ Les erreurs sont lisibles et maintenables


---

4️⃣ Résumé rapide

Cas	Résultat

config valide	✅ OK
sagList vide	❌ KO
SAG incomplet	❌ KO
attemptNumber = 0	❌ KO



---

Si tu veux, au prochain message je peux te proposer un test d’intégration complet (avec @SpringBootTest) qui vérifie que SagConnectionService ne démarre jamais avec une mauvaise config.
