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
package org.apache.cxf.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.namespace.QName;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerRegistry;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.wsdl11.ServiceWSDLBuilder;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This class provides unit test support for tests that look at generated WSDL
 * contents, as well as some test methods for invoking services.
 */
public class TestUtilities {

    private static String preKeepAlive;
    private static String basedirPath;
    protected Bus bus;
    protected Class<?> classpathAnchor;

    /**
     * Namespaces for the XPath expressions.
     */
    private Map<String, String> namespaces = new HashMap<>();

    /**
     * This class provides utilities to several conflicting inheritance stacks
     * of test support. Thus, it can't be a base class, and so can't use
     * getClass() to find resources. Users should pass getClass() to this
     * constructor instead.
     *
     * @param classpathReference
     */
    public TestUtilities(Class<?> classpathReference) {
        classpathAnchor = classpathReference;
    }

    public static void setKeepAliveSystemProperty(boolean setAlive) {
        preKeepAlive = System.getProperty("http.keepAlive");
        System.setProperty("http.keepAlive", Boolean.toString(setAlive));
    }

    public static void recoverKeepAliveSystemProperty() {
        if (preKeepAlive != null) {
            System.setProperty("http.keepAlive", preKeepAlive);
        } else {
            System.clearProperty("http.keepAlive");
        }
    }

    public void addDefaultNamespaces() {
        addNamespace("s", "http://schemas.xmlsoap.org/soap/envelope/");
        addNamespace("xsd", "http://www.w3.org/2001/XMLSchema");
        addNamespace("wsdl", "http://schemas.xmlsoap.org/wsdl/");
        addNamespace("wsdlsoap", "http://schemas.xmlsoap.org/wsdl/soap/");
        addNamespace("soap", "http://schemas.xmlsoap.org/soap/");
        addNamespace("soap12env", "http://www.w3.org/2003/05/soap-envelope");
        addNamespace("xml", "http://www.w3.org/XML/1998/namespace");
    }

    /**
     * Handy function for checking correctness of qualifies names in schema attribute values.
     * @param prefix
     * @param node
     * @return
     * @throws Exception
     */
    public String resolveNamespacePrefix(String prefix, Node node) throws Exception {
        String url = null;
        NodeList nodeNamespaces = this.assertValid("namespace::*", node);
        for (int x = 0; x < nodeNamespaces.getLength(); x++) {
            Attr nsAttr = (Attr)nodeNamespaces.item(x);
            String localName = nsAttr.getLocalName();
            if (localName.equals(prefix)) {
                url = nsAttr.getValue();
                break;
            }
        }
        return url;
    }

    /**
     * Assert that the following XPath query selects one or more nodes.
     *
     * @param xpath
     * @throws Exception
     */
    public NodeList assertValid(String xpath, Node node) throws Exception {
        return XPathAssert.assertValid(xpath, node, namespaces);
    }

    /**
     * Assert that the following XPath query selects a boolean value.
     *
     * @param xpath
     * @throws Exception
     */
    public void assertValidBoolean(String xpath, Node node) throws Exception {
        XPathAssert.assertValidBoolean(xpath, node, namespaces);
    }


    /**
     * Assert that the following XPath query selects no nodes.
     *
     * @param xpath
     */
    public NodeList assertInvalid(String xpath, Node node) throws Exception {
        return XPathAssert.assertInvalid(xpath, node, namespaces);
    }

    /**
     * Assert that the text of the xpath node retrieved is equal to the value
     * specified.
     *
     * @param xpath
     * @param value
     * @param node
     */
    public void assertXPathEquals(String xpath, String value, Node node) throws Exception {
        XPathAssert.assertXPathEquals(xpath, value, node, namespaces);
    }
    /**
     * Assert that the text of the xpath node retrieved is equal to the value
     * specified.
     *
     * @param xpath
     * @param value
     * @param node
     */
    public void assertXPathEquals(String xpath, QName value, Node node) throws Exception {
        XPathAssert.assertXPathEquals(xpath, value, node, namespaces);
    }

    /**
     * Assert that this node is not a Soap fault body.
     *
     * @param node
     * @throws Exception
     */
    public void assertNoFault(Node node) throws Exception {
        XPathAssert.assertNoFault(node);
    }

    public byte[] invokeBytes(String address, String transport, String message) throws Exception {
        EndpointInfo ei = new EndpointInfo(null, "http://schemas.xmlsoap.org/soap/http");
        ei.setAddress(address);

        ConduitInitiatorManager conduitMgr = getBus().getExtension(ConduitInitiatorManager.class);
        ConduitInitiator conduitInit = conduitMgr.getConduitInitiator(transport);
        Conduit conduit = conduitInit.getConduit(ei, getBus());

        TestMessageObserver obs = new TestMessageObserver();
        conduit.setMessageObserver(obs);

        Message m = new MessageImpl();
        conduit.prepare(m);

        OutputStream os = m.getContent(OutputStream.class);
        InputStream is = getResourceAsStream(message);
        if (is == null) {
            throw new RuntimeException("Could not find resource " + message);
        }

        IOUtils.copy(is, os);

        // TODO: shouldn't have to do this. IO caching needs cleaning
        // up or possibly removal...
        os.flush();
        is.close();
        os.close();

        return obs.getResponseStream().toByteArray();
    }

