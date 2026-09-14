Oui, là on a l’information qui manquait. Les champs existent déjà dans ResellerEventLogPageItem :

private String operationType;
private String operationActionType;
private String product;
private String limitType;
private String idLimit;

Donc pour tes 4 colonnes, le mapping est clair :

Colonne CSV	Propriété Java	Getter attendu
OPERATION_TYPE	operationType	getOperationType()
OPERATION_ACTION_TYPE	operationActionType	getOperationActionType()
PRODUCT	product	getProduct()
TYPE_LIMIT	limitType	getLimitType()

1. EventLogExportConstants.java

Ajoute :

public static final String COLUMN_OPERATION_TYPE = "OPERATION_TYPE";
public static final String COLUMN_OPERATION_ACTION_TYPE = "OPERATION_ACTION_TYPE";
public static final String COLUMN_PRODUCT = "PRODUCT";
public static final String COLUMN_LIMIT_TYPE = "TYPE_LIMIT";

Je te conseille COLUMN_LIMIT_TYPE plutôt que COLUMN_TYPE_LIMIT, car le modèle s’appelle limitType.

2. ResellerEventLogExportColumnRegistry.java

Ajoute les clés :

private static final String KEY_OPERATION_TYPE = "operationType";
private static final String KEY_OPERATION_ACTION_TYPE = "operationActionType";
private static final String KEY_PRODUCT = "product";
private static final String KEY_LIMIT_TYPE = "limitType";

Puis dans ton registerEntityColumns(...), après les colonnes existantes :

map.put(KEY_OPERATION_TYPE,
        column(COLUMN_OPERATION_TYPE,
                item -> defaultString(item.getOperationType())));
map.put(KEY_OPERATION_ACTION_TYPE,
        column(COLUMN_OPERATION_ACTION_TYPE,
                item -> defaultString(item.getOperationActionType())));
map.put(KEY_PRODUCT,
        column(COLUMN_PRODUCT,
                item -> defaultString(item.getProduct())));
map.put(KEY_LIMIT_TYPE,
        column(COLUMN_LIMIT_TYPE,
                item -> defaultString(item.getLimitType())));

Donc ta méthode devient en substance :

@Override
protected void registerEntityColumns(
        final SequencedMap<String, ExportColumn<ResellerEventLogPageItem>> map) {
    map.put(KEY_RESELLER_EXTERNAL_ID,
            column(COLUMN_RESELLER_EXTERNAL_ID,
                    item -> defaultString(item.getResellerExternalId())));
    map.put(KEY_RESELLER_FIRST_NAME,
            column(COLUMN_RESELLER_FIRST_NAME,
                    item -> defaultString(item.getResellerFirstName())));
    map.put(KEY_RESELLER_LAST_NAME,
            column(COLUMN_RESELLER_LAST_NAME,
                    item -> defaultString(item.getResellerLastName())));
    map.put(KEY_OFFER,
            column(COLUMN_OFFER,
                    item -> defaultString(item.getOffer())));
    map.put(KEY_DAILY_OR_WEEKLY,
            column(COLUMN_DAILY_OR_WEEKLY,
                    item -> defaultString(item.getDailyOrWeekly())));
    map.put(KEY_OPERATION_TYPE,
            column(COLUMN_OPERATION_TYPE,
                    item -> defaultString(item.getOperationType())));
    map.put(KEY_OPERATION_ACTION_TYPE,
            column(COLUMN_OPERATION_ACTION_TYPE,
                    item -> defaultString(item.getOperationActionType())));
    map.put(KEY_PRODUCT,
            column(COLUMN_PRODUCT,
                    item -> defaultString(item.getProduct())));
    map.put(KEY_LIMIT_TYPE,
            column(COLUMN_LIMIT_TYPE,
                    item -> defaultString(item.getLimitType())));
}

3. Côté front : getExportColumns()

Il faudra envoyer les clés, donc :

private getExportColumns(): string[] {
    return [
        // colonnes déjà exportées...
        'operationType',
        'operationActionType',
        'product',
        'limitType'
    ];
}

Pas :

'OPERATION_TYPE'

car OPERATION_TYPE est le header CSV, tandis que operationType est la clé utilisée par le registry.

Il reste cependant un point important avant de considérer le ticket terminé : vérifier que les quatre propriétés sont effectivement alimentées lors du mapping ResellerEventLog -> ResellerEventLogPageItem. Le fait qu’elles existent dans PageItem ne garantit pas encore qu’elles contiennent une valeur.

👉 Montre-moi maintenant la partie de ResellerEventLogPageItem avec les getters de operationType jusqu’à idLimit, puis surtout cherche les usages de setOperationType (Option+F7 / Find Usages). C’est cette dernière vérification qui nous dira si le backend est déjà alimenté ou s’il faut également modifier le mapper.
