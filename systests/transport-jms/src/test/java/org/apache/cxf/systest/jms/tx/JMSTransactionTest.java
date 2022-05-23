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
package org.apache.cxf.systest.jms.tx;

import java.util.Collections;
import java.util.Enumeration;

import jakarta.jms.Connection;
import jakarta.jms.JMSException;
import jakarta.jms.Queue;
import jakarta.jms.QueueBrowser;
import jakarta.jms.Session;
import jakarta.transaction.TransactionManager;
import org.apache.activemq.artemis.jms.client.ActiveMQXAConnectionFactory;
import org.apache.cxf.BusFactory;
import org.apache.cxf.configuration.ConfiguredBeanLocator;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.systest.jms.AbstractVmJMSTest;
import org.apache.cxf.transport.jms.ConnectionFactoryFeature;
import org.apache.cxf.transport.jms.spec.JMSSpecConstants;
import org.apache.cxf.transport.jms.util.JMSUtil;
import org.apache.hello_world_doc_lit.Greeter;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class JMSTransactionTest extends AbstractVmJMSTest {
    private static final String SERVICE_ADDRESS =
        "jms:queue:greeter.queue.tx?receivetTimeOut=5000&sessionTransacted=true";
    private static EndpointImpl endpoint;
    private static TransactionManager transactionManager;

    public static void startBusAndJMS(Class<?> testClass) {
        final String brokerURI =
            "vm://" + testClass.getName() + "?broker.persistent=false&broker.useJmx=false&jms.xaAckMode=1";
        startBusAndJMS(brokerURI);
        startBroker(brokerURI);
    }

    public static void startBusAndJMS(String brokerURI) {
        transactionManager = com.arjuna.ats.jta.TransactionManager.transactionManager();
        bus = BusFactory.getDefaultBus();
        registerTransactionManager();
        cf = new ActiveMQXAConnectionFactory(brokerURI);
        cff = new ConnectionFactoryFeature(cf);
    }

    /**
     * For real world scenarios create a bean for the transaction manager in blueprint or spring
     */
    private static void registerTransactionManager() {
        ConfiguredBeanLocator cbl = bus.getExtension(ConfiguredBeanLocator.class);
        MyBeanLocator mybl = new MyBeanLocator(cbl);
        mybl.register("tm", transactionManager);
        bus.setExtension(mybl, ConfiguredBeanLocator.class);
    }

    @BeforeClass
    public static void startServers() throws Exception {
        startBusAndJMS(JMSTransactionTest.class);
        //startBusAndJMS("tcp://localhost:61616");

        endpoint = new EndpointImpl(bus, new GreeterImplWithTransaction());
        endpoint.setAddress(SERVICE_ADDRESS);
        endpoint.setFeatures(Collections.singletonList(cff));
        endpoint.publish();
    }

    @AfterClass
    public static void clearProperty() {
        endpoint.stop();
    }

    /**
     * Request reply should not cause roll backs
     *
     * @throws Exception
     */
    @Test
    public void testNoTransactionRequestReply() throws Exception {
        Greeter greeter = markForClose(createGreeterProxy());

        greeter.greetMe("Good guy");
        try {
            greeter.greetMe("Bad guy");
            Assert.fail("Expecting exception here");
        } catch (Exception e) {
            // Fine
        }
    }

    @Test
    public void testTransactionOneWay() throws Exception {
        Connection conn = cf.createConnection();
        conn.start();
        Queue queue = JMSUtil.createQueue(conn, "ActiveMQ.DLQ");
        assertNumMessagesInQueue("DLQ should be empty", conn, queue, 0, 2000);

        Greeter greeter = markForClose(createGreeterProxy());
        // Should be processed normally
        greeter.greetMeOneWay(GreeterImplWithTransaction.GOOD_GUY);


        assertNumMessagesInQueue("DLQ should be empty", conn, queue, 0, 2000);

        // Should cause rollback, redelivery and in the end the message should go to the dead letter queue
        greeter.greetMeOneWay(GreeterImplWithTransaction.BAD_GUY);

        assertNumMessagesInQueue("Request should be put into DLQ", conn, queue, 1, 5000);
        conn.close();
    }

    private Greeter createGreeterProxy() throws Exception {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setBus(bus);
        factory.getFeatures().add(cff);
        factory.setTransportId(JMSSpecConstants.SOAP_JMS_SPECIFICATION_TRANSPORTID);
        factory.setServiceClass(Greeter.class);
        factory.setAddress(SERVICE_ADDRESS);
        return (Greeter)markForClose(factory.create());
    }

    private static void assertNumMessagesInQueue(String message, Connection connection, Queue queue,
                                          int expectedNum, int timeout) throws JMSException, InterruptedException {
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        QueueBrowser browser = session.createBrowser(queue);
        int actualNum = 0;
        for (long startTime = System.currentTimeMillis(); System.currentTimeMillis() - startTime < timeout;
            Thread.sleep(100L)) {
            actualNum = 0;
            for (Enumeration<?> messages = browser.getEnumeration(); messages.hasMoreElements(); actualNum++) {
                messages.nextElement();
            }
            if (actualNum == expectedNum) {
                break;
            }
            //System.out.println("Messages in queue " + queue.getQueueName() + ": " + actualNum
            //                   + ", expecting: " + expectedNum);
        }
        browser.close();
        session.close();
        Assert.assertEquals(message + " -> number of messages", expectedNum, actualNum);
    }

}
