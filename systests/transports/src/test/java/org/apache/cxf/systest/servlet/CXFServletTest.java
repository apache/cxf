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
package org.apache.cxf.systest.servlet;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import jakarta.jws.WebService;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.soap.SOAPBinding;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.hello_world_soap_http.BaseGreeterImpl;
import org.apache.html.dom.HTMLAnchorElementImpl;
import org.apache.html.dom.HTMLDocumentImpl;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;


public class CXFServletTest extends AbstractServletTest {

    @Ignore
    public static class EmbeddedJettyServer extends AbstractJettyServer {
        public static final int PORT = allocatePortAsInt(EmbeddedJettyServer.class);

        public EmbeddedJettyServer() {
            super("/org/apache/cxf/systest/servlet/web.xml", "/", CONTEXT, PORT);
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly", launchServer(EmbeddedJettyServer.class, true));
        createStaticBus();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        stopAllServers();
    }

    @Test
    public void testPostInvokeServices() throws Exception {

        invoke(StandardCharsets.UTF_8.name());
        invoke("iso-8859-1");
    }

    private void invoke(String encoding) throws Exception {
        try (CloseableHttpClient client = newClient()) {
            final HttpPost method = new HttpPost(uri("/services/greeter"));

            method.setEntity(new InputStreamEntity(getClass().getResourceAsStream("GreeterMessage.xml"),
                ContentType.create("text/xml", encoding)));

            try (CloseableHttpResponse response = client.execute(method)) {
                assertEquals("text/xml", getContentType(response));
                assertEquals(encoding.toUpperCase(), getCharset(response));
        
                Document doc = StaxUtils.read(response.getEntity().getContent());
                assertNotNull(doc);
        
                addNamespace("h", "http://apache.org/hello_world_soap_http/types");
        
                assertValid("/s:Envelope/s:Body", doc);
                assertValid("//h:sayHiResponse", doc);
            }
        }
    }

    @Test
    public void testGetServiceList() throws Exception {
        try (CloseableHttpClient client = newClient()) {
            final HttpGet method = new HttpGet(uri("/services"));
    
            try (CloseableHttpResponse res = client.execute(method)) {
                HTMLDocumentImpl doc = parse(res.getEntity().getContent());
                Collection<HTMLAnchorElementImpl> links = getLinks(doc);
                assertEquals("Wrong number of service links", 6, links.size());
        
                Set<String> links2 = new HashSet<>();
                for (HTMLAnchorElementImpl l : links) {
                    links2.add(l.getHref());
                }
        
                assertTrue(links2.contains(uri("/services/greeter?wsdl")));
                assertTrue(links2.contains(uri("/services/greeter2?wsdl")));
                assertTrue(links2.contains("http://cxf.apache.org/MyGreeter?wsdl"));

                assertEquals("text/html", getContentType(res));
            }
        
            try (CloseableHttpResponse res = client.execute(method)) {
                HTMLDocumentImpl doc = parse(res.getEntity().getContent());
                Collection<HTMLAnchorElementImpl> links = getLinks(doc);

                Set<String> links2 = new HashSet<>();
                for (HTMLAnchorElementImpl l : links) {
                    links2.add(l.getHref());
                }

                assertEquals("Wrong number of service links", 6, links.size());
                assertTrue(links2.contains(uri("/services/greeter?wsdl")));
                assertTrue(links2.contains(uri("/services/greeter2?wsdl")));
                assertTrue(links2.contains("http://cxf.apache.org/MyGreeter?wsdl"));
        
                assertEquals("text/html", getContentType(res));
        
                // Ensure that the Bus is available for people doing an Endpoint.publish() or similar.
                assertNotNull(BusFactory.getDefaultBus(false));
            }
        }
    }

    @Test
    public void testGetUnformatServiceList() throws Exception {
        try (CloseableHttpClient client = newClient()) {
            final HttpGet method = new HttpGet(uri("/services?formatted=false"));

            try (CloseableHttpResponse res = client.execute(method)) {
                final String content = IOUtils.readStringFromStream(res.getEntity().getContent());
                assertTrue(content.contains(uri("/services/greeter3")));
                assertTrue(content.contains(uri("/services/greeter2")));
                assertTrue(content.contains(uri("/services/greeter")));
            }
        }
    }

