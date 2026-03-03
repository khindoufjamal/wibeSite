Dans ton flow, IPEU / IPBE c’est justement le cas “spécial” : tu transformes le message entrant en IPMQ (XML) avant de construire MessageAR.

Ce que ça implique

Le message initial (JSON) d’IPEU/IPBE n’existe plus après createIPMQMsg(...
MessageARFactory.build(...) est appelé après la transformation, donc :

messageAR.content = le XML IPMQ (celui mis dans ActiveMQTextMessage#setText(xml))

fillMsgAr(...) récupère les properties JMS (context, requestType, bic, typeFlux, etc.) qui ont été posées dans createIPMQMessage(eventIPAstI).



➡️ Donc oui, pour IPEU/IPBE, MessageAR contient déjà tout ce qu’il faut pour reconstruire le message JMS “IPMQ” (pas le JSON original).


---

Méthode pour reconstruire le JMS depuis MessageAR (cas IPEU / IPBE)

Version Spring JMS (safe) : MessageCreator

import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;
import org.springframework.jms.core.MessageCreator;

public final class MessageARToJmsMapper {

  private MessageARToJmsMapper() {}

  public static MessageCreator toIpMqJmsMessage(MessageAR ar) {
    return (Session session) -> {
      TextMessage msg = session.createTextMessage(ar.getContent()); // XML IPMQ

      // Repose les properties nécessaires au downstream
      setStringProp(msg, Keys.JMS_ATTR_CONTEXT, ar.getTransferId());
      setStringProp(msg, Keys.JMS_ATTR_FILE_COMPONENT, ar.getFileLocator());
      setStringProp(msg, Keys.JMS_ATTR_REQUEST_TYPE, ar.getRequestType());
      setStringProp(msg, Keys.JMS_ATTR_BIC_SYSTEM_EXTERN, ar.getBicSystemExterne());
      setStringProp(msg, Keys.JMS_ATTR_TYPE_FLUX, ar.getTypeFlux());
      setStringProp(msg, Keys.JMS_ATTR_TYPE_TRANSFERT, ar.getTypeTransfert());
      setStringProp(msg, Keys.JMS_ATTR_MODE_TRANSFERT, ar.getModeTransfert());

      return msg;
    };
  }

  private static void setStringProp(Message msg, String key, String value) throws Exception {
    if (value != null && !value.isBlank()) {
      msg.setStringProperty(key, value);
    }
  }
}

Utilisation :

if (List.of(IPEU.getLabel(), IPBE.getLabel()).contains(codeSe)) {
  jmsTemplate.send(destinationQueue, MessageARToJmsMapper.toIpMqJmsMessage(messageAR));
}


---

Point à retenir

✅ Reconstruction OK pour IPEU/IPBE au format IPMQ (XML + props).

❌ Impossible de reconstruire le JSON initial (tu l’as jeté lors de createIPMQMsg), sauf si tu le sauvegardes explicitement (ex: messageAR.setRawIncoming(...) avant transformation).


Si tu me dis sur quelle queue tu veux le renvoyer (même queue d’entrée ? queue “core” ? DLQ/retry ?), je te donne la version “retry propre” (incluant JMSXDeliveryCount, headers de traçabilité, et stratégie de correlationId).
