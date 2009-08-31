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

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

import javax.wsdl.WSDLException;
import javax.xml.namespace.NamespaceContext;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.helpers.MapNamespaceContext;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;


/**
 * A basic test case meant for helping users unit test their services.
 * @see TestUtilities
 */
@org.junit.Ignore
public class AbstractCXFTest extends Assert {
    
    protected TestUtilities testUtilities;
    protected Bus bus;
    
    protected AbstractCXFTest() {
        testUtilities = new TestUtilities(getClass());
        testUtilities.addDefaultNamespaces();
    }
    
    @Before
    public void setUpBus() throws Exception {
        if (bus == null) {
            bus = createBus();
            testUtilities.setBus(bus);
        }
    }
    
    public Bus getBus() {
        return bus;
    }
    
    @After
    public void shutdownBus() {       
        if (bus != null) {
            bus.shutdown(false);
            bus = null;
        } 
        BusFactory.setDefaultBus(null);
    }


    protected Bus createBus() throws BusException {
        return BusFactory.newInstance().createBus();
    }

    protected byte[] invokeBytes(String address, 
                                 String transport,
                                 String message) throws Exception {
        return testUtilities.invokeBytes(address, transport, message);
    }
    
    protected Node invoke(String address, 
                          String transport,
                          String message) throws Exception {
        return testUtilities.invoke(address, transport, message);
    }

    protected Node invoke(String address, 
                          String transport,
                          byte[] message) throws Exception {
        return testUtilities.invoke(address, transport, message);
    }

    /**
     * Assert that the following XPath query selects one or more nodes.
     * 
     * @param xpath
     * @throws Exception 
     */
    public NodeList assertValid(String xpath, Node node) throws Exception {
        return testUtilities.assertValid(xpath, node);
    }

    /**
     * Assert that the following XPath query selects a boolean value.
     * 
     * @param xpath
     * @throws Exception 
     */
    public void assertValidBoolean(String xpath, Node node) throws Exception {
        testUtilities.assertValidBoolean(xpath, node);
    }
    /**
     * Assert that the following XPath query selects no nodes.
     * 
     * @param xpath
     */
    public NodeList assertInvalid(String xpath, Node node) throws Exception {
        return testUtilities.assertInvalid(xpath, node);
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
        testUtilities.assertXPathEquals(xpath, value, node);
    }

    /**
     * Assert that this node is not a SOAP fault part.
     * @param node
     * @throws Exception
     */
    public void assertNoFault(Node node) throws Exception {
        testUtilities.assertNoFault(node);
    }
    
    /**
     * Add a namespace that will be used for XPath expressions.
     * 
     * @param ns Namespace name.
     * @param uri The namespace uri.
     */
    public void addNamespace(String ns, String uri) {
        testUtilities.addNamespace(ns, uri);
    }
    
    public NamespaceContext getNamespaceContext() {
        return new MapNamespaceContext(testUtilities.getNamespaces());
    }

    public Map<String, String> getNamespaces() {
        return testUtilities.getNamespaces();
    }

    protected InputStream getResourceAsStream(String resource) {
        return testUtilities.getResourceAsStream(resource);
    }

    protected Reader getResourceAsReader(String resource) {
        return testUtilities.getResourceAsReader(resource);
    }

    public File getTestFile(String relativePath) {
        return testUtilities.getTestFile(relativePath);
    }

    public static String getBasedir() {
        return TestUtilities.getBasedir();
    }

    protected Document getWSDLDocument(Server server) throws WSDLException {
        return testUtilities.getWSDLDocument(server);
    }
    
    public static class TestMessageObserver extends TestUtilities.TestMessageObserver {

        public TestMessageObserver() {
            super();
        }
    }
}
