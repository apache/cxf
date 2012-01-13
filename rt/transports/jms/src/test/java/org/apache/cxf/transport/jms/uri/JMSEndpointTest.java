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


import org.junit.Assert;
import org.junit.Test;

public class JMSEndpointTest extends Assert {

    @Test
    public void testBasicQueue() throws Exception {
        JMSEndpoint endpoint = resolveEndpoint("jms:queue:Foo.Bar");
        assertTrue(endpoint instanceof JMSQueueEndpoint);
        assertEquals(endpoint.getDestinationName(), "Foo.Bar");
        assertEquals(endpoint.getJmsVariant(), JMSURIConstants.QUEUE);
    }

    @Test
    public void testQueueParameters() throws Exception {
        JMSEndpoint endpoint = resolveEndpoint("jms:queue:Foo.Bar?foo=bar&foo2=bar2");
        assertTrue(endpoint instanceof JMSQueueEndpoint);
        assertEquals(endpoint.getDestinationName(), "Foo.Bar");
        assertEquals(endpoint.getJmsVariant(), JMSURIConstants.QUEUE);
        assertEquals(endpoint.getParameters().size(), 2);
        assertEquals(endpoint.getParameter("foo"), "bar");
        assertEquals(endpoint.getParameter("foo2"), "bar2");
    }

    @Test
    public void testBasicTopic() throws Exception {
        JMSEndpoint endpoint = resolveEndpoint("jms:topic:Foo.Bar");
        assertTrue(endpoint instanceof JMSTopicEndpoint);
        assertEquals(endpoint.getDestinationName(), "Foo.Bar");
        assertEquals(endpoint.getJmsVariant(), JMSURIConstants.TOPIC);
    }

    @Test
    public void testTopicParameters() throws Exception {
        JMSEndpoint endpoint = resolveEndpoint("jms:topic:Foo.Bar?foo=bar&foo2=bar2");
        assertTrue(endpoint instanceof JMSTopicEndpoint);
        assertEquals(endpoint.getParameters().size(), 2);
        assertEquals(endpoint.getParameter("foo"), "bar");
        assertEquals(endpoint.getParameter("foo2"), "bar2");
    }

    @Test
    public void testBasicJNDI() throws Exception {
        JMSEndpoint endpoint = resolveEndpoint("jms:jndi:Foo.Bar");
        assertTrue(endpoint instanceof JMSJNDIEndpoint);
        assertEquals(endpoint.getDestinationName(), "Foo.Bar");
        assertEquals(endpoint.getJmsVariant(), JMSURIConstants.JNDI);
    }

    @Test
    public void testJNDIParameters() throws Exception {
        JMSEndpoint endpoint = resolveEndpoint("jms:jndi:Foo.Bar?" + "jndiInitialContextFactory"
                                               + "=org.apache.activemq.jndi.ActiveMQInitialContextFactory"
                                               + "&jndiConnectionFactoryName=ConnectionFactory"
                                               + "&jndiURL=tcp://localhost:61616");
        assertTrue(endpoint instanceof JMSJNDIEndpoint);
        assertEquals(endpoint.getParameters().size(), 0);
        assertEquals(endpoint.getDestinationName(), "Foo.Bar");
        assertEquals(endpoint.getJndiInitialContextFactory(),
                     "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        assertEquals(endpoint.getJndiConnectionFactoryName(),
                     "ConnectionFactory");
        assertEquals(endpoint.getJndiURL(), "tcp://localhost:61616");
    }
    
