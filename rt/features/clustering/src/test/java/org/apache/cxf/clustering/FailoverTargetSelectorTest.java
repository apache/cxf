/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.clustering;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import org.apache.cxf.binding.Binding;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Retryable;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.MessageObserver;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * Tests for {@link FailoverTargetSelector}.
 * See CXF-9204.
 */
public class FailoverTargetSelectorTest {

    /**
     * Verifies that when a failover retry throws an exception, the original
     * exception is properly restored on the outbound message. Previously, the
     * catch block in performFailover only conditionally restored the exception,
     * but the exception had already been unconditionally cleared before the retry.
     * This caused the fault observer in ClientImpl to enter the "handle success"
     * path with a null inbound message, resulting in NPE.
     */
    @Test
    public void testExceptionRestoredOnFailedRetry() {
        // Set up a stub endpoint
        EndpointInfo ei = new EndpointInfo(new ServiceInfo(), "http://test");
        ei.setAddress("http://localhost:9999/test");
        Endpoint endpoint = new StubEndpoint(ei);

        // Set up the exchange
        Exchange exchange = new ExchangeImpl();
        exchange.put(Endpoint.class, endpoint);
        exchange.put(BindingOperationInfo.class, new BindingOperationInfo());

        // Set up the out message with an exception (simulating connection failure)
        Message outMessage = new MessageImpl();
        IOException originalException = new IOException("Connection refused");
        outMessage.setContent(Exception.class, originalException);
        outMessage.put(Message.REQUESTOR_ROLE, Boolean.TRUE);
        outMessage.put(Message.INBOUND_MESSAGE, Boolean.FALSE);

        List<Object> params = new ArrayList<>();
        params.add("testParam");
        outMessage.setContent(List.class, params);

        Map<String, Object> context = new HashMap<>();
        Map<String, Object> reqContext = new HashMap<>();
        context.put("RequestContext", reqContext);
        outMessage.put(Message.INVOCATION_CONTEXT, context);

        exchange.setOutMessage(outMessage);

        // Set up a Retryable that always throws (simulating failed retry)
        Retryable failingRetry = (opInfo, prms, ctx, ex) -> {
            throw new IOException("Retry also failed");
        };
        exchange.put(Retryable.class, failingRetry);

        // Create the FailoverTargetSelector
        FailoverTargetSelector selector = new FailoverTargetSelector();
        selector.setEndpoint(endpoint);
        selector.setStrategy(new SequentialStrategy() {
            @Override
            public List<String> getAlternateAddresses(Exchange exch) {
                List<String> addresses = new ArrayList<>();
                addresses.add("http://localhost:9998/alternate");
                return addresses;
            }
        });

        // Prepare the selector (registers the invocation context)
        selector.prepare(outMessage);

        // Call complete — this should trigger failover, which fails, and restore the exception
        selector.complete(exchange);

        // Verify: the original exception must be restored on the outMessage
        Exception restoredEx = outMessage.getContent(Exception.class);
        assertNotNull("Exception should be restored on outMessage after failed retry", restoredEx);
        assertSame("Original exception should be restored", originalException, restoredEx);
    }

    /**
     * Minimal Endpoint stub for testing purposes.
     */
    private static class StubEndpoint extends HashMap<String, Object> implements Endpoint {
        private static final long serialVersionUID = 1L;
        private final EndpointInfo endpointInfo;

        StubEndpoint(EndpointInfo ei) {
            this.endpointInfo = ei;
        }

        public EndpointInfo getEndpointInfo() {
            return endpointInfo;
        }

        public Binding getBinding() {
            return null;
        }

        public Service getService() {
            return null;
        }

        public void setExecutor(Executor executor) {
        }

        public Executor getExecutor() {
            return null;
        }

        public MessageObserver getInFaultObserver() {
            return null;
        }

        public MessageObserver getOutFaultObserver() {
            return null;
        }

        public void setInFaultObserver(MessageObserver observer) {
        }

        public void setOutFaultObserver(MessageObserver observer) {
        }

        public List<Feature> getActiveFeatures() {
            return Collections.emptyList();
        }

        public void addCleanupHook(Closeable c) {
        }

        public List<Closeable> getCleanupHooks() {
            return Collections.emptyList();
        }

        public List<Interceptor<? extends Message>> getInInterceptors() {
            return Collections.emptyList();
        }

        public List<Interceptor<? extends Message>> getOutInterceptors() {
            return Collections.emptyList();
        }

        public List<Interceptor<? extends Message>> getInFaultInterceptors() {
            return Collections.emptyList();
        }

        public List<Interceptor<? extends Message>> getOutFaultInterceptors() {
            return Collections.emptyList();
        }
    }
}