    @Test
    public void testServiceListWithLoopAddress() throws Exception {
        try (CloseableHttpClient client = newClient()) {
            final HttpGet method = new HttpGet(uri("/services"));

            try (CloseableHttpResponse res = client.execute(method)) {
                final String content = IOUtils.readStringFromStream(res.getEntity().getContent());
                assertTrue(content.contains(uri("/services/greeter3")));
                assertTrue(content.contains(uri("/services/greeter2")));
                assertTrue(content.contains(uri("/services/greeter")));
            }
            
            final HttpGet greeter = new HttpGet(uri("/services/greeter?wsdl"));
            try (CloseableHttpResponse res = client.execute(greeter)) {
                assertThat(res.getStatusLine().getStatusCode(), equalTo(200));
            }
            
            final HttpGet greeter2 = new HttpGet(uri("/services/greeter2?wsdl"));
            try (CloseableHttpResponse res = client.execute(greeter2)) {
                assertThat(res.getStatusLine().getStatusCode(), equalTo(200));
            }
            
            final HttpGet greeter3 = new HttpGet(uri("/services/greeter3?wsdl"));
            try (CloseableHttpResponse res = client.execute(greeter3)) {
                assertThat(res.getStatusLine().getStatusCode(), equalTo(200));
            }
                
            final HttpGet loopback = new HttpGet("http://127.0.0.1:" + getPort() + "/mycontext/services");
            try (CloseableHttpResponse res = client.execute(loopback)) {
                final String content = IOUtils.readStringFromStream(res.getEntity().getContent());
                assertTrue(content.contains("http://127.0.0.1:" + getPort() + "/mycontext/services"));
            }
        }
    }

    @Test
    public void testGetWSDL() throws Exception {
        try (CloseableHttpClient client = newClient()) {
            final HttpGet method = new HttpGet(uri("/services/greeter?wsdl"));

            try (CloseableHttpResponse res = client.execute(method)) {
                assertEquals(200, res.getStatusLine().getStatusCode());
                assertEquals("text/xml", getContentType(res));
                Document doc = StaxUtils.read(res.getEntity().getContent());
                assertNotNull(doc);
        
                assertValid("//wsdl:operation[@name='greetMe']", doc);
                assertValid("//wsdlsoap:address[@location='" + uri("/services/greeter") + "']", doc);
            }
        }
    }


    @Test
    public void testGetWSDLWithMultiplePublishedEndpointUrl() throws Exception {
        try (CloseableHttpClient client = newClient()) {
            final HttpGet method = new HttpGet(uri("/services/greeter5?wsdl"));

            try (CloseableHttpResponse res = client.execute(method)) {
                assertEquals(200, res.getStatusLine().getStatusCode());
                assertEquals("text/xml", getContentType(res));
                Document doc = StaxUtils.read(res.getEntity().getContent());
                assertNotNull(doc);
                WSDLReader wsdlReader = WSDLFactory.newInstance().newWSDLReader();
                wsdlReader.setFeature("javax.wsdl.verbose", false);
        
                assertValid(
                    "//wsdl:service[@name='SOAPService']/wsdl:port[@name='SoapPort']/wsdlsoap:address[@location='"
                        + "http://cxf.apache.org/publishedEndpointUrl1']", doc);
                assertValid(
                    "//wsdl:service[@name='SOAPService']/wsdl:port[@name='SoapPort1']/wsdlsoap:address[@location='"
                        + "http://cxf.apache.org/publishedEndpointUrl2']", doc);
            }
        }
    }

    @Test
    public void testGetWSDLWithIncludes() throws Exception {
        try (CloseableHttpClient client = newClient()) {
            final HttpGet wsdl = new HttpGet(uri("/services/greeter3?wsdl"));

            try (CloseableHttpResponse res = client.execute(wsdl)) {
                assertEquals(200, res.getStatusLine().getStatusCode());
                assertEquals("text/xml", getContentType(res));
                Document doc = StaxUtils.read(res.getEntity().getContent());
                assertNotNull(doc);
        
                assertXPathEquals("//xsd:include/@schemaLocation",
                                  uri("/services/greeter3?xsd=hello_world_includes2.xsd"),
                                  doc.getDocumentElement());
            }
        
            final HttpGet xsd = new HttpGet(uri("/services/greeter3?xsd=hello_world_includes2.xsd"));
            try (CloseableHttpResponse res = client.execute(xsd)) {
                assertEquals(200, res.getStatusLine().getStatusCode());
                assertEquals("text/xml", getContentType(res));
                Document doc = StaxUtils.read(res.getEntity().getContent());
                assertNotNull(doc);
        
                assertValid("//xsd:complexType[@name='ErrorCode']", doc);
            }
        }
    }

