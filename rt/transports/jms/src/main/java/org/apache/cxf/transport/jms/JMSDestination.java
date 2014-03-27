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
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.Session;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.classloader.ClassLoaderUtils.ClassLoaderHolder;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.continuations.ContinuationProvider;
import org.apache.cxf.continuations.SuspendedInvocationException;
import org.apache.cxf.interceptor.OneWayProcessorInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractMultiplexDestination;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.jms.continuations.JMSContinuationProvider;
import org.apache.cxf.transport.jms.util.JMSListenerContainer;
import org.apache.cxf.transport.jms.util.JMSUtil;
import org.apache.cxf.transport.jms.util.MessageListenerContainer;
import org.apache.cxf.transport.jms.util.ResourceCloser;

public class JMSDestination extends AbstractMultiplexDestination implements MessageListener {

    private static final Logger LOG = LogUtils.getL7dLogger(JMSDestination.class);

    private JMSConfiguration jmsConfig;
    private Bus bus;
    
    @SuppressWarnings("unused")
    private EndpointInfo ei;
    private JMSListenerContainer jmsListener;
    private ThrottlingCounter suspendedContinuations;
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
        return new BackChannelConduit(inMessage, jmsConfig, jmsListener.getConnection());
    }

    /**
     * Initialize jmsTemplate and jmsListener from jms configuration data in jmsConfig {@inheritDoc}
     */
    public void activate() {
        getLogger().log(Level.FINE, "JMSDestination activate().... ");
        jmsConfig.ensureProperlyConfigured();

        jmsListener = createTargetDestinationListener();
        int restartLimit = jmsConfig.getMaxSuspendedContinuations() * jmsConfig.getReconnectPercentOfMax()
                           / 100;
        this.suspendedContinuations = new ThrottlingCounter(this.jmsListener, restartLimit,
                                                            jmsConfig.getMaxSuspendedContinuations());
    }
    
    
    private JMSListenerContainer createTargetDestinationListener() {
        Session session = null;
        try {
            Connection connection = JMSFactory.createConnection(jmsConfig);
            connection.start();
            session = connection.createSession(jmsConfig.isSessionTransacted(), Session.AUTO_ACKNOWLEDGE);
            Destination destination = jmsConfig.getTargetDestination(session);
            MessageListenerContainer container = new MessageListenerContainer(connection, destination, this);
            container.setMessageSelector(jmsConfig.getMessageSelector());
            Executor executor = JMSFactory.createExecutor(bus, "jms-destination");
            container.setExecutor(executor);
            container.start();
            return container;
        } catch (JMSException e) {
            throw JMSUtil.convertJmsException(e);
        } finally {
            ResourceCloser.close(session);
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
            getLogger().log(Level.FINE,
                            "JMS destination received message " + message + " on "
                                + jmsConfig.getTargetDestination());
            Message inMessage = JMSMessageUtils
                .asCXFMessage(message, JMSConstants.JMS_SERVER_REQUEST_HEADERS);
            SecurityContext securityContext = JMSMessageUtils.buildSecurityContext(message, jmsConfig);
            inMessage.put(SecurityContext.class, securityContext);
            inMessage.put(JMSConstants.JMS_SERVER_RESPONSE_HEADERS, new JMSMessageHeadersType());
            inMessage.put(JMSConstants.JMS_REQUEST_MESSAGE, message);
            ((MessageImpl)inMessage).setDestination(this);
            if (jmsConfig.getMaxSuspendedContinuations() != 0) {
                JMSContinuationProvider cp = new JMSContinuationProvider(bus, inMessage, incomingObserver,
                                                                         suspendedContinuations);
                inMessage.put(ContinuationProvider.class.getName(), cp);
            }

            origBus = BusFactory.getAndSetThreadDefaultBus(bus);

            // FIXME
            // JCATransactionalMessageListenerContainer.setMessageEndpoint(inMessage);

            // handle the incoming message
            incomingObserver.onMessage(inMessage);

            if (inMessage.getExchange() != null && inMessage.getExchange().getInMessage() != null) {
                inMessage = inMessage.getExchange().getInMessage();
            }

            // need to propagate any exceptions back so transactions can occur
            if (inMessage.getContent(Exception.class) != null) {
                Exception ex = inMessage.getContent(Exception.class);
                if (!(ex instanceof org.apache.cxf.interceptor.Fault)) {
                    if (ex.getCause() instanceof RuntimeException) {
                        throw (RuntimeException)ex.getCause();
                    } else {
                        throw new RuntimeException(ex);
                    }
                }
            }

        } catch (SuspendedInvocationException ex) {
            getLogger().log(Level.FINE, "Request message has been suspended");
        } catch (UnsupportedEncodingException ex) {
            getLogger().log(Level.WARNING, "can't get the right encoding information. " + ex);
        } catch (JMSException e) {
            throw JMSUtil.convertJmsException(e);
        } finally {
            if (origBus != bus) {
                BusFactory.setThreadDefaultBus(origBus);
            }
            if (origLoader != null) {
                origLoader.reset();
            }
        }
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