    public Node invoke(String address, String transport, String message) throws Exception {
        byte[] bs = invokeBytes(address, transport, message);

        ByteArrayInputStream input = new ByteArrayInputStream(bs);
        return StaxUtils.read(input);
    }

    public InputStream getResourceAsStream(String resource) {
        return classpathAnchor.getResourceAsStream(resource);
    }

    public Reader getResourceAsReader(String resource) {
        return new InputStreamReader(getResourceAsStream(resource), UTF_8);
    }

    public File getTestFile(String relativePath) {
        return new File(getBasedir(), relativePath);
    }

    public static String getBasedir() {
        if (basedirPath != null) {
            return basedirPath;
        }

        basedirPath = System.getProperty("basedir");

        if (basedirPath == null) {
            basedirPath = new File("").getAbsolutePath();
        }

        return basedirPath;
    }

    /**
     * Return a DOM tree for the WSDL for a server.
     *
     * @param server the server.
     * @return the DOM tree.
     * @throws WSDLException
     */
    public Document getWSDLDocument(Server server) throws WSDLException {
        Definition definition = getWSDLDefinition(server);
        WSDLWriter writer = WSDLFactory.newInstance().newWSDLWriter();
        return writer.getDocument(definition);
    }

    /**
     * Return a WSDL definition model for a server.
     *
     * @param server the server.
     * @return the definition.
     * @throws WSDLException
     */
    public Definition getWSDLDefinition(Server server) throws WSDLException {
        Service service = server.getEndpoint().getService();

        ServiceWSDLBuilder wsdlBuilder = new ServiceWSDLBuilder(bus, service.getServiceInfos().get(0));
        wsdlBuilder.setUseSchemaImports(false);
        return wsdlBuilder.build();
    }

    public Server getServerForService(QName serviceName) throws WSDLException {
        ServerRegistry svrMan = bus.getExtension(ServerRegistry.class);
        for (Server s : svrMan.getServers()) {
            Service svc = s.getEndpoint().getService();
            if (svc.getName().equals(serviceName)) {
                return s;
            }
        }
        return null;
    }

    public Server getServerForAddress(String address) throws WSDLException {
        ServerRegistry svrMan = bus.getExtension(ServerRegistry.class);
        for (Server s : svrMan.getServers()) {
            if (address.equals(s.getEndpoint().getEndpointInfo().getAddress())) {
                return s;
            }
        }
        return null;
    }

    public static class TestMessageObserver implements MessageObserver {
        ByteArrayOutputStream response = new ByteArrayOutputStream();
        boolean written;
        String contentType;

        public ByteArrayOutputStream getResponseStream() throws Exception {
            synchronized (this) {
                while (!written) {
                    wait(10 * 1000);
                }
            }
            return response;
        }

        public String getResponseContentType() {
            return contentType;
        }

        public void onMessage(Message message) {
            try {
                contentType = (String)message.get(Message.CONTENT_TYPE);
                InputStream is = message.getContent(InputStream.class);
                try {
                    IOUtils.copy(is, response);
                    is.close();
                    response.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } finally {
                synchronized (this) {
                    written = true;
                    notifyAll();
                }
            }
        }
    }

    /**
     * Add a namespace that will be used for XPath expressions.
     *
     * @param ns Namespace name.
     * @param uri The namespace uri.
     */
    public void addNamespace(String ns, String uri) {
        namespaces.put(ns, uri);
    }

    /**
     * retrieve the entire namespace map.
     *
     * @return
     */
    public Map<String, String> getNamespaces() {
        return namespaces;
    }

    /**
     * Return the CXF bus used.
     *
     * @return
     */
    public Bus getBus() {
        return bus;
    }

    /**
     * Set the CXF bus.
     *
     * @param bus
     */
    public void setBus(Bus bus) {
        this.bus = bus;
    }

    public static boolean checkUnrestrictedPoliciesInstalled() {
        boolean unrestrictedPoliciesInstalled = false;
        try {
            byte[] data = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07};

            SecretKey key192 = new SecretKeySpec(
                new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,     //NOPMD
                            0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f,
                            0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17},
                            "AES");
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.ENCRYPT_MODE, key192);
            c.doFinal(data);
            unrestrictedPoliciesInstalled = true;
        } catch (Exception e) {
            return unrestrictedPoliciesInstalled;
        }
        return unrestrictedPoliciesInstalled;
    }
}
