
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
