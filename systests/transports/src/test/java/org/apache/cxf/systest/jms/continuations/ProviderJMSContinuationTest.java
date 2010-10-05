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
package org.apache.cxf.systest.jms.continuations;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.hello_world_jms.HelloWorldPortType;
import org.apache.cxf.hello_world_jms.HelloWorldService;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.EmbeddedJMSBrokerLauncher;

import org.junit.Before;
import org.junit.Test;

public class ProviderJMSContinuationTest extends AbstractBusClientServerTestBase {
    protected static boolean serversStarted;
    static final String JMS_PORT = EmbeddedJMSBrokerLauncher.PORT;
    static final String PORT = ProviderServer.PORT;


    @Before
    public void startServers() throws Exception {
        if (serversStarted) {
            return;
        }
        Map<String, String> props = new HashMap<String, String>();                
        if (System.getProperty("org.apache.activemq.default.directory.prefix") != null) {
            props.put("org.apache.activemq.default.directory.prefix",
                      System.getProperty("org.apache.activemq.default.directory.prefix"));
        }
        props.put("java.util.logging.config.file", 
                  System.getProperty("java.util.logging.config.file"));
        
        assertTrue("server did not launch correctly", 
                   launchServer(EmbeddedJMSBrokerLauncher.class, props, null));

        assertTrue("server did not launch correctly", 
                   launchServer(ProviderServer.class, false));
        serversStarted = true;
    }
    
    public URL getWSDLURL(String s) throws Exception {
        return getClass().getResource(s);
    }
    public QName getServiceName(QName q) {
        return q;
    }
    public QName getPortName(QName q) {
        return q;
    }
    
        
    @Test
    public void testProviderContinuation() throws Exception {
        try {
            QName serviceName = getServiceName(new QName("http://cxf.apache.org/hello_world_jms", 
                                 "HelloWorldService"));
            QName portName = getPortName(
                    new QName("http://cxf.apache.org/hello_world_jms", "HelloWorldPort"));
            URL wsdl = getWSDLURL("/org/apache/cxf/systest/jms/continuations/jms_test.wsdl");
            assertNotNull(wsdl);
            String wsdlString = wsdl.toString();
            EmbeddedJMSBrokerLauncher.updateWsdlExtensors(getBus(), wsdlString);

            HelloWorldService service = new HelloWorldService(wsdl, serviceName);
            assertNotNull(service);
            HelloWorldPortType greeter = service.getPort(portName, HelloWorldPortType.class);
            greeter.greetMe("ffang");
        } catch (Exception ex) {
            fail("shouldn't get exception here, which is caused by " 
                    + ex.getMessage());
        }     
    }
        
}