    @Test
    public void testGetWSDLWithXMLBinding() throws Exception {
        try (CloseableHttpClient client = newClient()) {
            final HttpGet method = new HttpGet(uri("/services/greeter2?wsdl"));

            try (CloseableHttpResponse res = client.execute(method)) {
                assertEquals(200, res.getStatusLine().getStatusCode());
                assertEquals("text/xml", getContentType(res));
        
                Document doc = StaxUtils.read(res.getEntity().getContent());
                assertNotNull(doc);
        
                addNamespace("http", "http://schemas.xmlsoap.org/wsdl/http/");
                assertValid("//wsdl:operation[@name='greetMe']", doc);
                NodeList addresses = assertValid("//http:address/@location", doc);
                boolean found = true;
                for (int i = 0; i < addresses.getLength(); i++) {
                    String address = addresses.item(i).getLocalName();
                    if (address.startsWith("http://localhost") && address.endsWith("/services/greeter2")) {
                        found = true;
                        break;
                    }
                }
                assertTrue(found);
            }
        }
    }

    @Test
    public void testInvalidServiceUrl() throws Exception {
        try (CloseableHttpClient client = newClient()) {
            final HttpGet method = new HttpGet(uri("/services/NoSuchService"));

            try (CloseableHttpResponse res = client.execute(method)) {
                assertEquals(404, res.getStatusLine().getStatusCode());
                assertEquals("text/html", getContentType(res));
            }
        }
    }

    @Test
    public void testServiceWsdlNotFound() throws Exception {
        final HttpGet method = new HttpGet(uri("/services/NoSuchService?wsdl"));
        expectErrorCode(method, 404, "Response code 404 required for invalid WSDL url.");
    }

    @Test
    public void testGetImportedXSD() throws Exception {
        try (CloseableHttpClient client = newClient()) {
            final HttpGet wsdl = new HttpGet(uri("/services/greeter?wsdl"));
            try (CloseableHttpResponse res = client.execute(wsdl)) {
                assertEquals(200, res.getStatusLine().getStatusCode());
                final String text = IOUtils.readStringFromStream(res.getEntity().getContent());
                assertEquals("text/xml", getContentType(res));
                assertTrue(text.contains(uri("/services/greeter?wsdl=test_import.xsd")));
            }
            
            final HttpGet xsd = new HttpGet(uri("/services/greeter?wsdl=test_import.xsd"));
            try (CloseableHttpResponse res = client.execute(xsd)) {
                assertEquals(200, res.getStatusLine().getStatusCode());
                final String text = IOUtils.readStringFromStream(res.getEntity().getContent());
        
                assertEquals("text/xml", getContentType(res));
                assertTrue("the xsd should contain the completType SimpleStruct",
                           text.contains("<complexType name=\"SimpleStruct\">"));
            }
        }
    }

    @Test
    public void testGetBinding() throws Exception {
        final Bus bus = BusFactory.getDefaultBus();
        try {
            BusFactory.setDefaultBus(null);
            Endpoint ep = Endpoint.create("http://schemas.xmlsoap.org/wsdl/soap/http", new HelloImpl());
            assertTrue(ep.getBinding() instanceof SOAPBinding);
        } finally {
            BusFactory.setDefaultBus(bus);
        }
    }

    @WebService(name = "Hello", portName = "HelloPort",
                serviceName = "HelloService", targetNamespace = "http://cxf.apache.org/hello")
    public static class HelloImpl {
        public String hello(String name) {
            return "Hello " + name;
        }
    }
    @WebService(serviceName = "SOAPService",
                portName = "SoapPort",
                endpointInterface = "org.apache.hello_world_soap_http.Greeter",
                targetNamespace = "http://apache.org/hello_world_soap_http")
    public static class NoWsdlGreeter extends BaseGreeterImpl {


    }

    @Override
    protected int getPort() {
        return EmbeddedJettyServer.PORT;
    }
}
