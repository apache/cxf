package org.apache.cxf.tracing.opentelemetry;

import java.util.HashMap;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.semconv.NetworkAttributes;
import jakarta.servlet.http.HttpServletRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OpenTelemetryClientStartInterceptorTest {
  @RegisterExtension
  static final OpenTelemetryExtension otel = OpenTelemetryExtension.create();

  @Mock
  Message message;

  @Mock
  HttpServletRequest request;

  @Mock
  Exchange exchange;

  @Test
  void handleMessageShouldStartTraceSpanAndSetProtocolVersion() {
    when(request.getProtocol()).thenReturn("HTTP/1.1");
    when(message.getContextualProperty("HTTP.REQUEST")).thenReturn(request);
    when(message.get(Message.PROTOCOL_HEADERS)).thenReturn(new HashMap<>());
    when(exchange.isSynchronous()).thenReturn(false);
    when(message.getExchange()).thenReturn(exchange);

    try (MockedStatic<PhaseInterceptorChain> mockedPhaseInterceptorChain = mockStatic(PhaseInterceptorChain.class)) {
      mockedPhaseInterceptorChain.when(PhaseInterceptorChain::getCurrentMessage).thenReturn(message);

      OpenTelemetryClientStartInterceptor interceptor = new OpenTelemetryClientStartInterceptor(otel.getOpenTelemetry(), "instrumentationName");
      interceptor.handleMessage(message);

    }

  }
}
