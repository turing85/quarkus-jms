package de.turing85;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.jms.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ApplicationScoped
public class JmsReceiveRoute implements Runnable {
  private final Logger logger;
  private final ConnectionFactory connectionFactory;
  private final String clientId;
  private final String subscriptionName;

  private final ExecutorService scheduler = Executors.newSingleThreadExecutor();

  public JmsReceiveRoute(
      Logger logger,

      @SuppressWarnings("CdiInjectionPointsInspection")
      ConnectionFactory connectionFactory,

      @ConfigProperty(name = "app.client-id") String clientId,
      @ConfigProperty(name = "app.subscription-name") String subscriptionName) {
    this.logger = logger;
    this.connectionFactory = connectionFactory;
    this.clientId = clientId;
    this.subscriptionName = subscriptionName;
  }

  void onStart(@Observes StartupEvent ev) {
    scheduler.submit(this);
  }

  void onStop(@Observes ShutdownEvent ev) {
    scheduler.shutdown();
  }

  @Override
  public void run() {
    try (JMSContext context = connectionFactory.createContext(JMSContext.AUTO_ACKNOWLEDGE)) {
      context.setClientID(clientId);
      try (JMSConsumer consumer = context.createSharedDurableConsumer(
          context.createTopic("prices"),
          subscriptionName)) {
        while (true) {
          Message message = consumer.receive();
          if (message == null) return;
          logger.infof("Received: %s", message.getBody(String.class));
        }
      }
    } catch (JMSException e) {
      throw new RuntimeException(e);
    }
  }
}
