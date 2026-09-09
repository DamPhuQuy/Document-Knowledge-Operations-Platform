package com.platform.app.iam.infrastructure.adapters.secondary.messaging;

import com.platform.app.iam.application.ports.outbound.EventPublisherPort;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringEventPublisherAdapter implements EventPublisherPort {

  private final ApplicationEventPublisher delegate;

  public SpringEventPublisherAdapter(ApplicationEventPublisher delegate) {
    this.delegate = Objects.requireNonNull(delegate, "ApplicationEventPublisher must not be null");
  }

  @Override
  public void publish(Object event) {
    if (event != null) {
      delegate.publishEvent(event);
    }
  }
}
