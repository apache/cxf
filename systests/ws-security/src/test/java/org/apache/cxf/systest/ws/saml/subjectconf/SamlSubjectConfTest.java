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

package org.apache.cxf.systest.ws.saml.subjectconf;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import javax.xml.namespace.QName;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.rt.security.SecurityConstants;
import org.apache.cxf.systest.ws.common.TestParam;
import org.apache.cxf.systest.ws.saml.client.SamlCallbackHandler;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A set of tests for the validation rules associated with various Subject Confirmation
 * methods.
 */
@RunWith(value = org.junit.runners.Parameterized.class)
public class SamlSubjectConfTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(Server.class);
    static final String STAX_PORT = allocatePort(StaxServer.class);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    final TestParam test;

    public SamlSubjectConfTest(TestParam type) {
        this.test = type;
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(Server.class, true)
        );
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(StaxServer.class, true)
        );
    }

    @Parameters(name = "{0}")
    public static Collection<TestParam> data() {

        return Arrays.asList(new TestParam[] {new TestParam(PORT, false),
                                              new TestParam(STAX_PORT, false),
        });
    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        stopAllServers();
    }

    //
    // HOK requires client auth + a internally signed token. The server is set up not to
    // require client auth to test this.
    //

    @org.junit.Test
    public void testHOKClientAuthentication() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlSubjectConfTest.class.getResource("client-auth.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlSubjectConfTest.class.getResource("DoubleItSamlSubjectConf.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2TransportPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        // Successful call
        SamlCallbackHandler callbackHandler = new SamlCallbackHandler(true, true);
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_HOLDER_KEY);

        callbackHandler.setCryptoAlias("morpit");
        callbackHandler.setCryptoPassword("password");
        callbackHandler.setCryptoPropertiesFile("morpit.properties");

        ((BindingProvider)port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, callbackHandler
        );
        int result = port.doubleIt(25);
        assertTrue(result == 50);

        // Don't sign the Assertion
        callbackHandler = new SamlCallbackHandler(true, false);
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_HOLDER_KEY);

        callbackHandler.setCryptoAlias("morpit");
        callbackHandler.setCryptoPassword("password");
        callbackHandler.setCryptoPropertiesFile("morpit.properties");

        ((BindingProvider)port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, callbackHandler
        );
        try {
            port.doubleIt(25);
            fail("Failure expected on a unsigned assertion");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    // Sign using "alice"
    @org.junit.Test
    public void testHOKNonMatchingCert() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlSubjectConfTest.class.getResource("client-auth.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlSubjectConfTest.class.getResource("DoubleItSamlSubjectConf.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2TransportPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        SamlCallbackHandler callbackHandler = new SamlCallbackHandler(true, true);
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_HOLDER_KEY);
        ((BindingProvider)port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, callbackHandler
        );

        try {
            port.doubleIt(25);
            fail("Failure expected on a non matching cert (SAML -> TLS)");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }

        ((java.io.Closeable)port).close();

        bus.shutdown(true);
    }

    @org.junit.Test
    public void testHOKNoClientAuthentication() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlSubjectConfTest.class.getResource("client-noauth.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlSubjectConfTest.class.getResource("DoubleItSamlSubjectConf.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2TransportPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        // Successful call
        SamlCallbackHandler callbackHandler = new SamlCallbackHandler(true, true);
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_HOLDER_KEY);

        callbackHandler.setCryptoAlias("morpit");
        callbackHandler.setCryptoPassword("password");
        callbackHandler.setCryptoPropertiesFile("morpit.properties");

        ((BindingProvider)port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, callbackHandler
        );
        try {
            port.doubleIt(25);
            fail("Failure expected on no client auth");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }

        ((java.io.Closeable)port).close();

        // Here we try against a service that has explicitly disabled the SAML Subject Confirmation Method requirements,
        // and so the invocation should pass
        portQName = new QName(NAMESPACE, "DoubleItSaml2TransportPort2");
        port = service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        ((BindingProvider)port).getRequestContext().put(SecurityConstants.SAML_CALLBACK_HANDLER, callbackHandler);
        int result = port.doubleIt(25);
        assertTrue(result == 50);
        ((java.io.Closeable)port).close();

        bus.shutdown(true);
    }

    //
    // SV requires client auth. The server is set up not to require client auth to
    // test this. SV does not require an internal signature unlike HOK.
    //
    @org.junit.Test
    public void testSVClientAuthentication() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlSubjectConfTest.class.getResource("client-auth.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlSubjectConfTest.class.getResource("DoubleItSamlSubjectConf.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2TransportPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        // Successful call
        SamlCallbackHandler callbackHandler = new SamlCallbackHandler(true, false);
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_SENDER_VOUCHES);

        ((BindingProvider)port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, callbackHandler
        );
        int result = port.doubleIt(25);
        assertTrue(result == 50);

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testSVNoClientAuthentication() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlSubjectConfTest.class.getResource("client-noauth.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlSubjectConfTest.class.getResource("DoubleItSamlSubjectConf.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2TransportPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        // Successful call
        SamlCallbackHandler callbackHandler = new SamlCallbackHandler(true, false);
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_SENDER_VOUCHES);

        ((BindingProvider)port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, callbackHandler
        );
        try {
            port.doubleIt(25);
            fail("Failure expected on no client auth");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    //
    // Bearer does not require client auth, but it does require an internal signature
    //

    @org.junit.Test
    public void testBearer() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlSubjectConfTest.class.getResource("client-auth.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlSubjectConfTest.class.getResource("DoubleItSamlSubjectConf.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2TransportPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        // Successful call
        SamlCallbackHandler callbackHandler = new SamlCallbackHandler(true, true);
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_BEARER);

        callbackHandler.setCryptoAlias("morpit");
        callbackHandler.setCryptoPassword("password");
        callbackHandler.setCryptoPropertiesFile("morpit.properties");

        ((BindingProvider)port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, callbackHandler
        );
        int result = port.doubleIt(25);
        assertTrue(result == 50);

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testUnsignedBearer() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlSubjectConfTest.class.getResource("client-auth.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlSubjectConfTest.class.getResource("DoubleItSamlSubjectConf.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2TransportPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        // Successful call
        SamlCallbackHandler callbackHandler = new SamlCallbackHandler(true, false);
        callbackHandler.setConfirmationMethod(SAML2Constants.CONF_BEARER);

        ((BindingProvider)port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, callbackHandler
        );

        try {
            port.doubleIt(25);
            fail("Failure expected on an unsigned bearer token");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }

    @org.junit.Test
    public void testUnknownCustomMethod() throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = SamlSubjectConfTest.class.getResource("client-auth.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = SamlSubjectConfTest.class.getResource("DoubleItSamlSubjectConf.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItSaml2TransportPort");
        DoubleItPortType port =
                service.getPort(portQName, DoubleItPortType.class);
        updateAddressPort(port, test.getPort());

        // Successful call
        SamlCallbackHandler callbackHandler = new SamlCallbackHandler(true, false);
        callbackHandler.setConfirmationMethod("urn:oasis:names:tc:SAML:2.0:cm:custom");

        ((BindingProvider)port).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, callbackHandler
        );

        try {
            port.doubleIt(25);
            fail("Failure expected on an unknown custom subject confirmation method");
        } catch (jakarta.xml.ws.soap.SOAPFaultException ex) {
            // expected
        }

        ((java.io.Closeable)port).close();
        bus.shutdown(true);
    }


}
