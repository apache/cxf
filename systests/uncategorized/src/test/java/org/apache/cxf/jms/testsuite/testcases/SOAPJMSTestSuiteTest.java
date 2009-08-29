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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import javax.jms.DeliveryMode;
import javax.xml.ws.BindingProvider;

import org.apache.cxf.jms.testsuite.services.Server;
import org.apache.cxf.jms.testsuite.util.JMSTestUtil;
import org.apache.cxf.jms_simple.JMSSimplePortType;
import org.apache.cxf.jms_simple.JMSSimpleService0001;
import org.apache.cxf.jms_simple.JMSSimpleService0003;
import org.apache.cxf.jms_simple.JMSSimpleService0005;
import org.apache.cxf.jms_simple.JMSSimpleService0006;
import org.apache.cxf.jms_simple.JMSSimpleService0008;
import org.apache.cxf.jms_simple.JMSSimpleService0009;
import org.apache.cxf.jms_simple.JMSSimpleService0010;
import org.apache.cxf.jms_simple.JMSSimpleService0011;
import org.apache.cxf.jms_simple.JMSSimpleService0012;
import org.apache.cxf.jms_simple.JMSSimpleService1001;
import org.apache.cxf.systest.jms.EmbeddedJMSBrokerLauncher;
import org.apache.cxf.testsuite.testcase.TestCaseType;
import org.apache.cxf.transport.jms.JMSConstants;
import org.apache.cxf.transport.jms.JMSMessageHeadersType;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 */
public class SOAPJMSTestSuiteTest extends AbstractSOAPJMSTestSuite {

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
        assertTrue("server did not launch correctly", launchServer(Server.class, false));
    }

    private void oneWayTest(TestCaseType testcase, JMSSimplePortType port) throws Exception {
        InvocationHandler handler = Proxy.getInvocationHandler(port);
        BindingProvider bp = (BindingProvider)handler;

        Map<String, Object> requestContext = bp.getRequestContext();
        JMSMessageHeadersType requestHeader = new JMSMessageHeadersType();
        requestContext.put(JMSConstants.JMS_CLIENT_REQUEST_HEADERS, requestHeader);
        Exception e = null;
        try {
            port.ping("test");
        } catch (Exception e1) {
            e = e1;
        }
        checkJMSProperties(testcase, requestHeader);
        if (e != null) {
            throw e;
        }
    }

    private void twoWayTest(TestCaseType testcase, final JMSSimplePortType port)
        throws Exception {
        JMSMessageHeadersType requestHeader = new JMSMessageHeadersType();
        twoWayTestWithRequestHeader(testcase, port, requestHeader);
    }

    private void twoWayTestWithRequestHeader(TestCaseType testcase, final JMSSimplePortType port,
                                             JMSMessageHeadersType requestHeader)
        throws Exception {
        InvocationHandler handler = Proxy.getInvocationHandler(port);
        BindingProvider bp = (BindingProvider)handler;

        Map<String, Object> requestContext = bp.getRequestContext();
        if (requestHeader == null) {
            requestHeader = new JMSMessageHeadersType();
        }
        requestContext.put(JMSConstants.JMS_CLIENT_REQUEST_HEADERS, requestHeader);
        Exception e = null;
        try {
            String response = port.echo("test");
            assertEquals(response, "test");
        } catch (Exception e1) {
            e = e1;
        }
        Map<String, Object> responseContext = bp.getResponseContext();
        JMSMessageHeadersType responseHeader = (JMSMessageHeadersType)responseContext
            .get(JMSConstants.JMS_CLIENT_RESPONSE_HEADERS);
        checkJMSProperties(testcase, requestHeader, responseHeader);
        if (e != null) {
            throw e;
        }
    }

    @Test
    public void test0001() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test0001");
        final JMSSimplePortType simplePort = getPort("JMSSimpleService0001", "SimplePort",
                                                     JMSSimpleService0001.class,
                                                     JMSSimplePortType.class);
        oneWayTest(testcase, simplePort);
    }

    @Test
    public void test0002() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test0002");
        final JMSSimplePortType simplePort = getPort("JMSSimpleService0001", "SimplePort",
                                                     JMSSimpleService0001.class,
                                                     JMSSimplePortType.class);
        twoWayTest(testcase, simplePort);
    }

    @Test
    public void test0003() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test0003");
        final JMSSimplePortType simplePort = getPort("JMSSimpleService0003", "SimplePort",
                                                     JMSSimpleService0003.class,
                                                     JMSSimplePortType.class);
        oneWayTest(testcase, simplePort);
    }

    @Test
    public void test0004() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test0004");
        final JMSSimplePortType simplePort = getPort("JMSSimpleService0003", "SimplePort",
                                                     JMSSimpleService0003.class,
                                                     JMSSimplePortType.class);
        twoWayTest(testcase, simplePort);
    }

    @Test
    public void test0005() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test0005");
        final JMSSimplePortType simplePort = getPort("JMSSimpleService0005", "SimplePort",
                                                     JMSSimpleService0005.class,
                                                     JMSSimplePortType.class);
        twoWayTest(testcase, simplePort);
    }

    @Test
    public void test0006() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test0006");
        final JMSSimplePortType simplePort = getPort("JMSSimpleService0006", "SimplePort",
                                                     JMSSimpleService0006.class,
                                                     JMSSimplePortType.class);
        twoWayTest(testcase, simplePort);
    }

    @Test
    public void test0008() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test0008");
        final JMSSimplePortType simplePort = getPort("JMSSimpleService0008", "SimplePort",
                                                     JMSSimpleService0008.class,
                                                     JMSSimplePortType.class);

        JMSMessageHeadersType requestHeader = new JMSMessageHeadersType();
        requestHeader.setJMSDeliveryMode(DeliveryMode.NON_PERSISTENT);
        requestHeader.setTimeToLive(14400000);
        requestHeader.setJMSPriority(8);
        requestHeader.setJMSReplyTo("dynamicQueues/replyqueue0008");

        twoWayTestWithRequestHeader(testcase, simplePort, requestHeader);
    }

    @Test
    public void test0009() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test0009");
        final JMSSimplePortType simplePort = getPort("JMSSimpleService0009", "SimplePort",
                                                     JMSSimpleService0009.class,
                                                     JMSSimplePortType.class);

        JMSMessageHeadersType requestHeader = new JMSMessageHeadersType();
        requestHeader.setJMSDeliveryMode(DeliveryMode.NON_PERSISTENT);
        requestHeader.setTimeToLive(10800000);
        requestHeader.setJMSPriority(3);
        requestHeader.setJMSReplyTo("dynamicQueues/replyqueue00093");

        twoWayTestWithRequestHeader(testcase, simplePort, requestHeader);
    }

    @Test
    public void test0010() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test0010");
        final JMSSimplePortType simplePort = getPort("JMSSimpleService0010", "SimplePort",
                                                     JMSSimpleService0010.class,
                                                     JMSSimplePortType.class);
        twoWayTest(testcase, simplePort);
    }

    @Test
    public void test0011() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test0011");
        final JMSSimplePortType simplePort = getPort("JMSSimpleService0011", "SimplePort",
                                                     JMSSimpleService0011.class,
                                                     JMSSimplePortType.class);
        twoWayTest(testcase, simplePort);
    }

    @Test
    public void test0012() throws Exception {
        // same to test0002
        TestCaseType testcase = JMSTestUtil.getTestCase("test0012");
        final JMSSimplePortType simplePort = getPort("JMSSimpleService0012", "SimplePort",
                                                     JMSSimpleService0012.class,
                                                     JMSSimplePortType.class);
        twoWayTest(testcase, simplePort);
    }

    @Test
    public void test1001() throws Exception {
        // same to test0002
        TestCaseType testcase = JMSTestUtil.getTestCase("test1001");
        final JMSSimplePortType simplePort = getPort("JMSSimpleService1001", "SimplePort",
                                                     JMSSimpleService1001.class,
                                                     JMSSimplePortType.class);

        JMSMessageHeadersType requestHeader = new JMSMessageHeadersType();
        requestHeader.setSOAPJMSBindingVersion("0.3");
        try {
            twoWayTestWithRequestHeader(testcase, simplePort, requestHeader);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Unrecognized BindingVersion"));
        }
    }
    
    @Test
    public void test1002() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test1002");
        
        twoWayTestWithCreateMessage(testcase);
    }
    
    @Test
    public void test1003() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test1003");
        
        twoWayTestWithCreateMessage(testcase);
    }
    
    @Test
    public void test1004() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test1004");
        
        twoWayTestWithCreateMessage(testcase);
    }
    
    @Test
    public void test1006() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test1006");
        
        twoWayTestWithCreateMessage(testcase);
    }
    
    @Test
    public void test1007() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test1007");
        
        twoWayTestWithCreateMessage(testcase);
    }
    
    @Test
    public void test1008() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test1008");
        
        twoWayTestWithCreateMessage(testcase);
    }
}
