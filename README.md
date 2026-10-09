On continue avec RTL-1440, en implémentant les indicateurs ADD, CAN, REG et leur répartition dans MovementBlock.

Je te propose de séparer le calcul des indicateurs de la classification des opérations. Cela permettra de généraliser la logique sans dupliquer les traitements.

1. Règle de classification

Chaque opération doit contribuer une seule fois au rapport :

* 1 externalService → agrégation dans externalServices.
* 0 ou plusieurs externalServices → agrégation dans undefinedExternalService.

Le filtrage métier reste basé sur operation.creationDate.

2. Construire les indicateurs

La classe Indicators contient déjà les champs nécessaires. On peut créer un accumulateur qui regroupe les montants et les nombres d’items.

public class FinancialIndicatorsAccumulator {
    private long netAmountAdd;
    private long grossAmountAdd;
    private int nbItemAdd;
    private long netAmountCan;
    private long grossAmountCan;
    private int nbItemCan;
    private long netAmountReg;
    private long grossAmountReg;
    private int nbItemReg;
    private long promotionAmount;
    public void add(
            OperationActionTypeCode action,
            long netAmount,
            long grossAmount,
            int nbItems,
            long promotion) {
        switch (action) {
            case ADD:
                netAmountAdd += netAmount;
                grossAmountAdd += grossAmount;
                nbItemAdd += nbItems;
                break;
            case CAN:
                netAmountCan += netAmount;
                grossAmountCan += grossAmount;
                nbItemCan += nbItems;
                break;
            case REG:
                netAmountReg += netAmount;
                grossAmountReg += grossAmount;
                nbItemReg += nbItems;
                break;
            default:
                break;
        }
        promotionAmount += promotion;
    }
    public Indicators toIndicators() {
        Indicators result = new Indicators();
        result.setNetAmountAdd(netAmountAdd);
        result.setGrossAmountAdd(grossAmountAdd);
        result.setNbItemAdd(nbItemAdd);
        result.setNetAmountCan(netAmountCan);
        result.setGrossAmountCan(grossAmountCan);
        result.setNbItemCan(nbItemCan);
        result.setNetAmountReg(netAmountReg);
        result.setGrossAmountReg(grossAmountReg);
        result.setNbItemReg(nbItemReg);
        result.setPromotionAmount(promotionAmount);
        return result;
    }
}

Ce code est une structure de calcul. Les valeurs netAmount, grossAmount, nbItems et promotion devront provenir des champs appropriés de Operation et de sa logique financière. Les valeurs de l’énumération OperationActionTypeCode doivent aussi être vérifiées : les noms ADD, CAN, REG correspondent aux indicateurs du rapport, mais pas nécessairement aux constantes réelles de ton projet.

3. Construire MovementBlock

L’objectif est ensuite d’utiliser un accumulateur pour chaque service mono-produit et un autre pour les opérations non attribuables.

Map<Long, FinancialIndicatorsAccumulator> byService =
        new HashMap<>();
FinancialIndicatorsAccumulator undefined =
        new FinancialIndicatorsAccumulator();
for (Operation operation : operations) {
    Set<ExternalService> services =
            operation.getExternalServices();
    FinancialIndicatorsAccumulator accumulator;
    if (services != null && services.size() == 1) {
        ExternalService service =
                services.iterator().next();
        accumulator = byService.computeIfAbsent(
                service.getId(),
                id -> new FinancialIndicatorsAccumulator());
    } else {
        accumulator = undefined;
    }
    // Convertir l'opération en valeurs financières
    // selon les règles métier existantes, puis :
    //
    // accumulator.add(action, net, gross, nbItems, promotion);
}

Il restera à transformer chaque entrée de byService en ExternalServiceAggregate, en utilisant les propriétés de ce modèle, puis à affecter les résultats à MovementBlock.

4. Ce qu’il faut vérifier avant l’intégration

Il y a trois éléments que les captures ne permettent pas encore de déterminer avec certitude : la correspondance des actions avec ADD/CAN/REG, la formule des montants nets et bruts, et la structure complète de ExternalServiceAggregate.

Je préfère reprendre les règles existantes plutôt que d’introduire un calcul financier incorrect.

Envoie-moi maintenant le code de ExternalServiceAggregate.java et la partie de OperationReportMapper qui calcule les montants. Je pourrai te fournir l’implémentation complète du mapping vers MovementBlock, avec les bons types, les bonnes méthodes et sans logique fictive.
