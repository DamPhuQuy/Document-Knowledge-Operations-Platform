package com.platform.app.iam.infrastructure.adapters.secondary.messaging;

import com.platform.app.iam.application.ports.outbound.EventPublisherPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringEventPublisherAdapter implements EventPublisherPort {

  private final ApplicationEventPublisher delegate;

  @Override
  public void publish(Object event) {
    if (event != null) {
      delegate.publishEvent(event);
    }
  }
}
