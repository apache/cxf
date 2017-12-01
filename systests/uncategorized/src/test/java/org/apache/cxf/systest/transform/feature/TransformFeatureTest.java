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



package org.apache.cxf.systest.transform.feature;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.feature.transform.XSLTInInterceptor;
import org.apache.cxf.feature.transform.XSLTOutInterceptor;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TransformFeatureTest extends AbstractBusClientServerTestBase {    
    private static final String PORT = EchoServer.PORT;
    private static final QName PORT_NAME = new QName("http://apache.org/echo", "EchoPort");
    private static final QName SERVICE_NAME = new QName("http://apache.org/echo", "EchoService");
    private static final String XSLT_REQUEST_PATH = "request.xsl"; 
    private static final String XSLT_RESPONSE_PATH = "response.xsl"; 
    private static final String TRANSFORMED_CONSTANT = "TRANSFORMED";

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(EchoServer.class, true));
    }

    @Test
    public void testClientOutTransformation() {
        Service service = Service.create(SERVICE_NAME);
        String endpoint = "http://localhost:" + PORT + "/EchoContext/EchoPort";
        service.addPort(PORT_NAME, SOAPBinding.SOAP11HTTP_BINDING, endpoint);
         
        Echo port = service.getPort(PORT_NAME, Echo.class);
        Client client = ClientProxy.getClient(port);
        XSLTOutInterceptor outInterceptor = new XSLTOutInterceptor(XSLT_REQUEST_PATH);
        client.getOutInterceptors().add(outInterceptor);
        String response = port.echo("test");
        Assert.assertTrue("Request was not transformed", response.contains(TRANSFORMED_CONSTANT));
    }

    @Test
    public void testClientInTransformation() {
        Service service = Service.create(SERVICE_NAME);
        String endpoint = "http://localhost:" + PORT + "/EchoContext/EchoPort";
        service.addPort(PORT_NAME, SOAPBinding.SOAP11HTTP_BINDING, endpoint);
         
        Echo port = service.getPort(PORT_NAME, Echo.class);
        Client client = ClientProxy.getClient(port);
        XSLTInInterceptor inInterceptor = new XSLTInInterceptor(XSLT_RESPONSE_PATH);
        client.getInInterceptors().add(inInterceptor);
        String response = port.echo("test");
        Assert.assertTrue(response.contains(TRANSFORMED_CONSTANT));
    }
}

