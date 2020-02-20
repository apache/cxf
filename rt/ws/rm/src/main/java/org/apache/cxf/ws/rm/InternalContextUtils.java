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

package org.apache.cxf.ws.rm;


import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.ConduitSelector;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.NullConduitSelector;
import org.apache.cxf.endpoint.PreexistingConduitSelector;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.interceptor.OutgoingChainInterceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.workqueue.OneShotAsyncExecutor;
import org.apache.cxf.workqueue.SynchronousExecutor;
import org.apache.cxf.workqueue.WorkQueueManager;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.EndpointReferenceUtils;
import org.apache.cxf.ws.addressing.Names;



/**
 * Holder for utility methods relating to contexts. Somewhat stripped-down version of class of same name in
 * org.apache.cxf.ws.addressing.impl.
 */
final class InternalContextUtils {
    private static final class DecoupledDestination implements Destination {
        private final EndpointInfo ei;
        private final EndpointReferenceType reference;

        private DecoupledDestination(EndpointInfo ei, EndpointReferenceType reference) {
            this.ei = ei;
            this.reference = reference;
        }

        public EndpointReferenceType getAddress() {
            return reference;
        }

        public Conduit getBackChannel(Message inMessage) throws IOException {
            if (ContextUtils.isNoneAddress(reference)) {
                return null;
            }
            Bus bus = inMessage.getExchange().getBus();
            //this is a response targeting a decoupled endpoint.   Treat it as a oneway so
            //we don't wait for a response.
            inMessage.getExchange().setOneWay(true);
            ConduitInitiator conduitInitiator
                = bus.getExtension(ConduitInitiatorManager.class)
                    .getConduitInitiatorForUri(reference.getAddress().getValue());
            if (conduitInitiator != null) {
                Conduit c = conduitInitiator.getConduit(ei, reference, bus);
                // ensure decoupled back channel input stream is closed
                c.setMessageObserver(new MessageObserver() {
                    public void onMessage(Message m) {
                        InputStream is = m.getContent(InputStream.class);
                        if (is != null) {
                            try {
                                is.close();
                            } catch (Exception e) {
                                // ignore
                            }
                        }
                    }
                });
                return c;
            }
            return null;
        }

        public MessageObserver getMessageObserver() {
            return null;
        }

        public void shutdown() {
        }

        public void setMessageObserver(MessageObserver observer) {
        }
    }

    private static final Logger LOG = LogUtils.getL7dLogger(InternalContextUtils.class);

   /**
    * Prevents instantiation.
    */
    private InternalContextUtils() {
    }


