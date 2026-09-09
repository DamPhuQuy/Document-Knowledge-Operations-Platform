package com.platform.app.iam.application.ports.outbound;

public interface EventPublisherPort {
  void publish(Object event);
}
