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

package org.apache.cxf.ws.addressing;


import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.cxf.binding.soap.model.SoapOperationInfo;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.endpoint.ConduitSelector;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.NullConduitSelector;
import org.apache.cxf.endpoint.PreexistingConduitSelector;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.interceptor.OutgoingChainInterceptor;
import org.apache.cxf.io.DelegatingInputStream;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.Extensible;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.workqueue.OneShotAsyncExecutor;
import org.apache.cxf.workqueue.SynchronousExecutor;
import org.apache.cxf.workqueue.WorkQueueManager;
import org.apache.cxf.wsdl.EndpointReferenceUtils;

import static org.apache.cxf.message.Message.ASYNC_POST_RESPONSE_DISPATCH;
import static org.apache.cxf.message.Message.REQUESTOR_ROLE;

import static org.apache.cxf.ws.addressing.JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES;
import static org.apache.cxf.ws.addressing.JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES_INBOUND;
import static org.apache.cxf.ws.addressing.JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES_OUTBOUND;
import static org.apache.cxf.ws.addressing.JAXWSAConstants.SERVER_ADDRESSING_PROPERTIES_INBOUND;
import static org.apache.cxf.ws.addressing.JAXWSAConstants.SERVER_ADDRESSING_PROPERTIES_OUTBOUND;


/**
 * Holder for utility methods relating to contexts.
 */
public final class ContextUtils {

    public static final ObjectFactory WSA_OBJECT_FACTORY = new ObjectFactory();
    public static final String ACTION = ContextUtils.class.getName() + ".ACTION";

    private static final EndpointReferenceType NONE_ENDPOINT_REFERENCE = 
        EndpointReferenceUtils.getEndpointReference(Names.WSA_NONE_ADDRESS);
    
    private static final Logger LOG = LogUtils.getL7dLogger(ContextUtils.class);
    
    /**
     * Used to fabricate a Uniform Resource Name from a UUID string
     */
    private static final String URN_UUID = "urn:uuid:";
    
    private static JAXBContext jaxbContext;
     
    /**
     * Used by MAPAggregator to cache bad MAP fault name
     */
    private static final String MAP_FAULT_NAME_PROPERTY = 
        "org.apache.cxf.ws.addressing.map.fault.name";

    /**
     * Used by MAPAggregator to cache bad MAP fault reason
     */
    private static final String MAP_FAULT_REASON_PROPERTY = 
        "org.apache.cxf.ws.addressing.map.fault.reason";
    
    /**
     * Indicates a partial response has already been sent
     */
    private static final String PARTIAL_REPONSE_SENT_PROPERTY =
        "org.apache.cxf.ws.addressing.partial.response.sent";
 
   /**
    * Prevents instantiation.
    */
    private ContextUtils() {
    }

   /**
    * Determine if message is outbound.
    *
    * @param message the current Message
    * @return true iff the message direction is outbound
    */
    public static boolean isOutbound(Message message) {
        Exchange exchange = message.getExchange();
        return message != null
               && exchange != null
               && (message == exchange.getOutMessage()
                   || message == exchange.getOutFaultMessage());
    }

   /**
    * Determine if message is fault.
    *
    * @param message the current Message
    * @return true iff the message is a fault
    */
    public static boolean isFault(Message message) {
        return message != null
               && message.getExchange() != null
               && (message == message.getExchange().getInFaultMessage()
                   || message == message.getExchange().getOutFaultMessage());
    }

   /**
    * Determine if current messaging role is that of requestor.
    *
    * @param message the current Message
    * @return true if the current messaging role is that of requestor
    */
    public static boolean isRequestor(Message message) {
        Boolean requestor = (Boolean)message.get(REQUESTOR_ROLE);
        return requestor != null && requestor.booleanValue();
    }

    /**
     * Get appropriate property name for message addressing properties.
     *
     * @param isProviderContext true if the binding provider request context 
     * available to the client application as opposed to the message context 
     * visible to handlers
     * @param isRequestor true if the current messaging role is that of
     * requestor
     * @param isOutbound true if the message is outbound
     * @return the property name to use when caching the MAPs in the context
     */
    public static String getMAPProperty(boolean isRequestor, 
                                        boolean isProviderContext,
                                        boolean isOutbound) {
        return isRequestor
                ? isProviderContext
                 ? CLIENT_ADDRESSING_PROPERTIES
                 : isOutbound
                   ? CLIENT_ADDRESSING_PROPERTIES_OUTBOUND
                   : CLIENT_ADDRESSING_PROPERTIES_INBOUND
               : isOutbound
                 ? SERVER_ADDRESSING_PROPERTIES_OUTBOUND
                 : SERVER_ADDRESSING_PROPERTIES_INBOUND;
    }