    @Test
    public void testReplyToNameParameters() throws Exception {
        JMSEndpoint endpoint = resolveEndpoint("jms:queue:Foo.Bar?replyToName=FOO.Tar");
        assertTrue(endpoint instanceof JMSQueueEndpoint);
        assertEquals("Foo.Bar", endpoint.getDestinationName());
        assertNull(endpoint.getTopicReplyToName());
        assertEquals("FOO.Tar", endpoint.getReplyToName());
        try {
            resolveEndpoint("jms:queue:Foo.Bar?replyToName=FOO.Tar&topicReplyToName=FOO.Zar");
            fail("Expecting exception here");
        } catch (IllegalArgumentException ex) {
            // expect the exception
        }
        
        endpoint = resolveEndpoint("jms:queue:Foo.Bar?topicReplyToName=FOO.Zar");
        assertEquals("Foo.Bar", endpoint.getDestinationName());
        assertNull(endpoint.getReplyToName());
        assertEquals("FOO.Zar", endpoint.getTopicReplyToName());
    }
    
    @Test
    public void testJNDIWithAdditionalParameters() throws Exception {
        JMSEndpoint endpoint = resolveEndpoint("jms:jndi:Foo.Bar?" + "jndiInitialContextFactory"
                                               + "=org.apache.activemq.jndi.ActiveMQInitialContextFactory"
                                               + "&jndiConnectionFactoryName=ConnectionFactory"
                                               + "&jndiURL=tcp://localhost:61616"
                                               + "&jndi-com.sun.jndi.someParameter=someValue");
        assertTrue(endpoint instanceof JMSJNDIEndpoint);
        assertEquals(endpoint.getParameters().size(), 0);
        assertEquals(endpoint.getJndiInitialContextFactory(),
                     "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        assertEquals(endpoint.getJndiConnectionFactoryName(), "ConnectionFactory");
        assertEquals(endpoint.getJndiURL(), "tcp://localhost:61616");
        Map<String, String> addParas = endpoint.getJndiParameters();
        assertEquals(addParas.size(), 1);
        assertEquals(addParas.get("com.sun.jndi.someParameter"), "someValue");
    }

    @Test
    public void testSharedParameters() throws Exception {
        JMSEndpoint endpoint = resolveEndpoint("jms:queue:Foo.Bar?" + "deliveryMode=NON_PERSISTENT"
                                               + "&timeToLive=100" + "&priority=5" + "&replyToName=foo.bar2");
        assertTrue(endpoint instanceof JMSQueueEndpoint);
        assertEquals(endpoint.getParameters().size(), 0);
        assertEquals(endpoint.getDeliveryMode(),
                     JMSURIConstants.DELIVERYMODE_NON_PERSISTENT);
        assertEquals(endpoint.getTimeToLive(), 100);
        assertEquals(endpoint.getPriority(), 5);
        assertEquals(endpoint.getReplyToName(), "foo.bar2");
    }

    @Test
    public void testRequestUri() throws Exception {
        JMSEndpoint endpoint = resolveEndpoint("jms:jndi:Foo.Bar?" + "jndiInitialContextFactory"
                                               + "=org.apache.activemq.jndi.ActiveMQInitialContextFactory"
                                               + "&targetService=greetMe"
                                               + "&replyToName=replyQueue"
                                               + "&timeToLive=1000"
                                               + "&priority=3"
                                               + "&foo=bar"
                                               + "&foo2=bar2");
        assertTrue(endpoint instanceof JMSJNDIEndpoint);
        assertEquals(endpoint.getParameters().size(), 3);
        String requestUri = endpoint.getRequestURI();
        // Checking what's the request uri should have
        assertTrue(requestUri.startsWith("jms:jndi:Foo.Bar?"));
        assertTrue(requestUri.contains("foo=bar"));
        assertTrue(requestUri.contains("foo2=bar2"));
        // Cheching what's the request uri should not have
        assertFalse(requestUri.contains("jndiInitialContextFactory"));
        assertFalse(requestUri.contains("targetService"));
        assertFalse(requestUri.contains("replyToName"));
        assertFalse(requestUri.contains("priority=3"));
    }
    
    private JMSEndpoint resolveEndpoint(String uri) throws Exception {
        return JMSEndpointParser.createEndpoint(uri);
    }
}
