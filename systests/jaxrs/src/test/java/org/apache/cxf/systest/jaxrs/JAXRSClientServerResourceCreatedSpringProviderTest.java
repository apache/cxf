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

package org.apache.cxf.systest.jaxrs;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.model.wadl.WadlGenerator;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.HTTPConduit;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JAXRSClientServerResourceCreatedSpringProviderTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerResourceCreatedSpringProviders.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly",
                   launchServer(BookServerResourceCreatedSpringProviders.class, true));
        createStaticBus();
    }

    @Test
    public void testBasePetStoreWithoutTrailingSlash() throws Exception {

        String endpointAddress = "http://localhost:" + PORT + "/webapp/pets";
        WebClient client = WebClient.create(endpointAddress);
        HTTPConduit conduit = WebClient.getConfig(client).getHttpConduit();
        conduit.getClient().setReceiveTimeout(1000000);
        conduit.getClient().setConnectionTimeout(1000000);
        String value = client.accept("text/plain").get(String.class);
        assertEquals(PetStore.CLOSED, value);
    }

    @Test
    public void testBasePetStore() throws Exception {

        String endpointAddress = "http://localhost:" + PORT + "/webapp/pets/";
        WebClient client = WebClient.create(endpointAddress);
        HTTPConduit conduit = WebClient.getConfig(client).getHttpConduit();
        conduit.getClient().setReceiveTimeout(1000000);
        conduit.getClient().setConnectionTimeout(1000000);
        String value = client.accept("text/plain").get(String.class);
        assertEquals(PetStore.CLOSED, value);
    }

    @Test
    public void testMultipleRootsWadl() throws Exception {
        List<Element> resourceEls = getWadlResourcesInfo("http://localhost:" + PORT + "/webapp/resources",
                                                         "http://localhost:" + PORT + "/webapp/resources", 2);
        String path1 = resourceEls.get(0).getAttribute("path");
        int bookStoreInd = path1.contains("/bookstore") ? 0 : 1;
        int petStoreInd = bookStoreInd == 0 ? 1 : 0;
        checkBookStoreInfo(resourceEls.get(bookStoreInd));
        checkServletInfo(resourceEls.get(petStoreInd));
    }

    @Test
    public void testBookStoreWadl() throws Exception {
        List<Element> resourceEls = getWadlResourcesInfo("http://localhost:" + PORT + "/webapp/resources",
            "http://localhost:" + PORT + "/webapp/resources/bookstore", 1);
        checkBookStoreInfo(resourceEls.get(0));
    }

    private void assertValidType(XPathUtils xpu, String xpath, String type, Element el) {
        String s = (String)xpu.getValue(xpath, el, XPathConstants.STRING);
        assertNotNull(s);
        assertTrue("Expected " + type + " but found " + s, s.endsWith(type));
    }
    private void assertValid(XPathUtils xpu, String xpath, Element el) {
        assertNotNull(xpu.getValue(xpath, el, XPathConstants.NODE));
    }
    @Test
    public void testPetStoreWadl() throws Exception {
        List<Element> resourceEls = getWadlResourcesInfo("http://localhost:" + PORT + "/webapp/pets",
            "http://localhost:" + PORT + "/webapp/pets/", 1);
        checkPetStoreInfo(resourceEls.get(0));

        Element el = (Element)resourceEls.get(0).getParentNode().getParentNode();
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("xsd", "http://www.w3.org/2001/XMLSchema");
        namespaces.put("ns", "http://pets");
        namespaces.put("wadl", "http://wadl.dev.java.net/2009/02");
        XPathUtils xpu = new XPathUtils(namespaces);
        assertValidType(xpu, "//xsd:element[@name='elstatus']/@type", "petStoreStatusElement", el);
        assertValidType(xpu, "//xsd:schema[@targetNamespace='http://pets']/xsd:element[@name='status']/@type",
                        "status", el);
        assertValidType(xpu, "//xsd:element[@name='statusType']/@type",
                        "statusType", el);
        assertValidType(xpu, "//xsd:element[@name='statusImpl1']/@type",
                        "petStoreStatusImpl1", el);
        assertValidType(xpu, "//xsd:element[@name='statusImpl1']/@substitutionGroup",
                        "statusType", el);
        assertValidType(xpu, "//xsd:element[@name='statusImpl2']/@type",
                        "petStoreStatusImpl2", el);
        assertValidType(xpu, "//xsd:element[@name='statusImpl2']/@substitutionGroup",
                        "statusType", el);

        assertValid(xpu, "//wadl:representation[@element='prefix1:status']", el);
        assertValid(xpu, "//wadl:representation[@element='prefix1:elstatus']", el);
        assertValid(xpu, "//wadl:representation[@element='prefix1:statuses']", el);
        assertValid(xpu, "//wadl:representation[@element='prefix1:statusType']", el);

    }

    @Test
    public void testWadlPublishedEndpointUrl() throws Exception {
        String requestURI = "http://localhost:" + PORT + "/webapp/resources2";
        WebClient client = WebClient.create(requestURI + "?_wadl&_type=xml");
        Document doc = StaxUtils.read(new InputStreamReader(client.get(InputStream.class), StandardCharsets.UTF_8));
        Element root = doc.getDocumentElement();
        assertEquals(WadlGenerator.WADL_NS, root.getNamespaceURI());
        assertEquals("application", root.getLocalName());
        List<Element> resourcesEls = DOMUtils.getChildrenWithName(root,
                                                                  WadlGenerator.WADL_NS, "resources");
        assertEquals(1, resourcesEls.size());
        Element resourcesEl = resourcesEls.get(0);
        assertEquals("http://proxy", resourcesEl.getAttribute("base"));

    }


    private void checkBookStoreInfo(Element resource) {
        assertEquals("/bookstore", resource.getAttribute("path"));
    }

    private void checkServletInfo(Element resource) {
        assertEquals("/servlet", resource.getAttribute("path"));
    }

    private void checkPetStoreInfo(Element resource) {
        assertEquals("/", resource.getAttribute("path"));
    }

    private List<Element> getWadlResourcesInfo(String baseURI, String requestURI, int size) throws Exception {
        WebClient client = WebClient.create(requestURI + "?_wadl&_type=xml");
        WebClient.getConfig(client).getHttpConduit().getClient().setReceiveTimeout(10000000);
        Document doc = StaxUtils.read(new InputStreamReader(client.get(InputStream.class), StandardCharsets.UTF_8));
        Element root = doc.getDocumentElement();
        assertEquals(WadlGenerator.WADL_NS, root.getNamespaceURI());
        assertEquals("application", root.getLocalName());
        List<Element> resourcesEls = DOMUtils.getChildrenWithName(root,
                                                                  WadlGenerator.WADL_NS, "resources");
        assertEquals(1, resourcesEls.size());
        Element resourcesEl = resourcesEls.get(0);
        assertEquals(baseURI, resourcesEl.getAttribute("base"));
        List<Element> resourceEls =
            DOMUtils.getChildrenWithName(resourcesEl,
                                         WadlGenerator.WADL_NS, "resource");
        assertEquals(size, resourceEls.size());
        return resourceEls;
    }

    @Test
    public void testServletConfigInitParam() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/webapp/resources/servlet/config/a";
        WebClient wc = WebClient.create(endpointAddress);
        wc.accept("text/plain");

        assertEquals("avalue", wc.get(String.class));
    }

    @Test
    public void testGetBook123() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/webapp/resources/bookstore/books/123";
        URL url = new URL(endpointAddress);
        URLConnection connect = url.openConnection();
        connect.addRequestProperty("Accept", "application/json");
        connect.addRequestProperty("Content-Language", "badgerFishLanguage");
        InputStream in = connect.getInputStream();
        assertNotNull(in);

        //Ensure BadgerFish output as this should have replaced the standard JSONProvider
        InputStream expected = getClass()
            .getResourceAsStream("resources/expected_get_book123badgerfish.txt");

        assertEquals("BadgerFish output not correct",
                     stripXmlInstructionIfNeeded(getStringFromInputStream(expected).trim()),
                     stripXmlInstructionIfNeeded(getStringFromInputStream(in).trim()));
    }

    private String stripXmlInstructionIfNeeded(String str) {
        if (str != null && str.startsWith("<?xml")) {
            int index = str.indexOf("?>");
            str = str.substring(index + 2);
        }
        return str;
    }
    @Test
    public void testGetBookNotFound() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/webapp/resources/bookstore/books/12345";
        URL url = new URL(endpointAddress);
        HttpURLConnection connect = (HttpURLConnection)url.openConnection();
        connect.addRequestProperty("Accept", "text/plain,application/xml");
        assertEquals(500, connect.getResponseCode());
        InputStream in = connect.getErrorStream();
        assertNotNull(in);

        InputStream expected = getClass()
            .getResourceAsStream("resources/expected_get_book_notfound_mapped.txt");

        assertEquals("Exception is not mapped correctly",
                     stripXmlInstructionIfNeeded(getStringFromInputStream(expected).trim()),
                     stripXmlInstructionIfNeeded(getStringFromInputStream(in).trim()));
    }

    @Test
    public void testGetBookNotExistent() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/webapp/resources/bookstore/nonexistent";
        URL url = new URL(endpointAddress);
        HttpURLConnection connect = (HttpURLConnection)url.openConnection();
        connect.addRequestProperty("Accept", "application/xml");
        assertEquals(405, connect.getResponseCode());
        InputStream in = connect.getErrorStream();
        assertNotNull(in);

        assertEquals("Exception is not mapped correctly",
                     "StringTextWriter - Nonexistent method",
                     getStringFromInputStream(in).trim());
    }

    @Test
    public void testPostPetStatus() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/webapp/pets/petstore/pets";

        URL url = new URL(endpointAddress);
        HttpURLConnection httpUrlConnection = (HttpURLConnection)url.openConnection();

        httpUrlConnection.setUseCaches(false);
        httpUrlConnection.setDefaultUseCaches(false);
        httpUrlConnection.setDoOutput(true);
        httpUrlConnection.setDoInput(true);
        httpUrlConnection.setRequestMethod("POST");
        httpUrlConnection.setRequestProperty("Accept",   "text/xml");
        httpUrlConnection.setRequestProperty("Content-type",   "application/x-www-form-urlencoded");
        httpUrlConnection.setRequestProperty("Connection",   "close");

        try (OutputStream outputstream = httpUrlConnection.getOutputStream()) {
            IOUtils.copy(getClass().getResourceAsStream("resources/singleValPostBody.txt"), outputstream);
        }

        int responseCode = httpUrlConnection.getResponseCode();
        assertEquals(200, responseCode);
        assertEquals("Wrong status returned", "open", getStringFromInputStream(httpUrlConnection
            .getInputStream()));
        httpUrlConnection.disconnect();
    }

    @Test
    public void testPostPetStatus2() throws Exception {


        try (Socket s = new Socket("localhost", Integer.parseInt(PORT))) {
            IOUtils.copyAndCloseInput(getClass().getResource("resources/formRequest.txt").openStream(),
                                      s.getOutputStream());

            s.getOutputStream().flush();
            assertTrue("Wrong status returned", getStringFromInputStream(s.getInputStream())
                       .contains("open"));
        }
    }

    private String getStringFromInputStream(InputStream in) throws Exception {
        return IOUtils.toString(in);
    }


    @Test
    public void testPostPetStatusType() throws Exception {
        JAXBElementProvider<Object> p = new JAXBElementProvider<>();
        p.setUnmarshallAsJaxbElement(true);
        WebClient wc = WebClient.create("http://localhost:" + PORT + "/webapp/pets/petstore/jaxb/statusType/",
                                        Collections.singletonList(p));
        WebClient.getConfig(wc).getInInterceptors().add(new LoggingInInterceptor());
        wc.accept("text/xml");
        PetStore.PetStoreStatusType type = wc.get(PetStore.PetStoreStatusType.class);
        assertEquals(PetStore.CLOSED, type.getStatus());
    }
}
