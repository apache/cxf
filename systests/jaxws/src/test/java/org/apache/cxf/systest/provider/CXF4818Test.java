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

package org.apache.cxf.systest.provider;


import java.io.InputStream;
import java.io.StringReader;

import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import jakarta.annotation.Resource;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.Provider;
import jakarta.xml.ws.ServiceMode;
import jakarta.xml.ws.WebServiceContext;
import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.WebServiceProvider;
import jakarta.xml.ws.soap.Addressing;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CXF4818Test extends AbstractBusClientServerTestBase {

    public static final String ADDRESS
        = "http://localhost:" + TestUtil.getPortNumber(Server.class)
            + "/AddressProvider/AddressProvider";

    public static class Server extends AbstractBusTestServerBase {

        protected void run() {
            Object implementor = new CXF4818Provider();
            Endpoint.publish(ADDRESS, implementor);
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
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @Test
    public void testCXF4818() throws Exception {
        InputStream body = getClass().getResourceAsStream("cxf4818data.txt");
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(ADDRESS);
        post.setEntity(new InputStreamEntity(body, ContentType.TEXT_XML));
        CloseableHttpResponse response = client.execute(post);

        Document doc = StaxUtils.read(response.getEntity().getContent());
        //System.out.println(StaxUtils.toString(doc));
        Element root = doc.getDocumentElement();
        Node child = root.getFirstChild();

        boolean foundBody = false;
        boolean foundHeader = false;
        while (child != null) {
            if ("Header".equals(child.getLocalName())) {
                foundHeader = true;
                assertFalse("Already found body", foundBody);
            } else if ("Body".equals(child.getLocalName())) {
                foundBody = true;
                assertTrue("Did not find header before the body", foundHeader);
            }
            child = child.getNextSibling();
        }
        assertTrue("Did not find the soap:Body element", foundBody);
        assertTrue("Did not find the soap:Header element", foundHeader);
    }



    @WebServiceProvider(serviceName = "GenericService",
        targetNamespace = "http://cxf.apache.org/basictest",
        portName = "GenericServicePosrt")
    @ServiceMode(value = jakarta.xml.ws.Service.Mode.MESSAGE)
    @Addressing
    public static class CXF4818Provider implements Provider<SOAPMessage> {

        @Resource
        protected WebServiceContext context;

        public SOAPMessage invoke(SOAPMessage request) {
            try {
                String responseText = "<SOAP-ENV:Envelope "
                    + "xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                    + "<SOAP-ENV:Body>"
                    + "<ns2:FooResponse xmlns:ns2=\"http://cxf.apache.org/soapheader/inband\">"
                    + "<ns2:Return>Foo Response Body</ns2:Return>"
                    + "</ns2:FooResponse>"
                    + "</SOAP-ENV:Body>"
                    + "</SOAP-ENV:Envelope>\n";


                // Create a SOAP request message
                MessageFactory soapmsgfactory = MessageFactory.newInstance();
                SOAPMessage responseMessage = soapmsgfactory.createMessage();
                StreamSource responseMessageSrc = null;

                responseMessageSrc = new StreamSource(new StringReader(responseText));
                responseMessage.getSOAPPart().setContent(responseMessageSrc);
                responseMessage.saveChanges();

                return responseMessage;

            } catch (Exception e) {
                throw new WebServiceException(e);
            }

        }

    }
}
