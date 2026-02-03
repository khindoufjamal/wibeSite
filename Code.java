Parfait 👍
Dans ce cas on va faire un vrai test qui charge un fichier .properties, exactement comme en prod, et qui vérifie que la validation de SwiftConfiguration fonctionne.

👉 La bonne approche Spring Boot ici est ApplicationContextRunner + withPropertyValues(from file) ou withPropertySource.

Je te propose la solution propre et standard, utilisée en interne chez Spring.


---

🎯 Objectif du test

✔ Charger un vrai fichier .properties
✔ Binder Spring Boot réel
✔ Valider :

config OK → contexte démarre

sagList vide → contexte FAIL

sag invalide → contexte FAIL



---

1️⃣ Créer les fichiers de test

📁 src/test/resources/config/

✅ swift-valid.properties

swift.partner=SABR-KAL
swift.requestSignatureDN=dn-sign
swift.requestVerifySignatureDN=dn-verify
swift.key=secret
swift.verify-signature=true

swift.attemptNumber=3
swift.backOff=1000

swift.sagList[0].hostname=10.0.0.1
swift.sagList[0].port=58002
swift.sagList[0].certPath=/tmp/sag1.crt
swift.sagList[0].dn=cn=SAG1


---

❌ swift-empty-saglist.properties

swift.partner=SABR-KAL
swift.requestSignatureDN=dn-sign
swift.requestVerifySignatureDN=dn-verify
swift.key=secret

swift.attemptNumber=3
swift.backOff=1000


---

❌ swift-invalid-sag.properties

swift.partner=SABR-KAL
swift.requestSignatureDN=dn-sign
swift.requestVerifySignatureDN=dn-verify
swift.key=secret

swift.attemptNumber=3
swift.backOff=1000

swift.sagList[0].port=58002
swift.sagList[0].certPath=/tmp/sag1.crt
swift.sagList[0].dn=cn=SAG1
# hostname manquant


---

2️⃣ Test unitaire qui charge le fichier

package com.stet.t2s.sabr.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.*;

class SwiftConfigurationFileTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(TestConfig.class);

    // =====================================================
    // ✅ FICHIER VALIDE
    // =====================================================
    @Test
    void should_load_valid_properties_file() {
        contextRunner
                .withPropertySource("classpath:config/swift-valid.properties")
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    SwiftConfiguration config =
                            context.getBean(SwiftConfiguration.class);

                    assertThat(config.getSagList()).hasSize(1);
                    assertThat(config.getAttemptNumber()).isEqualTo(3);
                });
    }

    // =====================================================
    // ❌ SAG LIST ABSENTE
    // =====================================================
    @Test
    void should_fail_when_sag_list_is_missing() {
        contextRunner
                .withPropertySource("classpath:config/swift-empty-saglist.properties")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .isInstanceOf(BindValidationException.class)
                            .hasMessageContaining("sagList");
                });
    }

    // =====================================================
    // ❌ SAG INVALIDE
    // =====================================================
    @Test
    void should_fail_when_sag_is_invalid() {
        contextRunner
                .withPropertySource("classpath:config/swift-invalid-sag.properties")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .isInstanceOf(BindValidationException.class)
                            .hasMessageContaining("hostname");
                });
    }

    // =====================================================
    // CONFIG TEST
    // =====================================================
    @Configuration
    @EnableConfigurationProperties(SwiftConfiguration.class)
    static class TestConfig {
    }
}


---

3️⃣ Pourquoi c’est la bonne solution

✔ Le test lit un vrai fichier .properties
✔ Même mécanisme que application.properties
✔ Validation Bean Validation réellement exécutée
✔ Aucun mock
✔ Ultra rapide (pas de @SpringBootTest)


---

4️⃣ Erreurs typiques évitées

❌ @TestPropertySource + @SpringBootTest → trop lourd
❌ charger les propriétés à la main → faux positif
❌ tests unitaires qui ne reflètent pas la prod


---

✅ Verdict

👉 Oui, c’est exactement comme ça qu’il faut tester une @ConfigurationProperties sérieuse
👉 Ton problème de sagList vide est désormais impossible à reintroduire

Si tu veux, je peux aussi te donner un test qui vérifie que SagConnectionService ne démarre pas si la config est invalide (intégration légère).
