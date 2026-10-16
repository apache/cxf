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
package org.apache.cxf.service.factory;

import java.io.File;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;

import javax.wsdl.Definition;
import javax.xml.namespace.QName;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ClientFactoryBean;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl11.ServiceWSDLBuilder;
import org.apache.hello_world_doc_lit.Greeter;
import org.apache.hello_world_doc_lit.GreeterImplDoc;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RountripTest extends AbstractSimpleFrontendTest {

    @Test
    public void testServerFactoryBean() throws Exception {
        ServerFactoryBean svrBean = new ServerFactoryBean();
        svrBean.setAddress("http://localhost/Hello");
        svrBean.setTransportId("http://schemas.xmlsoap.org/soap/http");
        svrBean.setServiceBean(new HelloServiceImpl());
        svrBean.setServiceClass(HelloService.class);
        svrBean.setBus(getBus());

        svrBean.create();

        ClientProxyFactoryBean proxyFactory = new ClientProxyFactoryBean();
        ClientFactoryBean clientBean = proxyFactory.getClientFactoryBean();
        clientBean.setAddress("http://localhost/Hello");
        clientBean.setTransportId("http://schemas.xmlsoap.org/soap/http");
        clientBean.setServiceClass(HelloService.class);
        clientBean.setBus(getBus());

        HelloService client = (HelloService) proxyFactory.create();

        assertEquals("hello", client.sayHello());
        assertEquals("hello", client.echo("hello"));
    }

    @Test
    public void testVoidMethodWithWsdl() throws Exception {
        ServerFactoryBean svrBean = new ServerFactoryBean();
        svrBean.setAddress("http://localhost/HelloWsdl");
        svrBean.setTransportId("http://schemas.xmlsoap.org/soap/http");
        svrBean.setServiceBean(new HelloServiceImpl());
        svrBean.setServiceClass(HelloService.class);
        svrBean.setBus(getBus());

        Server server = svrBean.create();

        Definition def = new ServiceWSDLBuilder(getBus(),
            server.getEndpoint().getService().getServiceInfos()).build();
        File wsdlFile = File.createTempFile("hello", ".wsdl");
        wsdlFile.deleteOnExit();
        try (Writer w = Files.newBufferedWriter(wsdlFile.toPath())) {
            getBus().getExtension(WSDLManager.class).getWSDLFactory().newWSDLWriter().writeWSDL(def, w);
        }

        ClientProxyFactoryBean proxyFactory = new ClientProxyFactoryBean();
        ClientFactoryBean clientBean = proxyFactory.getClientFactoryBean();
        clientBean.setAddress("http://localhost/HelloWsdl");
        clientBean.setTransportId("http://schemas.xmlsoap.org/soap/http");
        clientBean.setServiceClass(HelloService.class);
        clientBean.setWsdlURL(wsdlFile.toURI().toString());
        clientBean.setBus(getBus());

        HelloService client = (HelloService) proxyFactory.create();

        assertEquals("hello", client.sayHello());
        assertEquals("hello", client.echo("hello"));
        client.ping();
    }

    @Test
    public void testOneWay() throws Exception {
        ServerFactoryBean svrBean = new ServerFactoryBean();
        svrBean.setAddress("http://localhost/Hello2");
        svrBean.setTransportId("http://schemas.xmlsoap.org/soap/http");
        svrBean.setServiceBean(new GreeterImplDoc());
        svrBean.setServiceClass(Greeter.class);
        URL wsdl = RountripTest.class.getResource(
            "/wsdl/hello_world_doc_lit.wsdl");

        svrBean.setEndpointName(new QName("http://apache.org/hello_world_doc_lit",
                                        "SoapPort"));
        svrBean.setServiceName(new QName("http://apache.org/hello_world_doc_lit",
                                         "SOAPService"));
        svrBean.setWsdlLocation(wsdl.toString());
        svrBean.setBus(getBus());

        svrBean.create();
    }
}