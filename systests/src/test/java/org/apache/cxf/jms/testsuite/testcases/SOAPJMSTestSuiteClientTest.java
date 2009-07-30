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

package org.apache.cxf.jms.testsuite.testcases;

import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.Message;

import org.apache.cxf.jms.testsuite.util.JMSTestUtil;
import org.apache.cxf.jms_simple.JMSSimplePortType;
import org.apache.cxf.jms_simple.JMSSimpleService0001;
import org.apache.cxf.systest.jms.EmbeddedJMSBrokerLauncher;
import org.apache.cxf.testsuite.testcase.TestCaseType;
import org.apache.cxf.transport.jms.uri.JMSEndpoint;
import org.apache.cxf.transport.jms.uri.JMSEndpointParser;

import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.jms.core.JmsTemplate;

/**
 * 
 */
public class SOAPJMSTestSuiteClientTest extends AbstractSOAPJMSTestSuite {

    @BeforeClass
    public static void startServers() throws Exception {
        Map<String, String> props = new HashMap<String, String>();
        if (System.getProperty("activemq.store.dir") != null) {
            props.put("activemq.store.dir", System.getProperty("activemq.store.dir"));
        }
        props.put("java.util.logging.config.file", System
            .getProperty("java.util.logging.config.file"));

        assertTrue("server did not launch correctly", launchServer(EmbeddedJMSBrokerLauncher.class,
                                                                   props, null));
    }

    @Test
    public void test0001() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test0001");
        String address = testcase.getAddress().trim();
        JMSEndpoint endpoint = JMSEndpointParser.createEndpoint(address);
        String destinationName = endpoint.getDestinationName();

        JmsTemplate jmsTemplate = JMSTestUtil.getJmsTemplate(address);
        Destination dest = JMSTestUtil.getJmsDestination(jmsTemplate, destinationName, false);

        JMSSimplePortType simplePort = getPort("JMSSimpleService0001", "SimplePort",
                                               JMSSimpleService0001.class, JMSSimplePortType.class);
        simplePort.ping("test");

        Message message = jmsTemplate.receive(dest);
        try {
            checkJMSProperties(message, testcase.getRequestMessage(), true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void test0002() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test0002");
        String address = testcase.getAddress().trim();
        JMSEndpoint endpoint = JMSEndpointParser.createEndpoint(address);
        String destinationName = endpoint.getDestinationName();

        JmsTemplate jmsTemplate = JMSTestUtil.getJmsTemplate(address);
        Destination dest = JMSTestUtil.getJmsDestination(jmsTemplate, destinationName, false);

        final JMSSimplePortType simplePort = getPort("JMSSimpleService0001", "SimplePort",
                                                     JMSSimpleService0001.class,
                                                     JMSSimplePortType.class);
        Thread serviceThread = new Thread() {
            public void run() {
                simplePort.echo("test");
            }
        };
        serviceThread.start();

        Message message = jmsTemplate.receive(dest);
        checkJMSProperties(message, testcase.getRequestMessage(), false);

        serviceThread.interrupt();
    }
}