    /**
     * Store MAPs in the message.
     *
     * @param message the current message
     * @param isOutbound true if the message is outbound
     */
    public static void storeMAPs(AddressingProperties maps,
                                 Message message,
                                 boolean isOutbound) {
        storeMAPs(maps, message, isOutbound, isRequestor(message), false);
    }

    /**
     * Store MAPs in the message.
     *
     * @param maps the MAPs to store
     * @param message the current message
     * @param isOutbound true if the message is outbound
     * @param isRequestor true if the current messaging role is that of
     * requestor
     * @param handler true if HANDLER scope, APPLICATION scope otherwise
     */
    public static void storeMAPs(AddressingProperties maps,
                                 Message message,
                                 boolean isOutbound, 
                                 boolean isRequestor) {
        storeMAPs(maps, message, isOutbound, isRequestor, false);
    }
    
    /**
     * Store MAPs in the message.
     *
     * @param maps the MAPs to store
     * @param message the current message
     * @param isOutbound true if the message is outbound
     * @param isRequestor true if the current messaging role is that of
     * requestor
     * @param handler true if HANDLER scope, APPLICATION scope otherwise
     * @param isProviderContext true if the binding provider request context 
     */
    public static void storeMAPs(AddressingProperties maps,
                                 Message message,
                                 boolean isOutbound, 
                                 boolean isRequestor,
                                 boolean isProviderContext) {
        if (maps != null) {
            String mapProperty = getMAPProperty(isRequestor, isProviderContext, isOutbound);
            LOG.log(Level.FINE,
                    "associating MAPs with context property {0}",
                    mapProperty);
            message.put(mapProperty, maps);
        }
    }
    
    /**
     * @param message the current message
     * @param isProviderContext true if the binding provider request context
     * available to the client application as opposed to the message context
     * visible to handlers
     * @param isOutbound true if the message is outbound
     * @return the current addressing properties
     */
    public static AddressingPropertiesImpl retrieveMAPs(
                                                   Message message, 
                                                   boolean isProviderContext,
                                                   boolean isOutbound) {
        return retrieveMAPs(message, isProviderContext, isOutbound, true);
    }

    /**
     * @param message the current message
     * @param isProviderContext true if the binding provider request context
     * available to the client application as opposed to the message context
     * visible to handlers
     * @param isOutbound true if the message is outbound
     * @param warnIfMissing log a warning  message if properties cannot be retrieved
     * @return the current addressing properties
     */
    public static AddressingPropertiesImpl retrieveMAPs(
                                                   Message message, 
                                                   boolean isProviderContext,
                                                   boolean isOutbound,
                                                   boolean warnIfMissing) {
        boolean isRequestor = ContextUtils.isRequestor(message);
        String mapProperty =
            ContextUtils.getMAPProperty(isProviderContext, 
                                        isRequestor,
                                        isOutbound);
        LOG.log(Level.FINE,
                "retrieving MAPs from context property {0}",
                mapProperty);
        AddressingPropertiesImpl maps =
            (AddressingPropertiesImpl)message.get(mapProperty);
        if (maps != null) {
            LOG.log(Level.FINE, "current MAPs {0}", maps);
        } else if (!isProviderContext) {
            LogUtils.log(LOG, warnIfMissing ? Level.WARNING : Level.FINE, 
                "MAPS_RETRIEVAL_FAILURE_MSG");         
        }
        return maps;
    }

    /**
     * Helper method to get an attributed URI.
     *
     * @param uri the URI
     * @return an AttributedURIType encapsulating the URI
     */
    public static AttributedURIType getAttributedURI(String uri) {
        AttributedURIType attributedURI = 
            WSA_OBJECT_FACTORY.createAttributedURIType();
        attributedURI.setValue(uri);
        return attributedURI;
    }

    /**
     * Helper method to get a RealtesTo instance.
     *
     * @param uri the related URI
     * @return a RelatesToType encapsulating the URI
     */
    public static RelatesToType getRelatesTo(String uri) {
        RelatesToType relatesTo =
            WSA_OBJECT_FACTORY.createRelatesToType();
        relatesTo.setValue(uri);
        return relatesTo;
    }

