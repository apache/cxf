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

package org.apache.cxf.transport.jms;

import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.Session;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.classloader.ClassLoaderUtils.ClassLoaderHolder;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.continuations.ContinuationProvider;
import org.apache.cxf.continuations.SuspendedInvocationException;
import org.apache.cxf.interceptor.OneWayProcessorInterceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractMultiplexDestination;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.jms.continuations.JMSContinuation;
import org.apache.cxf.transport.jms.continuations.JMSContinuationProvider;
import org.apache.cxf.transport.jms.util.JMSSender;
import org.apache.cxf.transport.jms.util.JMSUtil;
import org.apache.cxf.transport.jms.util.ResourceCloser;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.EndpointReferenceUtils;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.destination.DestinationResolver;

public class JMSDestination extends AbstractMultiplexDestination 
    implements MessageListener, JMSExchangeSender {

    private static final Logger LOG = LogUtils.getL7dLogger(JMSDestination.class);

    private JMSConfiguration jmsConfig;
    private Bus bus;
    private EndpointInfo ei;
    private AbstractMessageListenerContainer jmsListener;
    private Collection<JMSContinuation> continuations = 
        new ConcurrentLinkedQueue<JMSContinuation>();
    private ClassLoader loader;

    public JMSDestination(Bus b, EndpointInfo info, JMSConfiguration jmsConfig) {
        super(b, getTargetReference(info, b), info);
        this.bus = b;
        this.ei = info;
        this.jmsConfig = jmsConfig;
        info.setProperty(OneWayProcessorInterceptor.USE_ORIGINAL_THREAD, Boolean.TRUE);
        loader = bus.getExtension(ClassLoader.class);
    }

    /**
     * @param inMessage the incoming message
     * @return the inbuilt backchannel
     */
    protected Conduit getInbuiltBackChannel(Message inMessage) {
        EndpointReferenceType anon = EndpointReferenceUtils.getAnonymousEndpointReference();
        return new BackChannelConduit(this, anon, inMessage);
    }

    /**
     * Initialize jmsTemplate and jmsListener from jms configuration data in jmsConfig {@inheritDoc}
     */
    public void activate() {
        getLogger().log(Level.FINE, "JMSDestination activate().... ");
        jmsConfig.ensureProperlyConfigured();
        Object o = ei.getProperty(AbstractMessageListenerContainer.class.getName());
        if (o instanceof AbstractMessageListenerContainer
            && jmsConfig.getMessageListenerContainer() == null) {
            jmsConfig.setMessageListenerContainer((AbstractMessageListenerContainer)o);
        }

        Destination targetDestination = resolveTargetDestination();
        jmsListener = JMSFactory.createJmsListener(ei, jmsConfig, this, 
                                                   targetDestination);
    }

    private Destination resolveTargetDestination() {
        ResourceCloser closer = new ResourceCloser();
        try {
            Session session = JMSFactory.createJmsSessionFactory(jmsConfig, closer).createSession();
            return jmsConfig.getTargetDestination(session);
        } catch (JMSException e) {
            throw JMSUtil.convertJmsException(e);
        } finally {
            closer.close();
        }
    }

    public void deactivate() {
        if (jmsListener != null) {
            jmsListener.shutdown();
        }
    }

    public void shutdown() {
        getLogger().log(Level.FINE, "JMSDestination shutdown()");
        this.deactivate();
    }

    public Destination getReplyToDestination(Session session, 
                                             Message inMessage) throws JMSException {
        javax.jms.Message message = (javax.jms.Message)inMessage.get(JMSConstants.JMS_REQUEST_MESSAGE);
        // If WS-Addressing had set the replyTo header.
        final String replyToName = (String)inMessage.get(JMSConstants.JMS_REBASED_REPLY_TO);
        if (replyToName != null) {
            DestinationResolver resolver = jmsConfig.getDestinationResolver();
            return resolver.resolveDestinationName(session, replyToName, jmsConfig.isReplyPubSubDomain());
        } else if (message.getJMSReplyTo() != null) {
            return message.getJMSReplyTo();
        } else {
            return jmsConfig.getReplyDestination(session);
        }
    }

    /**
     * Decides what correlationId to use for the reply by looking at the request headers
     * 
     * @param request jms request message
     * @return correlation id of request if set else message id from request
     * @throws JMSException
     */
    public String determineCorrelationID(javax.jms.Message request) throws JMSException {
        return StringUtils.isEmpty(request.getJMSCorrelationID())
            ? request.getJMSMessageID() 
            : request.getJMSCorrelationID();
    }

    /**
     * Convert JMS message received by ListenerThread to CXF message and inform incomingObserver that a
     * message was received. The observer will call the service and then send the response CXF message by
     * using the BackChannelConduit
     * 
     * @param message
     * @throws IOException
     */
    public void onMessage(javax.jms.Message message) {
        ClassLoaderHolder origLoader = null;
        Bus origBus = null;
        try {
            if (loader != null) {
                origLoader = ClassLoaderUtils.setThreadContextClassloader(loader);
            }
            getLogger().log(Level.INFO, "JMS destination received message " + message + " on " 
                + jmsConfig.getTargetDestination());
            Message inMessage = JMSMessageUtils.asCXFMessage(message, JMSConstants.JMS_SERVER_REQUEST_HEADERS);
            SecurityContext securityContext = JMSMessageUtils.buildSecurityContext(message, jmsConfig);
            inMessage.put(SecurityContext.class, securityContext);
            inMessage.put(JMSConstants.JMS_SERVER_RESPONSE_HEADERS, new JMSMessageHeadersType());
            inMessage.put(JMSConstants.JMS_REQUEST_MESSAGE, message);
            ((MessageImpl)inMessage).setDestination(this);
            if (jmsConfig.getMaxSuspendedContinuations() != 0) {
                inMessage.put(ContinuationProvider.class.getName(), 
                              new JMSContinuationProvider(bus,
                                                          inMessage,
                                                          incomingObserver,
                                                          continuations,
                                                          jmsListener,
                                                          jmsConfig));
            }
            
            origBus = BusFactory.getAndSetThreadDefaultBus(bus);

            
            Map<Class<?>, ?> mp = JCATransactionalMessageListenerContainer.ENDPOINT_LOCAL.get();
            if (mp != null) {
                for (Map.Entry<Class<?>, ?> ent : mp.entrySet()) {
                    inMessage.setContent(ent.getKey(), ent.getValue());
                }
                JCATransactionalMessageListenerContainer.ENDPOINT_LOCAL.remove();
            }

            // handle the incoming message
            incomingObserver.onMessage(inMessage);
            
            if (inMessage.getExchange() != null 
                && inMessage.getExchange().getInMessage() != null) {
                inMessage = inMessage.getExchange().getInMessage();
            }

            // need to propagate any exceptions back so transactions can occur
            if (inMessage.getContent(Exception.class) != null) {
                Exception ex = inMessage.getContent(Exception.class);
                if (ex.getCause() instanceof RuntimeException) {
                    throw (RuntimeException)ex.getCause();
                } else {
                    throw new RuntimeException(ex);
                }
            }
            
        } catch (SuspendedInvocationException ex) {
            getLogger().log(Level.FINE, "Request message has been suspended");
        } catch (UnsupportedEncodingException ex) {
            getLogger().log(Level.WARNING, "can't get the right encoding information. " + ex);
        } catch (JMSException e) {
            JMSUtil.convertJmsException(e);
        } finally {
            if (origBus != bus) {
                BusFactory.setThreadDefaultBus(origBus);
            }
            if (origLoader != null) { 
                origLoader.reset();
            }
        }
    }

    public void sendExchange(Exchange exchange, final Object replyObj) {
        if (exchange.isOneWay()) {
            //Don't need to send anything
            return;
        }
        final Message inMessage = exchange.getInMessage();
        final Message outMessage = exchange.getOutMessage();

        ResourceCloser closer = new ResourceCloser();
        try {
            Session session = JMSFactory.createJmsSessionFactory(jmsConfig, closer).createSession();

            final JMSMessageHeadersType messageProperties = (JMSMessageHeadersType)outMessage
                .get(JMSConstants.JMS_SERVER_RESPONSE_HEADERS);
            JMSMessageHeadersType inMessageProperties = (JMSMessageHeadersType)inMessage
                .get(JMSConstants.JMS_SERVER_REQUEST_HEADERS);
            initResponseMessageProperties(messageProperties, inMessageProperties);

            // setup the reply message
            final javax.jms.Message request = (javax.jms.Message)inMessage
                .get(JMSConstants.JMS_REQUEST_MESSAGE);
            final String msgType = JMSMessageUtils.isMtomEnabled(outMessage)
                ? JMSConstants.BINARY_MESSAGE_TYPE : JMSMessageUtils.getMessageType(request);
            if (isTimedOut(request)) {
                return;
            }

            Destination replyTo = getReplyToDestination(session, inMessage);
            if (replyTo == null) {
                throw new RuntimeException("No replyTo destination set");
            }

            getLogger().log(Level.FINE, "send out the message!");

            String correlationId = determineCorrelationID(request);
            javax.jms.Message reply = JMSMessageUtils.asJMSMessage(jmsConfig, 
                                      outMessage, 
                                      replyObj, 
                                      msgType,
                                      session,
                                      correlationId, JMSConstants.JMS_SERVER_RESPONSE_HEADERS);
            JMSSender sender = JMSFactory.createJmsSender(jmsConfig, messageProperties);
            LOG.log(Level.FINE, "server sending reply: ", reply);
            sender.sendMessage(closer, session, replyTo, reply);
        } catch (JMSException ex) {
            throw JmsUtils.convertJmsAccessException(ex);
        } finally {
            closer.close();
        }
    }
    
    /**
     * @param messageProperties
     * @param inMessageProperties
     */
    public static void initResponseMessageProperties(JMSMessageHeadersType messageProperties,
                                                     JMSMessageHeadersType inMessageProperties) {
        messageProperties.setJMSDeliveryMode(inMessageProperties.getJMSDeliveryMode());
        messageProperties.setJMSPriority(inMessageProperties.getJMSPriority());
        messageProperties.setSOAPJMSRequestURI(inMessageProperties.getSOAPJMSRequestURI());
        messageProperties.setSOAPJMSBindingVersion("1.0");
    }


    private boolean isTimedOut(final javax.jms.Message request) throws JMSException {
        if (request.getJMSExpiration() > 0) {
            TimeZone tz = new SimpleTimeZone(0, "GMT");
            Calendar cal = new GregorianCalendar(tz);
            long timeToLive = request.getJMSExpiration() - cal.getTimeInMillis();
            if (timeToLive < 0) {
                getLogger()
                    .log(Level.INFO, "Message time to live is already expired skipping response.");
                return true;
            }
        }
        return false;
    }

    protected Logger getLogger() {
        return LOG;
    }

    public JMSConfiguration getJmsConfig() {
        return jmsConfig;
    }

    public void setJmsConfig(JMSConfiguration jmsConfig) {
        this.jmsConfig = jmsConfig;
    }

}
