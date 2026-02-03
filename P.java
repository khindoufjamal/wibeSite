Parfait 👍
On va faire un vrai test unitaire de configuration Spring, qui vérifie que :

✅ la configuration valide démarre

❌ une liste SAG vide fait échouer le binding

❌ un SAG incomplet fait échouer le binding

❌ attemptNumber <= 0 est refusé


👉 On utilise les outils Spring Boot dédiés aux @ConfigurationProperties (c’est propre, rapide, et sans contexte applicatif complet).


---

🧪 Test unitaire de SwiftConfiguration

1️⃣ Dépendance requise (normalement déjà présente)

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-test</artifactId>
    <scope>test</scope>
</dependency>


---

2️⃣ Classe de test dédiée à la configuration

package com.stet.t2s.sabr.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.*;

class SwiftConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(TestConfig.class);

    // =====================================================
    // ✅ CONFIGURATION VALIDE
    // =====================================================
    @Test
    void should_load_configuration_when_valid() {
        contextRunner
                .withPropertyValues(
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
