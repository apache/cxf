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

package org.apache.cxf.transport.jms.uri;

import java.util.Map;

import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.jms.uri.JMSEndpoint.DeliveryModeType;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JMSEndpointTest {

    private static final String TEST_VALUE = "testValue";

    @Test
    public void testBasicQueue() throws Exception {
        JMSEndpoint endpoint = new JMSEndpoint("jms:queue:Foo.Bar?concurrentConsumers=21");
        assertEquals(JMSEndpoint.QUEUE, endpoint.getJmsVariant());
        assertEquals("Foo.Bar", endpoint.getDestinationName());
        assertEquals(JMSEndpoint.QUEUE, endpoint.getJmsVariant());
        assertEquals(21, endpoint.getConcurrentConsumers());
    }

    @Test
    public void testQueueParameters() throws Exception {
        JMSEndpoint endpoint = new JMSEndpoint("jms:queue:Foo.Bar?foo=bar&foo2=bar2&useConduitIdSelector=false");
        assertEquals(JMSEndpoint.QUEUE, endpoint.getJmsVariant());
        assertEquals(endpoint.getDestinationName(), "Foo.Bar");
        assertEquals(endpoint.getJmsVariant(), JMSEndpoint.QUEUE);
        assertFalse(endpoint.isUseConduitIdSelector());
        assertEquals(endpoint.getParameters().size(), 2);
        assertEquals(endpoint.getParameter("foo"), "bar");
        assertEquals(endpoint.getParameter("foo2"), "bar2");
    }

    @Test
    public void testBasicTopic() throws Exception {
        JMSEndpoint endpoint = new JMSEndpoint("jms:topic:Foo.Bar");
        assertEquals(JMSEndpoint.TOPIC, endpoint.getJmsVariant());
        assertEquals(endpoint.getDestinationName(), "Foo.Bar");
        assertEquals(endpoint.getJmsVariant(), JMSEndpoint.TOPIC);
    }

    @Test
    public void testTopicParameters() throws Exception {
        JMSEndpoint endpoint = new JMSEndpoint("jms:topic:Foo.Bar?foo=bar&foo2=bar2");
        assertEquals(JMSEndpoint.TOPIC, endpoint.getJmsVariant());
        assertEquals(endpoint.getParameters().size(), 2);
        assertEquals(endpoint.getParameter("foo"), "bar");
        assertEquals(endpoint.getParameter("foo2"), "bar2");
    }

    @Test
    public void testBasicJNDI() throws Exception {
        JMSEndpoint endpoint = new JMSEndpoint("jms:jndi:Foo.Bar");
        assertEquals(JMSEndpoint.JNDI, endpoint.getJmsVariant());
        assertEquals(endpoint.getDestinationName(), "Foo.Bar");
        assertEquals(endpoint.getJmsVariant(), JMSEndpoint.JNDI);
    }

    @Test
    public void testJNDIParameters() throws Exception {
        JMSEndpoint endpoint = new JMSEndpoint("jms:jndi:Foo.Bar?" + "jndiInitialContextFactory"
            + "=org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory"
            + "&jndiConnectionFactoryName=ConnectionFactory"
            + "&jndiURL=tcp://localhost:61616");
        assertEquals(JMSEndpoint.JNDI, endpoint.getJmsVariant());
        assertEquals(endpoint.getParameters().size(), 0);
        assertEquals(endpoint.getDestinationName(), "Foo.Bar");
        assertEquals(endpoint.getJndiInitialContextFactory(),
                     "org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory");
        assertEquals(endpoint.getJndiConnectionFactoryName(),
                     "ConnectionFactory");
        assertEquals(endpoint.getJndiURL(), "tcp://localhost:61616");
    }

    @Test
    public void testReplyToNameParameters() throws Exception {
        JMSEndpoint endpoint = new JMSEndpoint("jms:queue:Foo.Bar?replyToName=FOO.Tar");
        assertEquals(JMSEndpoint.QUEUE, endpoint.getJmsVariant());
        assertEquals("Foo.Bar", endpoint.getDestinationName());
        assertNull(endpoint.getTopicReplyToName());
        assertEquals("FOO.Tar", endpoint.getReplyToName());
        try {
            new JMSEndpoint("jms:queue:Foo.Bar?replyToName=FOO.Tar&topicReplyToName=FOO.Zar");
            fail("Expecting exception here");
        } catch (IllegalArgumentException ex) {
            // expect the exception
        }

        endpoint = new JMSEndpoint("jms:queue:Foo.Bar?topicReplyToName=FOO.Zar");
        assertEquals("Foo.Bar", endpoint.getDestinationName());
        assertNull(endpoint.getReplyToName());
        assertEquals("FOO.Zar", endpoint.getTopicReplyToName());
    }

    @Test
    public void testJNDIWithAdditionalParameters() throws Exception {
        JMSEndpoint endpoint = new JMSEndpoint("jms:jndi:Foo.Bar?" + "jndiInitialContextFactory"
            + "=org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory"
            + "&jndiConnectionFactoryName=ConnectionFactory"
            + "&jndiURL=tcp://localhost:61616"
            + "&jndi-com.sun.jndi.someParameter=someValue"
            + "&durableSubscriptionName=dur");
        assertEquals(JMSEndpoint.JNDI, endpoint.getJmsVariant());
        assertEquals(endpoint.getParameters().size(), 0);
        assertEquals("org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory",
                     endpoint.getJndiInitialContextFactory());
        assertEquals("ConnectionFactory", endpoint.getJndiConnectionFactoryName());
        assertEquals("tcp://localhost:61616", endpoint.getJndiURL());
        assertEquals("dur", endpoint.getDurableSubscriptionName());
        Map<String, String> addParas = endpoint.getJndiParameters();
        assertEquals(1, addParas.size());
        assertEquals("someValue", addParas.get("com.sun.jndi.someParameter"));
    }

    @Test
    public void testSharedParameters() throws Exception {
        JMSEndpoint endpoint = new JMSEndpoint("jms:queue:Foo.Bar?" + "deliveryMode=NON_PERSISTENT"
            + "&timeToLive=100" + "&priority=5" + "&replyToName=foo.bar2");
        assertEquals(JMSEndpoint.QUEUE, endpoint.getJmsVariant());
        assertEquals(0, endpoint.getParameters().size());
        assertEquals(DeliveryModeType.NON_PERSISTENT, endpoint.getDeliveryMode());
        assertEquals(100, endpoint.getTimeToLive());
        assertEquals(5, endpoint.getPriority());
        assertEquals("foo.bar2", endpoint.getReplyToName());
    }

    @Test
    public void testRequestUri() throws Exception {
        JMSEndpoint endpoint = new JMSEndpoint("jms:jndi:Foo.Bar"
            + "?jndiInitialContextFactory=org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory"
            + "&targetService=greetMe"
            + "&replyToName=replyQueue"
            + "&timeToLive=1000"
            + "&priority=3"
            + "&foo=bar"
            + "&foo2=bar2");
        assertEquals(JMSEndpoint.JNDI, endpoint.getJmsVariant());
        assertEquals(2, endpoint.getParameters().size());
        String requestUri = endpoint.getRequestURI();
        // Checking what's the request uri should have
        assertTrue(requestUri.startsWith("jms:jndi:Foo.Bar?"));
        assertTrue(requestUri.contains("foo=bar"));
        assertTrue(requestUri.contains("foo2=bar2"));
        // Checking what's the request uri should not have
        assertFalse(requestUri.contains("jndiInitialContextFactory"));
        assertFalse(requestUri.contains("targetService"));
        assertFalse(requestUri.contains("replyToName"));
        assertFalse(requestUri.contains("priority=3"));
    }

    @Test
    public void testRequestUriWithMessageType() throws Exception {
        JMSEndpoint endpoint = new JMSEndpoint("jms:queue:Foo.Bar?messageType=text");
        assertEquals(JMSEndpoint.QUEUE, endpoint.getJmsVariant());
        assertEquals("text", endpoint.getMessageType().value());

        endpoint = new JMSEndpoint("jms:queue:Foo.Bar");
        assertEquals(JMSEndpoint.QUEUE, endpoint.getJmsVariant());
        assertEquals("byte", endpoint.getMessageType().value());

        endpoint = new JMSEndpoint("jms:queue:Foo.Bar?messageType=binary");
        assertEquals(JMSEndpoint.QUEUE, endpoint.getJmsVariant());
        assertEquals("binary", endpoint.getMessageType().value());

    }

    @Test
    public void nonSoapJMS() throws Exception {
        JMSEndpoint endpoint = new JMSEndpoint("jms://");
        assertEquals(JMSEndpoint.QUEUE, endpoint.getJmsVariant());
    }

    @Test
    public void testTransactionManager() {
        JMSEndpoint endpoint = new JMSEndpoint("jms:queue:Foo.Bar?jndiTransactionManagerName=test");
        assertEquals("test", endpoint.getJndiTransactionManagerName());
    }

    @Test
    public void testJaxWsProps() throws Exception {
        EndpointInfo ei = new EndpointInfo();
        ei.setProperty(JMSEndpoint.JAXWS_PROPERTY_PREFIX + "durableSubscriptionName", TEST_VALUE);
        JMSEndpoint endpoint = new JMSEndpoint(ei, "jms:queue:Foo.Bar");
        assertEquals(endpoint.getDurableSubscriptionName(), TEST_VALUE);
    }

}