    /**
     * Helper method to determine if an EPR address is generic (either null,
     * none or anonymous).
     *
     * @param ref the EPR under test
     * @return true if the address is generic
     */
    public static boolean isGenericAddress(EndpointReferenceType ref) {
        return ref == null 
               || ref.getAddress() == null
               || Names.WSA_ANONYMOUS_ADDRESS.equals(ref.getAddress().getValue())
               || Names.WSA_NONE_ADDRESS.equals(ref.getAddress().getValue());
    }

    /**
     * Helper method to determine if an MAPs Action is empty (a null action
     * is considered empty, whereas a zero length action suppresses
     * the propagation of the Action property).
     *
     * @param ref the MAPs Action under test
     * @return true if the Action is empty
     */
    public static boolean hasEmptyAction(AddressingProperties maps) {
        boolean empty = maps.getAction() == null;
        if (maps.getAction() != null 
            && maps.getAction().getValue().length() == 0) {
            maps.setAction(null);
            empty = false;
        } 
        return empty;
    }
    
    /**
     * Rebase response on replyTo
     * 
     * @param reference the replyTo reference
     * @param inMAPs the inbound MAPs
     * @param inMessage the current message
     */
    public static void rebaseResponse(EndpointReferenceType reference,
                                      AddressingProperties inMAPs,
                                      final Message inMessage) {
        
        String namespaceURI = inMAPs.getNamespaceURI();
        if (!retrievePartialResponseSent(inMessage)) {
            storePartialResponseSent(inMessage);
            Exchange exchange = inMessage.getExchange();
            Message fullResponse = exchange.getOutMessage();
            Message partialResponse = createMessage(exchange);
            ensurePartialResponseMAPs(partialResponse, namespaceURI);
            
            // ensure the inbound MAPs are available in the partial response
            // message (used to determine relatesTo etc.)
            propogateReceivedMAPs(inMAPs, partialResponse);
            partialResponse.put(Message.PARTIAL_RESPONSE_MESSAGE, Boolean.TRUE);
            Destination target = inMessage.getDestination();
            if (target == null) {
                return;
            }
            
            try {
                exchange.setOutMessage(partialResponse);
                Conduit backChannel = target.getBackChannel(inMessage,
                                                            partialResponse,
                                                            reference);

                if (backChannel != null) {
                    // set up interceptor chains and send message
                    InterceptorChain chain =
                        fullResponse != null
                        ? fullResponse.getInterceptorChain()
                        : OutgoingChainInterceptor.getOutInterceptorChain(exchange);
                    partialResponse.setInterceptorChain(chain);
                    exchange.put(ConduitSelector.class,
                                 new PreexistingConduitSelector(backChannel,
                                                                exchange.get(Endpoint.class)));

                    if (chain != null && !chain.doIntercept(partialResponse) 
                        && partialResponse.getContent(Exception.class) != null) {
                        if (partialResponse.getContent(Exception.class) instanceof Fault) {
                            throw (Fault)partialResponse.getContent(Exception.class);
                        } else {
                            throw new Fault(partialResponse.getContent(Exception.class));
                        }
                    }
                    if (chain != null) {
                        chain.reset();                        
                    }
                    exchange.put(ConduitSelector.class, new NullConduitSelector());
                    if (fullResponse != null) {
                        exchange.setOutMessage(fullResponse);
                    } else {
                        fullResponse = createMessage(exchange);
                        exchange.setOutMessage(fullResponse);
                    }
                    
                    if (retrieveAsyncPostResponseDispatch(inMessage)) {
                        //need to suck in all the data from the input stream as
                        //the transport might discard any data on the stream when this 
                        //thread unwinds or when the empty response is sent back
                        DelegatingInputStream in = inMessage.get(DelegatingInputStream.class);
                        if (in != null) {
                            in.cacheInput();
                        }
                        
                        // async service invocation required *after* a response
                        // has been sent (i.e. to a oneway, or a partial response
                        // to a decoupled twoway)
                        
                        // pause dispatch on current thread ...
                        inMessage.getInterceptorChain().pause();

                        // ... and resume on executor thread
                        getExecutor(inMessage).execute(new Runnable() {
                            public void run() {
                                inMessage.getInterceptorChain().resume();
                            }
                        });
                    }
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "SERVER_TRANSPORT_REBASE_FAILURE_MSG", e);
            }
        }
    }

    
    /**
     * Propogate inbound MAPs onto full reponse & fault messages.
     * 
     * @param inMAPs the inbound MAPs
     * @param exchange the current Exchange
     */
    public static void propogateReceivedMAPs(AddressingProperties inMAPs,
                                              Exchange exchange) {
        if (exchange.getOutMessage() == null) {
            exchange.setOutMessage(createMessage(exchange));
        }
        propogateReceivedMAPs(inMAPs, exchange.getOutMessage());
        if (exchange.getOutFaultMessage() == null) {
            exchange.setOutFaultMessage(createMessage(exchange));
        }
        propogateReceivedMAPs(inMAPs, exchange.getOutFaultMessage());
    }

