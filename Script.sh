Très bonne question 👍
Réponse courte : non, tel quel ce fichier n’est PAS valide pour un .properties, il est écrit en YAML, pas en .properties.

👉 Tu es à 90% bon fonctionnellement, mais la syntaxe doit être corrigée.


---

🚨 Problème principal

Dans un fichier .properties :

❌ PAS de :

❌ PAS de listes avec -

❌ PAS d’indentation


Or tu as ceci :

swift.sagList:
  - hostname: 10.30.157.7
    port: 58002
    certPath: /app/sabr/cur/sag/swift_SAG1.crt
    dn: cn=CX5XXSAG01

➡️ Ça, c’est du YAML, donc Spring ne le lira pas dans un .properties.


---

✅ VERSION CORRECTE EN .properties

Voici la version valide et recommandée pour ton fichier .properties, compatible avec @ConfigurationProperties.

🔹 Partie simple (OK chez toi)

# Swift
swift.hostname=TOBEDEFINED
swift.port=58002
swift.partner=SABR-KAL

# DN signature
swift.requestSignatureDN=cn=corekal,ou=sabr,ou=sttsfrp1xxx,o=st
swift.requestVerifySignatureDN=cn=corekal,ou=sabr,ou=sttsfrp1xxx,o=st
swift.key=ABCD1234abcd1234ABCD1234abcd1234
swift.certPath=/app/sabr/sag/swift.crt
swift.dn=cn=CX5XXSAG01
swift.timeout=30

swift.verify-signature=true


---

🔹 Liste des SAG (syntaxe correcte en properties)

👉 Index obligatoire ([0], [1], …)

# Connection to SAG configuration
swift.sagList[0].hostname=10.30.157.7
swift.sagList[0].port=58002
swift.sagList[0].certPath=/app/sabr/cur/sag/swift_SAG1.crt
swift.sagList[0].dn=cn=CX5XXSAG01

swift.sagList[1].hostname=10.30.157.8
swift.sagList[1].port=58002
swift.sagList[1].certPath=/app/sabr/cur/sag/swift_SAG2.crt
swift.sagList[1].dn=cn=CX5XXSAG02

➡️ Ça correspond exactement à :

private List<SagEndpoint> sagList;


---

🔹 Retry (parfaitement OK)

swift.attemptNumber=3
swift.backoff=10000

👉 Tu pourras les injecter dans :

@Value("${swift.attemptNumber}")
private int attemptNumber;

@Value("${swift.backoff}")
private long backoff;

ou directement dans SwiftConfiguration.


---

✅ Alternative (si tu veux garder ton format actuel)

Si tu veux garder la syntaxe avec : et -, alors :

👉 renomme le fichier en

application.yml

et là TON FICHIER EST DÉJÀ BON (quasi parfait).


---

🧪 Vérification rapide (à faire)

Ajoute temporairement :

@PostConstruct
public void checkConfig() {
    log.info("SAG list loaded: {}", sagList);
}

Si tu vois bien les 2 SAG loggées → config OK.


---

✅ Verdict final

Option	Action	OK

.properties	convertir la liste avec [0], [1]	✅
.yml	renommer le fichier	✅
Tel quel en .properties	❌	❌


Si tu veux, envoie-moi la classe SwiftConfiguration, je te valide que les noms des propriétés matchent à 100%.
