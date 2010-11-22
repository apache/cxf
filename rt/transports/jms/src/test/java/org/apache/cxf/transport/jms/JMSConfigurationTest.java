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

import javax.jms.ConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.jms.connection.SingleConnectionFactory;

public class JMSConfigurationTest extends Assert {

    @Test
    public void testDestroyAutoWrappedConnectionFactory() {
        // create an empty configuration
        JMSConfiguration jmsConfig = new JMSConfiguration();
        // create the connectionFactory to wrap
        ActiveMQConnectionFactory amqCf = new ActiveMQConnectionFactory();
        jmsConfig.setConnectionFactory(amqCf);
        // get the connectionFactory
        assertTrue("Should get the instance of ActiveMQConnectionFactory",
                   jmsConfig.getConnectionFactory() instanceof ActiveMQConnectionFactory);
        // get the wrapped connectionFactory
        ConnectionFactory wrappingCf = jmsConfig.getOrCreateWrappedConnectionFactory();
        assertTrue("Should get the instance of SingleConnectionFactory",
                   wrappingCf instanceof SingleConnectionFactory);
        SingleConnectionFactory scf = (SingleConnectionFactory)wrappingCf;
        assertSame("Should get the wrapped ActiveMQConnectionFactory",
                    amqCf, scf.getTargetConnectionFactory());
        // destroy the wrapping
        jmsConfig.destroyWrappedConnectionFactory();
        // get the wrapping cf
        assertNull("Should be null after destroy", jmsConfig.getWrappedConnectionFactory());
        // original connectionFactory should be unchanged
        assertSame("Should be the same with original connectionFactory",
                    amqCf, jmsConfig.getConnectionFactory());
    }

    @Test
    public void testDestroySuppliedConnectionFactory() {
        // create an empty configuration
        JMSConfiguration jmsConfig = new JMSConfiguration();
        // create the connectionFactory to wrap
        ActiveMQConnectionFactory amqCf = new ActiveMQConnectionFactory();
        // wrap into SingleConnectionFactory
        SingleConnectionFactory scf = new SingleConnectionFactory(amqCf);
        // set the connectionFactory to reuse
        jmsConfig.setConnectionFactory(scf);
        // get the connectionFactory
        assertTrue("Should get the instance of SingleConnectionFactory",
                   jmsConfig.getConnectionFactory() instanceof SingleConnectionFactory);
        // get the wrapped connectionFactory
        ConnectionFactory wrappingCf = jmsConfig.getOrCreateWrappedConnectionFactory();
        assertTrue("Should get the instance of SingleConnectionFactory",
                   wrappingCf instanceof SingleConnectionFactory);
        assertSame("Should not be wrapped again",
                    scf, wrappingCf);
        // destroy the wrapping
        jmsConfig.destroyWrappedConnectionFactory();
        // get the wrapping cf
        assertNotNull("Should be not null after destroy", jmsConfig.getWrappedConnectionFactory());
        // original connectionFactory should be unchanged
        assertSame("Should be the same with supplied connectionFactory",
                    scf, jmsConfig.getConnectionFactory());
    }
}