    /**
     * Propogate inbound MAPs onto reponse message if applicable
     * (not applicable for oneways).
     * 
     * @param inMAPs the inbound MAPs
     * @param responseMessage
     */
    private static void propogateReceivedMAPs(AddressingProperties inMAPs,
                                             Message responseMessage) {
        if (responseMessage != null) {
            storeMAPs(inMAPs, responseMessage, false, false, false);
        }
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
        AddressingPropertiesImpl maps = new AddressingPropertiesImpl();
        maps.setTo(EndpointReferenceUtils.getAnonymousEndpointReference());
        maps.setReplyTo(WSA_OBJECT_FACTORY.createEndpointReferenceType());
        maps.getReplyTo().setAddress(getAttributedURI(Names.WSA_NONE_ADDRESS));
        maps.setAction(getAttributedURI(""));
        maps.exposeAs(namespaceURI);
        storeMAPs(maps, partialResponse, true, true, false);
    }

    /**
     * Get the Executor for this invocation.
     * @param endpoint
     * @return
     */
    private static Executor getExecutor(final Message message) {
        Endpoint endpoint = message.getExchange().get(Endpoint.class);
        Executor executor = endpoint.getService().getExecutor();
        
        if (executor == null || SynchronousExecutor.isA(executor)) {
            // need true asynchrony
            Bus bus = message.getExchange().get(Bus.class);
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
    
    /**
     * Store bad MAP fault name in the message.
     *
     * @param faultName the fault name to store
     * @param message the current message
     */
    public static void storeMAPFaultName(String faultName, 
                                         Message message) {
        message.put(MAP_FAULT_NAME_PROPERTY, faultName);
    }

    /**
     * Retrieve MAP fault name from the message.
     *
     * @param message the current message
     * @returned the retrieved fault name
     */
    public static String retrieveMAPFaultName(Message message) {
        return (String)message.get(MAP_FAULT_NAME_PROPERTY);
    }

    /**
     * Store MAP fault reason in the message.
     *
     * @param reason the fault reason to store
     * @param message the current message
     */
    public static void storeMAPFaultReason(String reason, 
                                           Message message) {
        message.put(MAP_FAULT_REASON_PROPERTY, reason);
    }

    /**
     * Retrieve MAP fault reason from the message.
     *
     * @param message the current message
     * @returned the retrieved fault reason
     */
    public static String retrieveMAPFaultReason(Message message) {
        return (String)message.get(MAP_FAULT_REASON_PROPERTY);
    }
    
    /**
     * Store an indication that a partial response has been sent.
     * Relavant if *both* the replyTo & faultTo are decoupled,
     * and a fault occurs, then we would already have sent the
     * partial response (pre-dispatch) for the replyTo, so
     * no need to send again.
     *
     * @param message the current message
     */
    public static void storePartialResponseSent(Message message) {
        message.put(PARTIAL_REPONSE_SENT_PROPERTY, Boolean.TRUE);
    }

    /**
     * Retrieve indication that a partial response has been sent.
     *
     * @param message the current message
     * @returned the retrieved indication that a partial response
     * has been sent
     */
    public static boolean retrievePartialResponseSent(Message message) {
        Boolean ret = (Boolean)message.get(PARTIAL_REPONSE_SENT_PROPERTY);
        return ret != null && ret.booleanValue();
    }

    /**
     * Store indication that a deferred uncorrelated message abort is
     * supported
     *
     * @param message the current message
     */
    public static void storeDeferUncorrelatedMessageAbort(Message message) {
        if (message.getExchange() != null) { 
            message.getExchange().put("defer.uncorrelated.message.abort", Boolean.TRUE);
        }
    }

    /**
     * Retrieve indication that a deferred uncorrelated message abort is
     * supported
     *
     * @param message the current message
     * @returned the retrieved indication 
     */
    public static boolean retrieveDeferUncorrelatedMessageAbort(Message message) {
        Boolean ret = message.getExchange() != null 
                      ? (Boolean)message.getExchange().get("defer.uncorrelated.message.abort")
                      : null;
        return ret != null && ret.booleanValue();
    }

    /**
     * Store indication that a deferred uncorrelated message abort should
     * occur
     *
     * @param message the current message
     */
    public static void storeDeferredUncorrelatedMessageAbort(Message message) {
        if (message.getExchange() != null) { 
            message.getExchange().put("deferred.uncorrelated.message.abort", Boolean.TRUE);
        }
    }

    /**
     * Retrieve indication that a deferred uncorrelated message abort should
     * occur.
     *
     * @param message the current message
     * @returned the retrieved indication 
     */
    public static boolean retrieveDeferredUncorrelatedMessageAbort(Message message) {
        Boolean ret = message.getExchange() != null 
                      ? (Boolean)message.getExchange().get("deferred.uncorrelated.message.abort")
                      : null;
        return ret != null && ret.booleanValue();
    }

    /**
     * Retrieve indication that an async post-response service invocation
     * is required.
     * 
     * @param message the current message
     * @returned the retrieved indication that an async post-response service
     * invocation is required.
     */
    public static boolean retrieveAsyncPostResponseDispatch(Message message) {
        Boolean ret = (Boolean)message.get(ASYNC_POST_RESPONSE_DISPATCH);
        return ret != null && ret.booleanValue();
    }
    
    /**
     * Retrieve a JAXBContext for marshalling and unmarshalling JAXB generated
     * types.
     *
     * @return a JAXBContext 
     */
    public static JAXBContext getJAXBContext() throws JAXBException {
        synchronized (ContextUtils.class) {
            if (jaxbContext == null) {
                jaxbContext =
                    JAXBContext.newInstance(
                        WSA_OBJECT_FACTORY.getClass().getPackage().getName(),
                        WSA_OBJECT_FACTORY.getClass().getClassLoader());
            }
        }
        return jaxbContext;
    }

    /**
     * Set the encapsulated JAXBContext (used by unit tests).
     * 
     * @param ctx JAXBContext 
     */
    public static void setJAXBContext(JAXBContext ctx) throws JAXBException {
        synchronized (ContextUtils.class) {
            jaxbContext = ctx;
        }
    }
    
    
    /**
     * @return a generated UUID
     */
    public static String generateUUID() {
        return URN_UUID + UUID.randomUUID();
    }
    
    /**
     * Retreive Conduit from Exchange if not already available
     * 
     * @param conduit the current value for the Conduit
     * @param message the current message
     * @return the Conduit if available
     */
    public static Conduit getConduit(Conduit conduit, Message message) {
        if (conduit == null) {
            Exchange exchange = message.getExchange();
            conduit = exchange != null ? exchange.getConduit(message) : null;
        }
        return conduit;
    }
    
    /**
     * Construct the Action URI.
     * 
     * @param message the current message
     * @return the Action URI
     */
    public static AttributedURIType getAction(Message message) {
        String action = null;
        LOG.fine("Determining action");
        Exception fault = message.getContent(Exception.class);

        // REVISIT: add support for @{Fault}Action annotation (generated
        // from the wsaw:Action WSDL element). For the moment we just
        // pick up the wsaw:Action attribute by walking the WSDL model
        // directly 
        action = getActionFromServiceModel(message, fault);
        LOG.fine("action: " + action);
        return action != null ? getAttributedURI(action) : null;
    }

    /**
     * Get action from service model.
     *
     * @param message the current message
     * @param fault the fault if one is set
     */
    private static String getActionFromServiceModel(Message message,
                                                    Exception fault) {
        String action = null;
        BindingOperationInfo bindingOpInfo =
            message.getExchange().get(BindingOperationInfo.class);
        if (bindingOpInfo != null) {
            if (bindingOpInfo.isUnwrappedCapable()) {
                bindingOpInfo = bindingOpInfo.getUnwrappedOperation();
            }
            if (fault == null) {
                action = (String)message.get(ACTION);
                if (StringUtils.isEmpty(action)) {
                    action = (String) message.get(SoapBindingConstants.SOAP_ACTION);
                }
                if (action == null || "".equals(action)) {
                    MessageInfo msgInfo = 
                        ContextUtils.isRequestor(message)
                        ? bindingOpInfo.getOperationInfo().getInput()
                        : bindingOpInfo.getOperationInfo().getOutput();
                    String cachedAction = (String)msgInfo.getProperty(ACTION);
                    if (cachedAction == null) {
                        action = getActionFromMessageAttributes(msgInfo);
                    } else {
                        action = cachedAction;
                    }
                    if (action == null && ContextUtils.isRequestor(message)) {
                        SoapOperationInfo soi = getSoapOperationInfo(bindingOpInfo);
                        action = soi == null ? null : soi.getAction();
                        action = StringUtils.isEmpty(action) ? null : action; 
                    }
                }
            } else {
                Throwable t = fault.getCause();
                
                // FaultAction attribute is not defined in 
                // http://www.w3.org/2005/02/addressing/wsdl schema
                for (BindingFaultInfo bfi : bindingOpInfo.getFaults()) {
                    FaultInfo fi = bfi.getFaultInfo();
                    Class<?> fiTypeClass = fi.getMessagePart(0).getTypeClass();
                    if (t != null 
                            && fiTypeClass != null
                            && t.getClass().isAssignableFrom(fiTypeClass)) {
                        if (fi.getExtensionAttributes() == null) {
                            continue;
                        }
                        String attr = (String)
                            fi.getExtensionAttributes().get(Names.WSAW_ACTION_QNAME);
                        if (attr == null) {
                            attr = (String)        
                                fi.getExtensionAttributes()
                                    .get(new QName(Names.WSA_NAMESPACE_WSDL_NAME_OLD,
                                                    Names.WSAW_ACTION_NAME));                            
                        }
                        if (attr != null) {
                            action = attr;
                            break;
                        }
                    }
                }
            }
        }
        LOG.fine("action determined from service model: " + action);
        return action;
    }

    public static SoapOperationInfo getSoapOperationInfo(BindingOperationInfo bindingOpInfo) {
        SoapOperationInfo soi = bindingOpInfo.getExtensor(SoapOperationInfo.class);
        if (soi == null && bindingOpInfo.isUnwrapped()) {
            soi = bindingOpInfo.getWrappedOperation()
                .getExtensor(SoapOperationInfo.class);
        }
        return soi;
    }

    /**
     * Get action from attributes on MessageInfo
     *
     * @param bindingOpInfo the current BindingOperationInfo
     * @param msgInfo the current MessageInfo
     * @return the action if set
     */
    private static String getActionFromMessageAttributes(MessageInfo msgInfo) {
        String action = null;
        if (msgInfo != null
            && msgInfo.getExtensionAttributes() != null) {
            String attr = getAction(msgInfo);
            if (!StringUtils.isEmpty(attr)) {
                action = attr;
                msgInfo.setProperty(ACTION, action);
            }
        }
        return action;
    }

    public static String getAction(Extensible ext) {
        Object o = ext.getExtensionAttribute(JAXWSAConstants.WSAW_ACTION_QNAME);
        if (o == null) {
            o = ext.getExtensionAttributes().get(new QName(Names.WSA_NAMESPACE_WSDL_METADATA,
                                                           Names.WSAW_ACTION_NAME));
        }
        if (o == null) {
            o = ext.getExtensionAttributes().get(new QName(Names.WSA_NAMESPACE_WSDL_NAME_OLD,
                                                   Names.WSAW_ACTION_NAME));
        }
        if (o instanceof QName) {
            return ((QName)o).getLocalPart();
        }
        return o.toString();
    }
    public static EndpointReferenceType getNoneEndpointReference() {
        return NONE_ENDPOINT_REFERENCE;
    }

    public static void applyReferenceParam(EndpointReferenceType toEpr, JAXBElement<String> el) {
        if (null == toEpr.getReferenceParameters()) {
            toEpr.setReferenceParameters(WSA_OBJECT_FACTORY.createReferenceParametersType());
        }
        toEpr.getReferenceParameters().getAny().add(el);
    }

    /**
     * Create a Binding specific Message.
     * 
     * @param message the current message
     * @return the Method from the BindingOperationInfo
     */
    private static Message createMessage(Exchange exchange) {
        Endpoint ep = exchange.get(Endpoint.class);
        Message msg = null;
        if (ep != null) {
            msg = new MessageImpl();
            msg.setExchange(exchange);
            if (ep.getBinding() != null) {
                msg = ep.getBinding().createMessage(msg);
            }
        }
        return msg;
    }
    
}