    /**
     * Rebase response on replyTo
     *
     * @param reference the replyTo reference
     * @param inMAPs the inbound MAPs
     * @param inMessage the current message
     */
    //CHECKSTYLE:OFF Max executable statement count limitation
    public static void rebaseResponse(EndpointReferenceType reference,
                                      AddressingProperties inMAPs,
                                      final Message inMessage) {

        String namespaceURI = inMAPs.getNamespaceURI();
        if (!ContextUtils.retrievePartialResponseSent(inMessage)) {
            ContextUtils.storePartialResponseSent(inMessage);
            Exchange exchange = inMessage.getExchange();
            Message fullResponse = exchange.getOutMessage();
            Message partialResponse = ContextUtils.createMessage(exchange);
            ensurePartialResponseMAPs(partialResponse, namespaceURI);

            // ensure the inbound MAPs are available in the partial response
            // message (used to determine relatesTo etc.)
            ContextUtils.propogateReceivedMAPs(inMAPs, partialResponse);
            Destination target = inMessage.getDestination();
            if (target == null) {
                return;
            }

            try {
                if (reference == null) {
                    reference = ContextUtils.getNoneEndpointReference();
                }
                Conduit backChannel = target.getBackChannel(inMessage);
                if (backChannel != null) {
                    partialResponse.put(Message.PARTIAL_RESPONSE_MESSAGE, Boolean.TRUE);
                    partialResponse.put(Message.EMPTY_PARTIAL_RESPONSE_MESSAGE, Boolean.TRUE);
                    boolean robust = MessageUtils.getContextualBoolean(inMessage, Message.ROBUST_ONEWAY, false);

                    if (robust) {
                        BindingOperationInfo boi = exchange.getBindingOperationInfo();
                        // insert the executor in the exchange to fool the OneWayProcessorInterceptor
                        exchange.put(Executor.class, getExecutor(inMessage));
                        // pause dispatch on current thread and resume...
                        inMessage.getInterceptorChain().pause();
                        inMessage.getInterceptorChain().resume();
                        // restore the BOI for the partial response handling
                        exchange.put(BindingOperationInfo.class, boi);
                    }


                    // set up interceptor chains and send message
                    InterceptorChain chain =
                        fullResponse != null
                        ? fullResponse.getInterceptorChain()
                        : OutgoingChainInterceptor.getOutInterceptorChain(exchange);
                    exchange.setOutMessage(partialResponse);
                    partialResponse.setInterceptorChain(chain);
                    exchange.put(ConduitSelector.class,
                                 new PreexistingConduitSelector(backChannel,
                                                                exchange.getEndpoint()));

                    if (chain != null && !chain.doIntercept(partialResponse)
                        && partialResponse.getContent(Exception.class) != null) {
                        if (partialResponse.getContent(Exception.class) instanceof Fault) {
                            throw (Fault)partialResponse.getContent(Exception.class);
                        }
                        throw new Fault(partialResponse.getContent(Exception.class));
                    }
                    if (chain != null) {
                        chain.reset();
                    }
                    exchange.put(ConduitSelector.class, new NullConduitSelector());

                    if (fullResponse == null) {
                        fullResponse = ContextUtils.createMessage(exchange);
                    }
                    exchange.setOutMessage(fullResponse);

                    Destination destination = createDecoupledDestination(
                        exchange,
                        reference);
                    exchange.setDestination(destination);

                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "SERVER_TRANSPORT_REBASE_FAILURE_MSG", e);
            }
        }
    }
    //CHECKSTYLE:ON

    private static Destination createDecoupledDestination(
        Exchange exchange, final EndpointReferenceType reference) {
        final EndpointInfo ei = exchange.getEndpoint().getEndpointInfo();
        return new DecoupledDestination(ei, reference);
    }

    /**
     * Construct and store MAPs for partial response.
     *
     * @param partialResponse the partial response message
     * @param namespaceURI the current namespace URI
     */
    private static void ensurePartialResponseMAPs(Message partialResponse,
                                                 String namespaceURI) {
        // ensure there is a MAPs instance available for the outbound
        // partial response that contains appropriate To and ReplyTo
        // properties (i.e. anonymous & none respectively)
        AddressingProperties maps = new AddressingProperties();
        maps.setTo(EndpointReferenceUtils.getAnonymousEndpointReference());
        maps.setReplyTo(ContextUtils.WSA_OBJECT_FACTORY.createEndpointReferenceType());
        maps.getReplyTo().setAddress(ContextUtils.getAttributedURI(Names.WSA_NONE_ADDRESS));
        maps.setAction(ContextUtils.getAttributedURI(""));
        maps.exposeAs(namespaceURI);
        ContextUtils.storeMAPs(maps, partialResponse, true, true, false);
    }

    /**
     * Get the Executor for this invocation.
     * @param message
     * @return
     */
    private static Executor getExecutor(final Message message) {
        Endpoint endpoint = message.getExchange().getEndpoint();
        Executor executor = endpoint.getService().getExecutor();

        if (executor == null || SynchronousExecutor.isA(executor)) {
            // need true asynchrony
            Bus bus = message.getExchange().getBus();
            if (bus != null) {
                WorkQueueManager workQueueManager =
                    bus.getExtension(WorkQueueManager.class);
                Executor autoWorkQueue =
                    workQueueManager.getNamedWorkQueue("ws-addressing");
                executor = autoWorkQueue != null
                           ? autoWorkQueue
                           :  workQueueManager.getAutomaticWorkQueue();
            } else {
                executor = OneShotAsyncExecutor.getInstance();
            }
        }
        message.getExchange().put(Executor.class, executor);
        return executor;
    }

}
