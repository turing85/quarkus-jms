package de.turing85;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.Topic;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class PeriodicSender implements Runnable {
  private final Logger logger;
  private final Duration period;
  private final AtomicInteger counter;
  private final JMSProducer producer;
  private final Topic topic;

  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

  public PeriodicSender(
      Logger logger,

      @SuppressWarnings("CdiInjectionPointsInspection")
      ConnectionFactory connectionFactory,

      @ConfigProperty(name = "send.period")
      Duration period) {
    this.logger = logger;
    this.period = period;
    this.counter = new AtomicInteger();
    JMSContext context = connectionFactory.createContext(JMSContext.AUTO_ACKNOWLEDGE);
    context.setClientID("sender");
    producer = context.createProducer();
    topic = context.createTopic("prices");
  }

  void onStart(@Observes StartupEvent ev) {
    scheduler.scheduleWithFixedDelay(this, 0L, period.toNanos(), TimeUnit.NANOSECONDS);
  }

  void onStop(@Observes ShutdownEvent ev) {
    scheduler.shutdown();
  }

  @Override
  public void run() {
    int valueToSend = counter.incrementAndGet();
    logger.infof("Sending: %d", valueToSend);
    producer.send(topic, Integer.toString(valueToSend));
  }
}
