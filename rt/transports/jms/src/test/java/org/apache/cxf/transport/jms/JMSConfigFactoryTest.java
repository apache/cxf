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

import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.transaction.xa.XAException;

import jakarta.transaction.TransactionManager;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.configuration.ConfiguredBeanLocator;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.jms.uri.JMSEndpoint;
import org.apache.cxf.transport.jms.uri.MyBeanLocator;

import org.junit.Assert;
import org.junit.Test;

public class JMSConfigFactoryTest extends AbstractJMSTester {

    @Test
    public void testJndiForbiddenProtocol() throws Exception {
        Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, "ldap://127.0.0.1:12345");
        // Allow following referrals (important for LDAP injection)
        env.put(Context.REFERRAL, "follow");
        
        JMSConfiguration jmsConfig = new JMSConfiguration();
        jmsConfig.setJndiEnvironment(env);
        jmsConfig.setConnectionFactoryName("objectName");
        
        try {
            jmsConfig.getConnectionFactory();
            Assert.fail("JNDI lookup should have failed");
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Unsafe protocol in JNDI URL"));
        }
    }

    @Test
    public void testUsernameAndPassword() throws Exception {
        EndpointInfo ei = setupServiceInfo("HelloWorldService", "HelloWorldPort");
        JMSConfiguration config = JMSConfigFactory.createFromEndpointInfo(bus, ei, null);
        Assert.assertEquals("User name does not match.", "testUser", config.getUserName());
        Assert.assertEquals("Password does not match.", "testPassword", config.getPassword());
    }

    @Test
    public void testTransactionManagerFromBus() throws XAException, NamingException {
        Bus bus = BusFactory.newInstance().createBus();
        ConfiguredBeanLocator cbl = bus.getExtension(ConfiguredBeanLocator.class);
        MyBeanLocator mybl = new MyBeanLocator(cbl);
        bus.setExtension(mybl, ConfiguredBeanLocator.class);

        TransactionManager tmExpected = com.arjuna.ats.jta.TransactionManager.transactionManager();
        mybl.register("tm", tmExpected);
        tmByName(bus, tmExpected);
        tmByClass(bus, tmExpected);
    }

    private void tmByName(Bus bus, TransactionManager tmExpected) {
        JMSEndpoint endpoint = new JMSEndpoint("jms:queue:Foo.Bar?jndiTransactionManagerName=tm");
        Assert.assertEquals("tm", endpoint.getJndiTransactionManagerName());
        JMSConfiguration jmsConfig = JMSConfigFactory.createFromEndpoint(bus, endpoint);
        TransactionManager tm = jmsConfig.getTransactionManager();
        Assert.assertEquals(tmExpected, tm);
    }

    private void tmByClass(Bus bus, TransactionManager tmExpected) {
        JMSEndpoint endpoint = new JMSEndpoint("jms:queue:Foo.Bar");
        JMSConfiguration jmsConfig = JMSConfigFactory.createFromEndpoint(bus, endpoint);
        TransactionManager tm = jmsConfig.getTransactionManager();
        Assert.assertEquals(tmExpected, tm);
    }


    @Test
    public void testTransactionManagerFromJndi() throws XAException, NamingException {
        JMSEndpoint endpoint =
            new JMSEndpoint("jms:queue:Foo.Bar?jndiTransactionManagerName=java:/comp/TransactionManager");
        Assert.assertEquals("java:/comp/TransactionManager", endpoint.getJndiTransactionManagerName());
        // TODO Check JNDI lookup
    }

    @Test
    public void testConcurrentConsumers() {
        JMSEndpoint endpoint = new JMSEndpoint("jms:queue:Foo.Bar?concurrentConsumers=4");
        JMSConfiguration jmsConfig = JMSConfigFactory.createFromEndpoint(bus, endpoint);
        Assert.assertEquals(4, jmsConfig.getConcurrentConsumers());

    }

    @Test
    public void testMessageSelectorIsSet() {
        EndpointInfo ei = setupServiceInfo("HelloWorldSelectorService", "HelloWorldPort");
        JMSConfiguration config = JMSConfigFactory.createFromEndpointInfo(bus, ei, null);
        Assert.assertEquals("customJMSAttribute=helloWorld", config.getMessageSelector());
    }
}
