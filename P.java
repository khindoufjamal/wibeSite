Parfait 👍
Là on est exactement au bon endroit pour bloquer le problème à la source : au chargement de la configuration, avant même que le service démarre.

Je te propose une solution propre, Spring-standard, robuste, avec Bean Validation + un validateur métier pour la liste SAG.


---

🎯 Objectif

👉 Empêcher démarrage de l’application si :

sagList est null

sagList est vide

un SagEndpoint est incomplet (hostname, port, certPath, dn)

attemptNumber <= 0

backOff < 0



---

1️⃣ Activer la validation sur @ConfigurationProperties

✅ Modifier SwiftConfiguration

package com.stet.t2s.sabr.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "swift")
@Validated
public class SwiftConfiguration {

    // === Core configuration ===

    @NotBlank(message = "swift.partner must not be blank")
    private String partner;

    @NotBlank(message = "swift.requestSignatureDN must not be blank")
    private String requestSignatureDN;

    @NotBlank(message = "swift.requestVerifySignatureDN must not be blank")
    private String requestVerifySignatureDN;

    @NotBlank(message = "swift.key must not be blank")
    private String key;

    private boolean verifySignature;
    private boolean flattenVerifyRequest;
    private boolean flattenSignRequest;

    // === Retry / SAG configuration ===

    @NotNull(message = "swift.sagList must be defined")
    @Size(min = 1, message = "swift.sagList must contain at least one SAG")
    @Valid
    private List<SagEndpoint> sagList;

    @Min(value = 1, message = "swift.attemptNumber must be >= 1")
    private int attemptNumber;

    @Min(value = 0, message = "swift.backoff must be >= 0")
    private long backOff;
}

📌 Ce que ça garantit déjà :

sagList=null ❌ → KO au démarrage

sagList=[] ❌ → KO au démarrage

tentative négative ❌ → KO au démarrage



---

2️⃣ Valider chaque SagEndpoint

✅ Modifier SagEndpoint

package com.stet.t2s.sabr.config;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SagEndpoint {

    @NotBlank(message = "sag.hostname must not be blank")
    private String hostname;

    @Min(value = 1, message = "sag.port must be > 0")
    private int port;

    @NotBlank(message = "sag.certPath must not be blank")
    private String certPath;

    @NotBlank(message = "sag.dn must not be blank")
    private String dn;

    @Min(value = 1, message = "sag.timeout must be > 0")
    private long timeout = 30L;
}

📌 Résultat :
Un SAG mal configuré → l’appli ne démarre pas, message clair.


---

3️⃣ (Option RECOMMANDÉE) Validateur métier : au moins N SAG

Si ton métier impose au moins 2 ou 4 SAG, on ajoute un validateur custom.

🔹 Annotation custom

@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MinSagValidator.class)
@Documented
public @interface MinSag {

    int value();

    String message() default "Not enough SAG configured";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

🔹 Implémentation

public class MinSagValidator implements ConstraintValidator<MinSag, List<SagEndpoint>> {

    private int min;

    @Override
    public void initialize(MinSag constraintAnnotation) {
        this.min = constraintAnnotation.value();
    }

    @Override
    public boolean isValid(List<SagEndpoint> value, ConstraintValidatorContext context) {
        return value != null && value.size() >= min;
    }
}

🔹 Utilisation dans SwiftConfiguration

@MinSag(value = 2, message = "At least 2 SAG endpoints must be configured")
@Valid
private List<SagEndpoint> sagList;


---

4️⃣ Résultat final (comportement attendu)

❌ Exemple KO au démarrage

swift.sagList=

⛔ Erreur claire :

Binding validation errors on swift:
 - swift.sagList must contain at least one SAG


---

❌ SAG incomplet

swift.sagList[0].hostname=

⛔ Erreur :

sag.hostname must not be blank


---

✅ Config valide

➡️ L’application démarre
➡️ SagConnectionService n’a plus besoin de vérifier isEmpty()
➡️ Le bug ne peut plus revenir


---

5️⃣ BONUS (bonne pratique)

Dans SagConnectionService, tu peux maintenant supprimer ce code défensif :

if (orderedEndpoints.isEmpty()) {
    throw new IllegalStateException("No SAG configured");
}

👉 La config est garantie valide par Spring.


---

✅ Verdict

✔ Sécurité au démarrage
✔ Erreurs explicites
✔ Bug impossible à reproduire
✔ Conforme Spring Boot / Clean Architecture

Si tu veux, je peux aussi te fournir un test unitaire de validation de SwiftConfiguration (avec @ConfigurationPropertiesTest).
