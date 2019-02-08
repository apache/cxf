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



package org.apache.cxf.systest.soapheader_ext;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceClient;

import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.soap_ext_header.audit.Audit;
import org.apache.cxf.soap_ext_header.ws.SamplePortType;
import org.apache.cxf.soap_ext_header.ws.SampleService;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ExtSoapHeaderClientServerTest extends AbstractBusClientServerTestBase {
    public static final String PORT0 = allocatePort(Server.class, 0);
    public static final String PORT1 = allocatePort(Server.class, 1);

    private static SamplePortType client;
    private static org.apache.cxf.endpoint.Server extserver;

    private static final QName SERVIVE_NAME = new QName("http://cxf.apache.org/soap_ext_header/ws", "SampleService");

    public static class Server extends AbstractBusTestServerBase {

        protected void run() {
            String address0 = "http://localhost:" + PORT0 + "/SoapExtHeader/SampleService";

            Object implementor1 = new SamplePortTypeImpl();
            Endpoint.publish(address0, implementor1);

            String address1 = "http://localhost:" + PORT1 + "/SoapExtHeader/SampleService";
            JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
            sf.setServiceClass(SamplePortTypeImpl.class);

            WebServiceClient webService = SampleService.class.getAnnotation(WebServiceClient.class);
            sf.setServiceName(new QName(webService.targetNamespace(), webService.name()));
            sf.setWsdlLocation(webService.wsdlLocation());
            sf.setAddress(address1);

            extserver = sf.create();
        }

        public static void main(String[] args) {
            try {
                Server s = new Server();
                s.start();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(-1);
            } finally {
                System.out.println("done!");
            }
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        createStaticBus();
        assertTrue("server did not launch correctly", launchServer(Server.class, true));

        initClient();
    }

    @AfterClass
    public static void tearDownExtServer() throws Exception {
        if (extserver != null) {
            extserver.stop();
            extserver.destroy();
        }
    }

    private static void initClient() {
        URL wsdl = ExtSoapHeaderClientServerTest.class.getResource("/wsdl_systest/soap_ext_header.wsdl");
        assertNotNull("WSDL is null", wsdl);

        SampleService service = new SampleService(wsdl, SERVIVE_NAME);

        assertNotNull("Service is null ", service);
        client = service.getSamplePort();
    }

    @Test
    public void testWithArg() throws Exception {
        testWithArg(PORT0);
    }

    @Test
    public void testWithArgWSDL() throws Exception {
        testWithArg(PORT1);
    }

    @Test
    public void testWithNoArg() throws Exception {
        testWithNoArg(PORT0);
    }

    @Test
    public void testWithNoArgWSDL() throws Exception {
        testWithNoArg(PORT1);
    }

    private void testWithArg(String port) throws Exception {

        updateAddressPort(client, port);

        Audit audit = createAudit();

        List<String> res = client.singleArg(Arrays.asList("Hello"), audit);
        assertEquals(1, res.size());

        assertEquals("jerry", res.get(0));
    }

    private void testWithNoArg(String port) throws Exception {

        updateAddressPort(client, port);

        Audit audit = createAudit();

        List<String> res = client.noArgs(audit);
        assertEquals(1, res.size());

        assertEquals("george", res.get(0));
    }

    private Audit createAudit() {
        Audit audit = new Audit();
        audit.setMessageId("m1");
        audit.setSender("s1");
        return audit;
    }

}

