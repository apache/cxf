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

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionConsumer;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.ConnectionMetaData;
import jakarta.jms.DeliveryMode;
import jakarta.jms.Destination;
import jakarta.jms.ExceptionListener;
import jakarta.jms.InvalidClientIDException;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.Queue;
import jakarta.jms.ServerSessionPool;
import jakarta.jms.Session;
import jakarta.jms.Topic;
import org.apache.activemq.artemis.jms.client.ActiveMQConnection;
import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.MultiplexDestination;
import org.apache.cxf.transport.jms.util.ResourceCloser;

import org.junit.Ignore;
import org.junit.Test;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class JMSDestinationTest extends AbstractJMSTester {
    private static class FaultyConnection implements Connection {
        private final Connection delegate;

        FaultyConnection(final Connection delegate) {
            this.delegate = delegate;
        }

        @Override
        public Session createSession(boolean transacted, int acknowledgeMode) throws JMSException {
            return delegate.createSession(transacted, acknowledgeMode);
        }

        @Override
        public String getClientID() throws JMSException {
            return delegate.getClientID();
        }

        @Override
        public void setClientID(String clientID) throws JMSException {
            delegate.setClientID(clientID);
        }

        @Override
        public ConnectionMetaData getMetaData() throws JMSException {
            return delegate.getMetaData();
        }

        @Override
        public ExceptionListener getExceptionListener() throws JMSException {
            return delegate.getExceptionListener();
        }

        @Override
        public void setExceptionListener(ExceptionListener listener) throws JMSException {
            delegate.setExceptionListener(listener);
        }

        @Override
        public void start() throws JMSException {
            delegate.start();
        }

        @Override
        public void stop() throws JMSException {
            delegate.stop();
        }

        @Override
        public void close() throws JMSException {
            delegate.close();
        }

        @Override
        public ConnectionConsumer createConnectionConsumer(Destination destination, String messageSelector,
                ServerSessionPool sessionPool, int maxMessages) throws JMSException {
            return delegate.createConnectionConsumer(destination, messageSelector, sessionPool, maxMessages);
        }

        @Override
        public ConnectionConsumer createDurableConnectionConsumer(Topic topic, String subscriptionName,
                String messageSelector, ServerSessionPool sessionPool, int maxMessages) throws JMSException {
            return delegate.createDurableConnectionConsumer(topic, subscriptionName, messageSelector,
                sessionPool, maxMessages);
        }

        @Override
        public Session createSession(int sessionMode) throws JMSException {
            return delegate.createSession(sessionMode);
        }

        @Override
        public Session createSession() throws JMSException {
            return delegate.createSession();
        }

        @Override
        public ConnectionConsumer createSharedConnectionConsumer(Topic topic, String subscriptionName,
                String messageSelector, ServerSessionPool sessionPool, int maxMessages) throws JMSException {
            return delegate.createSharedConnectionConsumer(topic, subscriptionName, 
                messageSelector, sessionPool, maxMessages);
        }

        @Override
        public ConnectionConsumer createSharedDurableConnectionConsumer(Topic topic, String subscriptionName,
                String messageSelector, ServerSessionPool sessionPool, int maxMessages) throws JMSException {
            return delegate.createSharedDurableConnectionConsumer(topic, subscriptionName, messageSelector, 
                sessionPool, maxMessages);
        }
    }

    private static final class FaultyConnectionFactory implements ConnectionFactory {
        private final AtomicInteger latch;
        private final ConnectionFactory delegate;
        private final Function<Connection, Connection> wrapper;
        private final AtomicInteger connectionsCreated = new AtomicInteger(0);

        private FaultyConnectionFactory(ConnectionFactory delegate, int faults) {
            this(delegate, FaultyConnection::new, faults);
        }

        private FaultyConnectionFactory(ConnectionFactory delegate,
                Function<Connection, Connection> wrapper, int faults) {
            this.delegate = delegate;
            this.wrapper = wrapper;
            this.latch = new AtomicInteger(faults);
        }

        @Override
        public Connection createConnection() throws JMSException {
            if (latch.getAndDecrement() <= 0) {
                connectionsCreated.incrementAndGet();
                return wrapper.apply(delegate.createConnection());
            } else {
                throw new JMSException("createConnection() failed (simulated)");
            }
        }

        @Override
        public Connection createConnection(String userName, String password) throws JMSException {
            if (latch.decrementAndGet() <= 0) {
                return wrapper.apply(delegate.createConnection(userName, password));
            } else {
                throw new JMSException("createConnection(userName, password) failed (simulated)");
            }
        }

        @Override
        public JMSContext createContext() {
            return delegate.createContext();
        }

        @Override
        public JMSContext createContext(String userName, String password) {
            return delegate.createContext(userName, password);
        }

        @Override
        public JMSContext createContext(String userName, String password, int sessionMode) {
            return delegate.createContext(userName, password, sessionMode);
        }

        @Override
        public JMSContext createContext(int sessionMode) {
            return delegate.createContext(sessionMode);
        }
    }

    @Test
    public void testGetConfigurationFromWSDL() throws Exception {
        EndpointInfo ei = setupServiceInfo("HelloWorldQueueBinMsgService", "HelloWorldQueueBinMsgPort");
        JMSDestination destination = setupJMSDestination(ei);
        assertEquals("Can't get the right AddressPolicy's Destination",
                     "test.jmstransport.binary",
                     destination.getJmsConfig().getTargetDestination());
        destination.shutdown();
    }

    @Test
    public void testDurableSubscriber() throws Exception {
        EndpointInfo ei = setupServiceInfo("HelloWorldPubSubService", "HelloWorldPubSubPort");
        JMSConduit conduit = setupJMSConduitWithObserver(ei);
        Message outMessage = createMessage();
        JMSDestination destination = setupJMSDestination(ei);
        destination.setMessageObserver(createMessageObserver());
        // The JMSBroker (ActiveMQ 5.x) need to take some time to setup the DurableSubscriber
        Thread.sleep(500L);
        sendOneWayMessage(conduit, outMessage);
        Message destMessage = waitForReceiveDestMessage();

        assertNotNull("The destiantion should have got the message ", destMessage);
        verifyReceivedMessage(destMessage);
        verifyHeaders(destMessage, outMessage);
        conduit.close();
        destination.shutdown();
    }

    @Test(expected = InvalidClientIDException.class)
    public void testDurableInvalidClientId() throws Throwable {
        Connection con = cf1.createConnection();
        JMSDestination destination = null;
        try { // NOPMD - UseTryWithResources
            con.setClientID("testClient");
            con.start();
            EndpointInfo ei = setupServiceInfo("HelloWorldPubSubService", "HelloWorldPubSubPort");
            JMSConfiguration jmsConfig = JMSConfigFactory.createFromEndpointInfo(bus, ei, null);
            jmsConfig.setDurableSubscriptionClientId("testClient");
            jmsConfig.setDurableSubscriptionName("testsub");
            jmsConfig.setConnectionFactory(cf);
            destination = new JMSDestination(bus, ei, jmsConfig);
            destination.setMessageObserver(createMessageObserver());
        } catch (RuntimeException e) {
            throw e.getCause();
        } finally {
            ResourceCloser.close(con);
            destination.shutdown();
        }
    }

    @Test
    public void testOneWayDestination() throws Exception {
        EndpointInfo ei = setupServiceInfo("HWStaticReplyQBinMsgService", "HWStaticReplyQBinMsgPort");
        JMSDestination destination = setupJMSDestination(ei);
        destination.setMessageObserver(createMessageObserver());

        JMSConduit conduit = setupJMSConduitWithObserver(ei);
        Message outMessage = createMessage();

        sendOneWayMessage(conduit, outMessage);
        // wait for the message to be get from the destination
        Message destMessage = waitForReceiveDestMessage();
        // just verify the Destination inMessage
        assertNotNull("The destiantion should have got the message ", destMessage);
        verifyReceivedMessage(destMessage);
        verifyHeaders(destMessage, outMessage);
        conduit.close();
        destination.shutdown();
    }

    private static void setupMessageHeader(Message outMessage, String correlationId, String replyTo) {
        JMSMessageHeadersType header = new JMSMessageHeadersType();
        header.setJMSCorrelationID(correlationId);
        header.setJMSDeliveryMode(DeliveryMode.PERSISTENT);
        header.setJMSPriority(1);
        header.setTimeToLive(1000L);
        header.setJMSReplyTo(replyTo);
        outMessage.put(JMSConstants.JMS_CLIENT_REQUEST_HEADERS, header);
        outMessage.put(Message.ENCODING, "US-ASCII");
    }

    private void verifyRequestResponseHeaders(Message msgIn, Message msgOut) {
        JMSMessageHeadersType outHeader = (JMSMessageHeadersType)msgOut
            .get(JMSConstants.JMS_CLIENT_REQUEST_HEADERS);
        String inEncoding = (String) msgIn.get(Message.ENCODING);
        String outEncoding = (String) msgOut.get(Message.ENCODING);

        assertEquals("The message encoding should be equal", inEncoding, outEncoding);

        JMSMessageHeadersType inHeader = (JMSMessageHeadersType)msgIn
            .get(JMSConstants.JMS_CLIENT_RESPONSE_HEADERS);

        verifyJmsHeaderEquality(outHeader, inHeader);
    }

    @Test
    public void testRoundTripDestination() throws Exception {
        Message msg = testRoundTripDestination(true);
        SecurityContext securityContext = msg.get(SecurityContext.class);

        assertNotNull("SecurityContext should be set in message received by JMSDestination", securityContext);
        assertEquals("Principal in SecurityContext should be", "testUser",
                securityContext.getUserPrincipal().getName());
    }

    @Test
    public void testRoundTripDestinationDoNotCreateSecurityContext() throws Exception {
        Message msg = testRoundTripDestination(false);
        SecurityContext securityContext = msg.get(SecurityContext.class);
        assertNull("SecurityContext should not be set in message received by JMSDestination", securityContext);
    }

    private Message testRoundTripDestination(boolean createSecurityContext) throws Exception {
        EndpointInfo ei = setupServiceInfo("HelloWorldService", "HelloWorldPort");
        JMSConduit conduit = setupJMSConduitWithObserver(ei);
        conduit.getJmsConfig().setCreateSecurityContext(createSecurityContext);

        final Message outMessage = createMessage();
        final JMSDestination destination = setupJMSDestination(ei);


        // set up MessageObserver for handling the conduit message
        MessageObserver observer = new MessageObserver() {
            public void onMessage(Message m) {
                Exchange exchange = new ExchangeImpl();
                exchange.setInMessage(m);
                m.setExchange(exchange);
                verifyReceivedMessage(m);
                verifyHeaders(m, outMessage);
                // setup the message for
                Conduit backConduit;
                try {
                    backConduit = destination.getBackChannel(m);
                    // wait for the message to be got from the conduit
                    Message replyMessage = new MessageImpl();
                    sendOneWayMessage(backConduit, replyMessage);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        destination.setMessageObserver(observer);
        sendMessageSync(conduit, outMessage);
        // wait for the message to be got from the destination,
        // create the thread to handler the Destination incoming message

        verifyReceivedMessage(waitForReceiveInMessage());
        // wait for a while for the jms session recycling

        // Send a second message to check for an issue
        // Where the session was closed the second time
        sendMessageSync(conduit, outMessage);
        Message inMessage = waitForReceiveInMessage();
        verifyReceivedMessage(inMessage);

        // wait for a while for the jms session recycling
//        Thread.sleep(1000L);
        conduit.close();
        destination.shutdown();

        return inMessage;
    }

    @Test
    public void testProperty() throws Exception {
        EndpointInfo ei = setupServiceInfo("HelloWorldService", "HelloWorldPort");
        final String customPropertyName = "THIS_PROPERTY_WILL_NOT_BE_AUTO_COPIED";

        // set up the conduit send to be true
        JMSConduit conduit = setupJMSConduitWithObserver(ei);
        final Message outMessage = createMessage();

        JMSMessageHeadersType headers = (JMSMessageHeadersType)outMessage
            .get(JMSConstants.JMS_CLIENT_REQUEST_HEADERS);
        headers.putProperty(customPropertyName, customPropertyName);

        final JMSDestination destination = setupJMSDestination(ei);

        // set up MessageObserver for handling the conduit message
        MessageObserver observer = new MessageObserver() {
            public void onMessage(Message m) {
                Exchange exchange = new ExchangeImpl();
                exchange.setInMessage(m);
                m.setExchange(exchange);
                verifyReceivedMessage(m);
                verifyHeaders(m, outMessage);
                // setup the message for
                Conduit backConduit;
                try {
                    backConduit = destination.getBackChannel(m);
                    // wait for the message to be got from the conduit
                    Message replyMessage = new MessageImpl();
                    // copy the message encoding
                    replyMessage.put(Message.ENCODING, m.get(Message.ENCODING));
                    sendOneWayMessage(backConduit, replyMessage);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        destination.setMessageObserver(observer);
        sendMessageSync(conduit, outMessage);
        // wait for the message to be got from the destination,
        // create the thread to handler the Destination incoming message

        Message inMessage = waitForReceiveInMessage();
        verifyReceivedMessage(inMessage);

        verifyRequestResponseHeaders(inMessage, outMessage);

        JMSMessageHeadersType inHeader = (JMSMessageHeadersType)inMessage
            .get(JMSConstants.JMS_CLIENT_RESPONSE_HEADERS);
        assertNotNull("The inHeader should not be null", inHeader);
        // TODO we need to check the SOAP JMS transport properties here

        // wait for a while for the jms session recycling
//        Thread.sleep(1000L);
        conduit.close();
        destination.shutdown();
    }
    
    @Test
    public void testTemporaryQueueDeletionUponReset() throws Exception {
        EndpointInfo ei = setupServiceInfo("HelloWorldService", "HelloWorldPort");

        // set up the conduit send to be true
        JMSConduit conduit = setupJMSConduitWithObserver(ei);
        assertNull(conduit.getJmsConfig().getReplyDestination());

        // Store the connection so we could check temporary queues
        final AtomicReference<ActiveMQConnection> connectionHolder = new AtomicReference<>();
        final Message outMessage = createMessage();
        
        // Capture the DestinationSource instance associated with the connection
        final JMSDestination destination = setupJMSDestination(ei, c -> new ConnectionFactory() {
            @Override
            public Connection createConnection() throws JMSException {
                final Connection connection = c.createConnection();
                connectionHolder.set((ActiveMQConnection)connection);
                return connection;
            }

            @Override
            public Connection createConnection(String userName, String password) throws JMSException {
                final Connection connection = c.createConnection(userName, password);
                connectionHolder.set((ActiveMQConnection)connection);
                return connection;
            }

            @Override
            public JMSContext createContext() {
                return c.createContext();
            }

            @Override
            public JMSContext createContext(String userName, String password) {
                return c.createContext(userName, password);
            }

            @Override
            public JMSContext createContext(String userName, String password, int sessionMode) {
                return c.createContext(userName, password, sessionMode);
            }

            @Override
            public JMSContext createContext(int sessionMode) {
                return c.createContext(sessionMode);
            }
        });

        // set up MessageObserver for handling the conduit message
        final MessageObserver observer = new MessageObserver() {
            public void onMessage(Message m) {
                final Exchange exchange = new ExchangeImpl();
                exchange.setInMessage(m);
                m.setExchange(exchange);

                try {
                    final Conduit backConduit = destination.getBackChannel(m);
                    sendOneWayMessage(backConduit, new MessageImpl());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        
        destination.setMessageObserver(observer);
        sendMessageSync(conduit, outMessage);
        // wait for the message to be got from the destination,
        // create the thread to handler the Destination incoming message

        Message inMessage = waitForReceiveInMessage();
        verifyReceivedMessage(inMessage);

        final ActiveMQConnection connection = connectionHolder.get();
        assertThat(ReflectionUtil.accessDeclaredField("tempQueues", ActiveMQConnection.class,
            connection, Set.class).size(), equalTo(1));
        
        // Force manual temporary queue deletion by resetting the reply destination
        conduit.getJmsConfig().resetCachedReplyDestination();
        // The queue deletion events (as well as others) are propagated asynchronously
        await()
            .atMost(1, TimeUnit.SECONDS)
            .untilAsserted(() -> assertThat(ReflectionUtil.accessDeclaredField("tempQueues", ActiveMQConnection.class, 
                connection, Set.class).size(), equalTo(0)));
        
        conduit.close();
        destination.shutdown();
    }

    @Test
    public void testIsMultiplexCapable() throws Exception {
        EndpointInfo ei = setupServiceInfo("HelloWorldService", "HelloWorldPort");
        final JMSDestination destination = setupJMSDestination(ei);
        destination.setMessageObserver(createMessageObserver());
        assertTrue("is multiplex", destination instanceof MultiplexDestination);
        destination.shutdown();
    }

    @Test
    public void testSecurityContext() throws Exception {
        SecurityContext ctx = testSecurityContext(true);
        assertNotNull("SecurityContext should be set in message received by JMSDestination", ctx);
        assertEquals("Principal in SecurityContext should be", "testUser",
                ctx.getUserPrincipal().getName());
    }

    @Test
    public void testDoNotCreateSecurityContext() throws Exception {
        SecurityContext ctx = testSecurityContext(false);
        assertNull("SecurityContext should not be set in message received by JMSDestination", ctx);
    }

    private SecurityContext testSecurityContext(boolean createSecurityContext) throws Exception {
        EndpointInfo ei = setupServiceInfo("HelloWorldService", "HelloWorldPort");
        final JMSDestination destination = setupJMSDestination(ei);
        destination.getJmsConfig().setCreateSecurityContext(createSecurityContext);
        destination.setMessageObserver(createMessageObserver());
        // set up the conduit send to be true
        JMSConduit conduit = setupJMSConduitWithObserver(ei);
        final Message outMessage = createMessage();
        sendOneWayMessage(conduit, outMessage);
        Message destMessage = waitForReceiveDestMessage();
        SecurityContext securityContext = destMessage.get(SecurityContext.class);

        conduit.close();
        destination.shutdown();

        return securityContext;
    }


    @Test
    @Ignore
    public void testOneWayReplyToSetUnset() throws Exception {
        /* 1. Test that replyTo destination set in WSDL is NOT used
         * in spec compliant mode */

        EndpointInfo ei = setupServiceInfo(
                         "HWStaticReplyQBinMsgService", "HWStaticReplyQBinMsgPort");
        JMSConduit conduit = setupJMSConduitWithObserver(ei);
        Message outMessage = createMessage();
        JMSDestination destination = setupJMSDestination(ei);
        destination.setMessageObserver(createMessageObserver());
        sendOneWayMessage(conduit, outMessage);
        Message destMessage = waitForReceiveDestMessage();
        // just verify the Destination inMessage
        assertNotNull("The destination should have got the message ", destMessage);
        verifyReplyToNotSet(destMessage);

        /* 2. Test that replyTo destination set in WSDL IS used
         * in spec non-compliant mode */

        sendOneWayMessage(conduit, outMessage);
        destMessage = waitForReceiveDestMessage();
        assertNotNull("The destination should have got the message ", destMessage);
        String exName = getQueueName(conduit.getJmsConfig().getReplyDestination());
        verifyReplyToSet(destMessage, Queue.class, exName);

        /* 3. Test that replyTo destination provided via invocation context
         * overrides the value set in WSDL and IS used in spec non-compliant mode */

        String contextReplyTo = conduit.getJmsConfig().getReplyDestination() + ".context";
        exName += ".context";
        setupMessageHeader(outMessage, "cidValue", contextReplyTo);
        sendOneWayMessage(conduit, outMessage);
        destMessage = waitForReceiveDestMessage();
        assertNotNull("The destiantion should have got the message ", destMessage);
        verifyReplyToSet(destMessage, Queue.class, exName);

        /* 4. Test that replyTo destination provided via invocation context
         * and the value set in WSDL are NOT used in spec non-compliant mode
         * when JMSConstants.JMS_SET_REPLY_TO == false */

        setupMessageHeader(outMessage, null, null);
        outMessage.put(JMSConstants.JMS_SET_REPLY_TO, Boolean.FALSE);
        sendOneWayMessage(conduit, outMessage);
        destMessage = waitForReceiveDestMessage();
        assertNotNull("The destiantion should have got the message ", destMessage);
        verifyReplyToNotSet(destMessage);

        /* 5. Test that replyTo destination set in WSDL IS used in spec non-compliant
         * mode when JMSConstants.JMS_SET_REPLY_TO == true */

        setupMessageHeader(outMessage, null, null);
        outMessage.put(JMSConstants.JMS_SET_REPLY_TO, Boolean.TRUE);
        sendOneWayMessage(conduit, outMessage);
        destMessage = waitForReceiveDestMessage();
        assertNotNull("The destiantion should have got the message ", destMessage);
        exName = getQueueName(conduit.getJmsConfig().getReplyDestination());
        verifyReplyToSet(destMessage, Queue.class, exName);

        conduit.close();
        destination.shutdown();
    }

    @Test
    public void testMessageObserverExceptionHandling() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        EndpointInfo ei = setupServiceInfo("HelloWorldPubSubService", "HelloWorldPubSubPort");
        JMSConduit conduit = setupJMSConduitWithObserver(ei);

        JMSDestination destination = setupJMSDestination(ei);
        destination.setMessageObserver(new MessageObserver() {
            @Override
            public void onMessage(Message message) {
                try {
                    throw new RuntimeException("Error!");
                } finally {
                    latch.countDown();
                }
            }
        });

        final Message outMessage = createMessage();
        Thread.sleep(500L);

        sendOneWayMessage(conduit, outMessage);
        latch.await(5, TimeUnit.SECONDS);

        conduit.close();
        destination.shutdown();
    }

    @Test
    public void testConnectionFactoryExceptionHandling() throws Exception {
        EndpointInfo ei = setupServiceInfo("HelloWorldPubSubService", "HelloWorldPubSubPort");
        final Function<ConnectionFactory, ConnectionFactory> wrapper =
            new Function<ConnectionFactory, ConnectionFactory>() {
                @Override
                public ConnectionFactory apply(ConnectionFactory cf) {
                    return new FaultyConnectionFactory(cf, 3);
                }
            };
        JMSConduit conduit = setupJMSConduitWithObserver(ei);
        JMSDestination destination = setupJMSDestination(ei, wrapper);
        destination.getJmsConfig().setRetryInterval(1000);
        destination.setMessageObserver(createMessageObserver());

        final Message outMessage = createMessage();
        Thread.sleep(4000L);

        sendOneWayMessage(conduit, outMessage);

        // wait for the message to be got from the destination,
        // create the thread to handler the Destination incoming message
        Message inMessage = waitForReceiveDestMessage();
        verifyReceivedMessage(inMessage);

        conduit.close();
        destination.shutdown();
    }

    @Test
    public void testBrokerExceptionHandling() throws Exception {
        EndpointInfo ei = setupServiceInfo("HelloWorldPubSubService", "HelloWorldPubSubPort");
        JMSConduit conduit = setupJMSConduitWithObserver(ei);
        JMSDestination destination = setupJMSDestination(ei);
        destination.getJmsConfig().setRetryInterval(1000);
        destination.setMessageObserver(createMessageObserver());

        Thread.sleep(500L);
        broker.stop();

        broker.start();
        Thread.sleep(2000L);

        final Message outMessage = createMessage();
        sendOneWayMessage(conduit, outMessage);

        // wait for the message to be got from the destination,
        // create the thread to handler the Destination incoming message
        Message inMessage = waitForReceiveDestMessage();
        verifyReceivedMessage(inMessage);

        conduit.close();
        destination.shutdown();
    }

    @SuppressWarnings("unused")
    @Test
    public void testSessionsExceptionHandling() throws Exception {
        final EndpointInfo ei = setupServiceInfo("HelloWorldPubSubService", "HelloWorldPubSubPort");
        final AtomicInteger sessionsToFail = new AtomicInteger(5);

        final Function<Connection, Connection> connection = c -> new FaultyConnection(c) {
            @Override
            public Session createSession(boolean transacted, int acknowledgeMode) throws JMSException {
                // Fail five times, starting with on successful call
                final int value = sessionsToFail.getAndDecrement();
                if (value >= 0 && value < 5) {
                    throw new JMSException("createSession() failed (simulated)");
                } else {
                    return super.createSession(transacted, acknowledgeMode);
                }
            }
        };

        final FaultyConnectionFactory faultyConnectionFactory = new FaultyConnectionFactory(cf, connection, 0);
        final Function<ConnectionFactory, ConnectionFactory> wrapper =
            new Function<ConnectionFactory, ConnectionFactory>() {
                @Override
                public ConnectionFactory apply(ConnectionFactory cf) {
                    return faultyConnectionFactory;
                }
            };
        JMSConduit conduit = setupJMSConduitWithObserver(ei);
        JMSConfiguration jmsConfig = JMSConfigFactory.createFromEndpointInfo(bus, ei, null);
        jmsConfig.setConnectionFactory(wrapper.apply(cf));
        jmsConfig.setRetryInterval(1000);
        jmsConfig.setConcurrentConsumers(10);
        JMSDestination destination = new JMSDestination(bus, ei, jmsConfig);
        destination.setMessageObserver(createMessageObserver());

        final Message outMessage = createMessage();
        Thread.sleep(4000L);

        sendOneWayMessage(conduit, outMessage);

        // wait for the message to be got from the destination,
        // create the thread to handler the Destination incoming message
        Message inMessage = waitForReceiveDestMessage();
        verifyReceivedMessage(inMessage);

        conduit.close();
        destination.shutdown();
        
        assertEquals("Only two createConnection() calls allowed because restartConnection() should be "
            + "called only once.", 2, faultyConnectionFactory.connectionsCreated.get());
    }


    private String getQueueName(String exName) {
        if (exName == null) {
            return null;
        }
        return (exName.indexOf('/') != -1 && exName.indexOf('/') < exName.length())
            ? exName.substring(exName.indexOf('/') + 1) : exName;
    }


    protected void verifyReplyToNotSet(Message cxfMsg) {
        jakarta.jms.Message jmsMsg =
            jakarta.jms.Message.class.cast(cxfMsg.get(JMSConstants.JMS_REQUEST_MESSAGE));
        assertNotNull("JMS Messsage must be null", jmsMsg);
    }

    private String getDestinationName(Destination dest) throws JMSException {
        if (dest instanceof Queue) {
            return ((Queue)dest).getQueueName();
        }
        return ((Topic)dest).getTopicName();
    }

    protected void verifyReplyToSet(Message cxfMsg,
                                    Class<? extends Destination> type,
                                    String expectedName) throws Exception {
        jakarta.jms.Message jmsMsg =
            jakarta.jms.Message.class.cast(cxfMsg.get(JMSConstants.JMS_REQUEST_MESSAGE));
        assertNotNull("JMS Messsage must not be null", jmsMsg);
        assertNotNull("JMS Messsage's replyTo must not be null", jmsMsg.getJMSReplyTo());
        assertTrue("JMS Messsage's replyTo type must be of type " + type.getName(),
                   type.isAssignableFrom(jmsMsg.getJMSReplyTo().getClass()));
        String receivedName = getDestinationName(jmsMsg.getJMSReplyTo());
        assertTrue("JMS Messsage's replyTo must be named " + expectedName + " but was " + receivedName,
                   expectedName == receivedName || receivedName.equals(expectedName));
    }
}