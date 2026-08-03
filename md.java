
[03/08 16:42] Jam Mar: @PostConstruct
private void init() throws Exception {

    ring = new Ring<>(agiAmqConfiguration.getConfigurations());

    for (ActiveMQConfiguration configuration : ring.getElements()) {

        GenericObjectPoolConfig<Tuple2<ConnectionFactory, ActiveMQConfiguration>> config =
                new GenericObjectPoolConfig<>();

        config.setMaxTotal(configuration.getSize());
        config.setMinIdle(configuration.getSize());
        config.setLifo(false);
        config.setTestOnBorrow(true);

        Ring<ActiveMQConfiguration> singleRing = new Ring<>(
                Collections.singletonList(configuration));

        GenericObjectPool<Tuple2<ConnectionFactory, ActiveMQConfiguration>> pool =
                new GenericObjectPool<>(
                        new ObjectFactory(singleRing),
                        config);

        pool.preparePool();

        pools.put(configuration, pool);
    }

    final ConnectionFactory connectionFactory =
            jmsTemplateService.amqConnectionFactory();

    senderSabr = jmsTemplateService.build(connectionFactory);
}
[03/08 16:42] Jam Mar: ActiveMQConfiguration configuration = ...;

GenericObjectPool<Tuple2<ConnectionFactory, ActiveMQConfiguration>> pool =
        pools.get(configuration);

Tuple2<ConnectionFactory, ActiveMQConfiguration> connection =
        pool.borrowObject();

try {
    // traitement
} finally {
    pool.returnObject(connection);
}



€€€€€€€€€€€€€€€€€--------------€€€€€€€€€€







    vous avez besoin d’un mécanisme de réutilisation des connexions, mais pas forcément d’un GenericObjectPool développé par vous-mêmes.

Dans votre cas, AtomikosConnectionFactoryBean possède déjà son propre pool interne. Donc la solution la plus propre est :

1 ActiveMQ configuré
→ 1 AtomikosConnectionFactoryBean partagé
→ 1 JmsTemplate partagé

et non :

1 ActiveMQ
→ 1 GenericObjectPool
→ plusieurs AtomikosConnectionFactoryBean
→ plusieurs pools internes

Ce qui est indispensable

Il ne faut surtout plus faire ceci à chaque transaction :

ConnectionFactory connectionFactory =
        jmsTemplateService.amqConnectionFactory(activeMqServer);
JmsTemplate sender =
        jmsTemplateService.build(connectionFactory);

Cela recrée une factory Atomikos et donc un nouveau pool à chaque message.

Ce que je recommande

Créer au démarrage une factory et un JmsTemplate par ActiveMQ :

private final Map<String, JmsTemplate> sendersByActiveMq =
        new ConcurrentHashMap<>();
private final Map<String, AtomikosConnectionFactoryBean> factoriesByActiveMq =
        new ConcurrentHashMap<>();
@PostConstruct
void init() {
    for (ActiveMQConfiguration config : configurations) {
        String key = activeMqKey(config);
        AtomikosConnectionFactoryBean factory =
                (AtomikosConnectionFactoryBean)
                        jmsTemplateService.amqConnectionFactory(config);
        JmsTemplate sender = jmsTemplateService.build(factory);
        factoriesByActiveMq.put(key, factory);
        sendersByActiveMq.put(key, sender);
    }
}

Puis :

public void processMsgToAgis(
        Message message,
        ActiveMQConfiguration activeMqServer) {
    String key = activeMqKey(activeMqServer);
    JmsTemplate sender = sendersByActiveMq.get(key);
    if (sender == null) {
        throw new IllegalStateException(
                "Aucun sender configuré pour " + key
        );
    }
    send(sender, activeMqServer.getQueueIn(), message);
}

Une instance de JmsTemplate peut être partagée entre plusieurs threads une fois configurée. Spring recommande également que la ConnectionFactory utilisée fournisse des connexions mutualisées ou partagées. 

Pourquoi éviter le pool Commons ici

Si vous configurez :

GenericObjectPool maxTotal = 10
Atomikos poolSize = 10

vous pouvez potentiellement avoir :

10 factories Atomikos
× 10 connexions chacune
= 100 connexions par ActiveMQ

Le pool Commons ajoute donc une seconde couche sans réel bénéfice, sauf besoin métier très particulier.

Conclusion

* Oui, vous avez besoin de pooling ou de caching pour éviter de recréer les connexions.
* Non, vous n’avez probablement pas besoin de remettre le GenericObjectPool.
* Votre AtomikosConnectionFactoryBean constitue déjà le pool.
* Le correctif optimal est de créer une factory Atomikos et un JmsTemplate par ActiveMQ au démarrage, puis de les réutiliser.

C’est à la fois plus simple que l’ancien code et plus sûr que l’implémentation actuelle.
