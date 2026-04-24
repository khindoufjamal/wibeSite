services:
  activemq:
    image: apache/activemq-classic:6.2.0
    container_name: activemq
    environment:
      ACTIVEMQ_USER: admin
      ACTIVEMQ_PASSWORD: admin
      ACTIVEMQ_OPTS: "-Xms512m -Xmx2048m"
    ports:
      - "8161:8161"
      - "61616:61616"
      - "5672:5672"
      - "1883:1883"
    volumes:
      - ./data/activemq:/opt/activemq/data
      - ./activemq/activemq.xml:/opt/activemq/conf/activemq.xml
    restart: unless-stopped
