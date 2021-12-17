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

package org.apache.cxf.systest.soap;

import jakarta.xml.ws.BindingProvider;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapBindingConfiguration;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.hello_world_soap_action.Greeter;
import org.apache.hello_world_soap_action.RPCGreeter;
import org.apache.hello_world_soap_action.WrappedGreeter;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SoapActionTest {
    static final String PORT1 = TestUtil.getPortNumber(SoapActionTest.class, 1);
    static final String PORT2 = TestUtil.getPortNumber(SoapActionTest.class, 2);
    static final String PORT3 = TestUtil.getPortNumber(SoapActionTest.class, 3);
    static final String PORT4 = TestUtil.getPortNumber(SoapActionTest.class, 4);
    static final String PORT5 = TestUtil.getPortNumber(SoapActionTest.class, 5);
    static final String PORT6 = TestUtil.getPortNumber(SoapActionTest.class, 6);
    static final String PORT7 = TestUtil.getPortNumber(SoapActionTest.class, 7);

    static Bus bus;
    static String add11 = "http://localhost:" + PORT1 + "/test11";
    static String add12 = "http://localhost:" + PORT2 + "/test12";
    static String add13 = "http://localhost:" + PORT3 + "/testWrapped";
    static String add14 = "http://localhost:" + PORT4 + "/testWrapped12";
    static String add15 = "http://localhost:" + PORT5 + "/testRPCLit";
    static String add16 = "http://localhost:" + PORT6 + "/testRPCEncoded";
    static String add17 = "http://localhost:" + PORT7 + "/testWrappedEncoded";

    @BeforeClass
    public static void createServers() throws Exception {
        bus = BusFactory.getDefaultBus();
        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setServiceBean(new SoapActionGreeterImpl());
        sf.setAddress(add11);
        sf.setBus(bus);
        sf.create();

        sf = new JaxWsServerFactoryBean();
        sf.setServiceBean(new SoapActionGreeterImpl());
        sf.setAddress(add12);
        sf.setBus(bus);
        SoapBindingConfiguration config = new SoapBindingConfiguration();
        config.setVersion(Soap12.getInstance());
        sf.setBindingConfig(config);
        sf.create();

        sf = new JaxWsServerFactoryBean();
        sf.setServiceBean(new WrappedSoapActionGreeterImpl());
        sf.setAddress(add13);
        sf.setBus(bus);
        sf.create();

        sf = new JaxWsServerFactoryBean();
        sf.setServiceBean(new WrappedSoapActionGreeterImpl());
        sf.setAddress(add14);
        sf.setBus(bus);
        config.setVersion(Soap12.getInstance());
        sf.setBindingConfig(config);
        sf.create();

        sf = new JaxWsServerFactoryBean();
        sf.setServiceBean(new RPCLitSoapActionGreeterImpl());
        sf.setAddress(add15);
        sf.setBus(bus);
        sf.create();

        sf = new JaxWsServerFactoryBean();
        sf.setServiceBean(new RPCEncodedSoapActionGreeterImpl());
        sf.setAddress(add16);
        sf.setBus(bus);
        sf.create();

        sf = new JaxWsServerFactoryBean();
        sf.setServiceBean(new WrappedEncodedSoapActionGreeterImpl());
        sf.setAddress(add17);
        sf.setBus(bus);
        sf.create();
    }

    @AfterClass
    public static void shutdown() throws Exception {
        bus.shutdown(true);
    }


    @Test
    public void testEndpoint() throws Exception {
        JaxWsProxyFactoryBean pf = new JaxWsProxyFactoryBean();
        pf.setServiceClass(Greeter.class);
        pf.setAddress(add11);
        pf.setBus(bus);
        Greeter greeter = (Greeter) pf.create();

        assertEquals("sayHi", greeter.sayHi("test"));
        assertEquals("sayHi2", greeter.sayHi2("test"));
    }

    @Test
    public void testSoap12Endpoint() throws Exception {

        JaxWsProxyFactoryBean pf = new JaxWsProxyFactoryBean();
        pf.setServiceClass(Greeter.class);
        pf.setAddress(add12);
        SoapBindingConfiguration config = new SoapBindingConfiguration();
        config.setVersion(Soap12.getInstance());
        pf.setBindingConfig(config);
        pf.setBus(bus);

        Greeter greeter = (Greeter) pf.create();

        assertEquals("sayHi", greeter.sayHi("test"));
        assertEquals("sayHi2", greeter.sayHi2("test"));
    }


    @Test
    public void testBareSoapActionSpoofing() throws Exception {
        JaxWsProxyFactoryBean pf = new JaxWsProxyFactoryBean();
        pf.setServiceClass(Greeter.class);
        pf.setAddress(add11);
        pf.setBus(bus);
        Greeter greeter = (Greeter) pf.create();

        assertEquals("sayHi", greeter.sayHi("test"));
        assertEquals("sayHi2", greeter.sayHi2("test"));

        // Now test spoofing attack
        ((BindingProvider)greeter).getRequestContext().put(BindingProvider.SOAPACTION_USE_PROPERTY, "true");
        ((BindingProvider)greeter).getRequestContext().put(
            BindingProvider.SOAPACTION_URI_PROPERTY, "SAY_HI_2"
        );
        try {
            greeter.sayHi("test");
            fail("Failure expected on spoofing attack");
        } catch (Exception ex) {
            // expected
        }

        // Test the other operation
        ((BindingProvider)greeter).getRequestContext().put(BindingProvider.SOAPACTION_USE_PROPERTY, "true");
        ((BindingProvider)greeter).getRequestContext().put(
            BindingProvider.SOAPACTION_URI_PROPERTY, "SAY_HI_1"
        );
        try {
            greeter.sayHi2("test");
            fail("Failure expected on spoofing attack");
        } catch (Exception ex) {
            // expected
        }

        // Test a SOAP Action that does not exist in the binding
        ((BindingProvider)greeter).getRequestContext().put(BindingProvider.SOAPACTION_USE_PROPERTY, "true");
        ((BindingProvider)greeter).getRequestContext().put(
            BindingProvider.SOAPACTION_URI_PROPERTY, "SAY_HI_UNKNOWN"
        );
        try {
            greeter.sayHi("test");
            fail("Failure expected on spoofing attack");
        } catch (Exception ex) {
            // expected
        }
    }

    @Test
    public void testBareSoap12ActionSpoofing() throws Exception {
        JaxWsProxyFactoryBean pf = new JaxWsProxyFactoryBean();
        pf.setServiceClass(Greeter.class);
        pf.setAddress(add12);
        SoapBindingConfiguration config = new SoapBindingConfiguration();
        config.setVersion(Soap12.getInstance());
        pf.setBindingConfig(config);
        pf.setBus(bus);
        Greeter greeter = (Greeter) pf.create();

        assertEquals("sayHi", greeter.sayHi("test"));
        assertEquals("sayHi2", greeter.sayHi2("test"));

        // Now test spoofing attack
        ((BindingProvider)greeter).getRequestContext().put(BindingProvider.SOAPACTION_USE_PROPERTY, "true");
        ((BindingProvider)greeter).getRequestContext().put(
            BindingProvider.SOAPACTION_URI_PROPERTY, "SAY_HI_2"
        );
        try {
            greeter.sayHi("test");
            fail("Failure expected on spoofing attack");
        } catch (Exception ex) {
            // expected
        }

        // Test the other operation
        ((BindingProvider)greeter).getRequestContext().put(BindingProvider.SOAPACTION_USE_PROPERTY, "true");
        ((BindingProvider)greeter).getRequestContext().put(
            BindingProvider.SOAPACTION_URI_PROPERTY, "SAY_HI_1"
        );
        try {
            greeter.sayHi2("test");
            fail("Failure expected on spoofing attack");
        } catch (Exception ex) {
            // expected
        }

        // Test a SOAP Action that does not exist in the binding
        ((BindingProvider)greeter).getRequestContext().put(BindingProvider.SOAPACTION_USE_PROPERTY, "true");
        ((BindingProvider)greeter).getRequestContext().put(
            BindingProvider.SOAPACTION_URI_PROPERTY, "SAY_HI_UNKNOWN"
        );
        try {
            greeter.sayHi("test");
            fail("Failure expected on spoofing attack");
        } catch (Exception ex) {
            // expected
        }
    }

    @Test
    public void testWrappedSoapActionSpoofing() throws Exception {
        JaxWsProxyFactoryBean pf = new JaxWsProxyFactoryBean();
        pf.setServiceClass(WrappedGreeter.class);
        pf.setAddress(add13);
        pf.setBus(bus);
        WrappedGreeter greeter = (WrappedGreeter) pf.create();

        assertEquals("sayHi", greeter.sayHiRequestWrapped("test"));
        assertEquals("sayHi2", greeter.sayHiRequest2Wrapped("test"));

        // Now test spoofing attack
        ((BindingProvider)greeter).getRequestContext().put(BindingProvider.SOAPACTION_USE_PROPERTY, "true");
        ((BindingProvider)greeter).getRequestContext().put(
            BindingProvider.SOAPACTION_URI_PROPERTY, "SAY_HI_2"
        );
        try {
            greeter.sayHiRequestWrapped("test");
            fail("Failure expected on spoofing attack");
        } catch (Exception ex) {
            // expected
        }

        // Test the other operation
        ((BindingProvider)greeter).getRequestContext().put(BindingProvider.SOAPACTION_USE_PROPERTY, "true");
        ((BindingProvider)greeter).getRequestContext().put(
            BindingProvider.SOAPACTION_URI_PROPERTY, "SAY_HI_1"
        );
        try {
            greeter.sayHiRequest2Wrapped("test");
            fail("Failure expected on spoofing attack");
        } catch (Exception ex) {
            // expected
        }

        // Test a SOAP Action that does not exist in the binding
        ((BindingProvider)greeter).getRequestContext().put(BindingProvider.SOAPACTION_USE_PROPERTY, "true");
        ((BindingProvider)greeter).getRequestContext().put(
            BindingProvider.SOAPACTION_URI_PROPERTY, "SAY_HI_UNKNOWN"
        );
        try {
            greeter.sayHiRequestWrapped("test");
            fail("Failure expected on spoofing attack");
        } catch (Exception ex) {
            // expected
        }
    }

    @Test
    public void testWrappedSoap12ActionSpoofing() throws Exception {
        JaxWsProxyFactoryBean pf = new JaxWsProxyFactoryBean();
        pf.setServiceClass(WrappedGreeter.class);
        pf.setAddress(add14);
        SoapBindingConfiguration config = new SoapBindingConfiguration();
        config.setVersion(Soap12.getInstance());
        pf.setBindingConfig(config);
        pf.setBus(bus);
        WrappedGreeter greeter = (WrappedGreeter) pf.create();

        assertEquals("sayHi", greeter.sayHiRequestWrapped("test"));
        assertEquals("sayHi2", greeter.sayHiRequest2Wrapped("test"));

        // Now test spoofing attack
        ((BindingProvider)greeter).getRequestContext().put(BindingProvider.SOAPACTION_USE_PROPERTY, "true");
        ((BindingProvider)greeter).getRequestContext().put(
            BindingProvider.SOAPACTION_URI_PROPERTY, "SAY_HI_2"
        );
        try {
            greeter.sayHiRequestWrapped("test");
            fail("Failure expected on spoofing attack");
        } catch (Exception ex) {
            // expected
        }

        // Test the other operation
        ((BindingProvider)greeter).getRequestContext().put(BindingProvider.SOAPACTION_USE_PROPERTY, "true");
        ((BindingProvider)greeter).getRequestContext().put(
            BindingProvider.SOAPACTION_URI_PROPERTY, "SAY_HI_1"
        );
        try {
            greeter.sayHiRequest2Wrapped("test");
            fail("Failure expected on spoofing attack");
        } catch (Exception ex) {
            // expected
        }

        // Test a SOAP Action that does not exist in the binding
        ((BindingProvider)greeter).getRequestContext().put(BindingProvider.SOAPACTION_USE_PROPERTY, "true");
        ((BindingProvider)greeter).getRequestContext().put(
            BindingProvider.SOAPACTION_URI_PROPERTY, "SAY_HI_UNKNOWN"
        );
        try {
            greeter.sayHiRequestWrapped("test");
            fail("Failure expected on spoofing attack");
        } catch (Exception ex) {
            // expected
        }
    }

    @Test
    public void testRPCLitSoapActionSpoofing() throws Exception {
        JaxWsProxyFactoryBean pf = new JaxWsProxyFactoryBean();
        pf.setServiceClass(RPCGreeter.class);
        pf.setAddress(add15);
        pf.setBus(bus);
        RPCGreeter greeter = (RPCGreeter) pf.create();

        assertEquals("sayHi", greeter.sayHi("test"));
        assertEquals("sayHi2", greeter.sayHi2("test"));

        // Now test spoofing attack
        ((BindingProvider)greeter).getRequestContext().put(BindingProvider.SOAPACTION_USE_PROPERTY, "true");
        ((BindingProvider)greeter).getRequestContext().put(
            BindingProvider.SOAPACTION_URI_PROPERTY, "SAY_HI_2"
        );
        try {
            greeter.sayHi("test");
            fail("Failure expected on spoofing attack");
        } catch (Exception ex) {
            // expected
        }

        // Test the other operation
        ((BindingProvider)greeter).getRequestContext().put(BindingProvider.SOAPACTION_USE_PROPERTY, "true");
        ((BindingProvider)greeter).getRequestContext().put(
            BindingProvider.SOAPACTION_URI_PROPERTY, "SAY_HI_1"
        );
        try {
            greeter.sayHi2("test");
            fail("Failure expected on spoofing attack");
        } catch (Exception ex) {
            // expected
        }

        // Test a SOAP Action that does not exist in the binding
        ((BindingProvider)greeter).getRequestContext().put(BindingProvider.SOAPACTION_USE_PROPERTY, "true");
        ((BindingProvider)greeter).getRequestContext().put(
            BindingProvider.SOAPACTION_URI_PROPERTY, "SAY_HI_UNKNOWN"
        );
        try {
            greeter.sayHi("test");
            fail("Failure expected on spoofing attack");
        } catch (Exception ex) {
            // expected
        }
    }

    @Test
    public void testRPCEncodedSoapActionSpoofing() throws Exception {
        JaxWsProxyFactoryBean pf = new JaxWsProxyFactoryBean();
        pf.setServiceClass(WrappedGreeter.class);
        pf.setAddress(add16);
        pf.setBus(bus);
        WrappedGreeter greeter = (WrappedGreeter) pf.create();

        assertEquals("sayHi", greeter.sayHiRequestWrapped("test"));
        assertEquals("sayHi2", greeter.sayHiRequest2Wrapped("test"));

        // Now test spoofing attack
        ((BindingProvider)greeter).getRequestContext().put(BindingProvider.SOAPACTION_USE_PROPERTY, "true");
        ((BindingProvider)greeter).getRequestContext().put(
            BindingProvider.SOAPACTION_URI_PROPERTY, "SAY_HI_2"
        );
        try {
            greeter.sayHiRequestWrapped("test");
            fail("Failure expected on spoofing attack");
        } catch (Exception ex) {
            // expected
        }

        // Test the other operation
        ((BindingProvider)greeter).getRequestContext().put(BindingProvider.SOAPACTION_USE_PROPERTY, "true");
        ((BindingProvider)greeter).getRequestContext().put(
            BindingProvider.SOAPACTION_URI_PROPERTY, "SAY_HI_1"
        );
        try {
            greeter.sayHiRequest2Wrapped("test");
            fail("Failure expected on spoofing attack");
        } catch (Exception ex) {
            // expected
        }

        // Test a SOAP Action that does not exist in the binding
        ((BindingProvider)greeter).getRequestContext().put(BindingProvider.SOAPACTION_USE_PROPERTY, "true");
        ((BindingProvider)greeter).getRequestContext().put(
            BindingProvider.SOAPACTION_URI_PROPERTY, "SAY_HI_UNKNOWN"
        );
        try {
            greeter.sayHiRequestWrapped("test");
            fail("Failure expected on spoofing attack");
        } catch (Exception ex) {
            // expected
        }
    }

    @Test
    public void testWrappedEncodedSoapActionSpoofing() throws Exception {
        JaxWsProxyFactoryBean pf = new JaxWsProxyFactoryBean();
        pf.setServiceClass(WrappedGreeter.class);
        pf.setAddress(add17);
        pf.setBus(bus);
        WrappedGreeter greeter = (WrappedGreeter) pf.create();

        assertEquals("sayHi", greeter.sayHiRequestWrapped("test"));
        assertEquals("sayHi2", greeter.sayHiRequest2Wrapped("test"));

        // Now test spoofing attack
        ((BindingProvider)greeter).getRequestContext().put(BindingProvider.SOAPACTION_USE_PROPERTY, "true");
        ((BindingProvider)greeter).getRequestContext().put(
            BindingProvider.SOAPACTION_URI_PROPERTY, "SAY_HI_2"
        );
        try {
            greeter.sayHiRequestWrapped("test");
            fail("Failure expected on spoofing attack");
        } catch (Exception ex) {
            // expected
        }

        // Test the other operation
        ((BindingProvider)greeter).getRequestContext().put(BindingProvider.SOAPACTION_USE_PROPERTY, "true");
        ((BindingProvider)greeter).getRequestContext().put(
            BindingProvider.SOAPACTION_URI_PROPERTY, "SAY_HI_1"
        );
        try {
            greeter.sayHiRequest2Wrapped("test");
            fail("Failure expected on spoofing attack");
        } catch (Exception ex) {
            // expected
        }

        // Test a SOAP Action that does not exist in the binding
        ((BindingProvider)greeter).getRequestContext().put(BindingProvider.SOAPACTION_USE_PROPERTY, "true");
        ((BindingProvider)greeter).getRequestContext().put(
            BindingProvider.SOAPACTION_URI_PROPERTY, "SAY_HI_UNKNOWN"
        );
        try {
            greeter.sayHiRequestWrapped("test");
            fail("Failure expected on spoofing attack");
        } catch (Exception ex) {
            // expected
        }
    }

}