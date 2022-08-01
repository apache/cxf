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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.HashSet;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.apache.cxf.test.TestUtilities;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.html.dom.HTMLAnchorElementImpl;
import org.apache.html.dom.HTMLDocumentImpl;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.cyberneko.html.parsers.DOMParser;

import static org.junit.Assert.assertEquals;

public abstract class AbstractServletTest extends AbstractBusClientServerTestBase {
    public static final String CONTEXT = "/mycontext";
    protected TestUtilities testUtilities;
    
    protected AbstractServletTest() {
        testUtilities = new TestUtilities(getClass());
        testUtilities.addDefaultNamespaces();
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

    protected CloseableHttpClient newClient() {
        return HttpClients.createDefault();
    }

    protected String uri(String path) {
        return "http://localhost:" + getPort() + CONTEXT + path;
    }
    
    protected Collection<HTMLAnchorElementImpl> getLinks(HTMLDocumentImpl document) {
        final Collection<HTMLAnchorElementImpl> links = new HashSet<>();

        for (int i = 0; i < document.getLinks().getLength(); ++i) {
            final HTMLAnchorElementImpl link = (HTMLAnchorElementImpl)document.getLinks().item(i); 
            links.add(link);
        }

        return links;
    }

    protected HTMLDocumentImpl parse(InputStream in) throws SAXException, IOException {
        final DOMParser parser = new DOMParser();
        parser.parse(new InputSource(in));
        return (HTMLDocumentImpl)parser.getDocument();
    }

    protected String getContentType(CloseableHttpResponse response) {
        return ContentType.parse(response.getFirstHeader(HttpHeaders.CONTENT_TYPE).getValue()).getMimeType();
    }
    
    protected String getCharset(CloseableHttpResponse response) {
        return ContentType.parse(response.getFirstHeader(HttpHeaders.CONTENT_TYPE).getValue()).getCharset().name();
    }

    protected abstract int getPort();

    /**
     * Here we expect an errorCode other than 200, and look for it checking for
     * text is omitted as it doesn't work. It would never work on java1.3, but
     * one may have expected java1.4+ to have access to the error stream in
     * responses. Clearly not.
     *
     * @param request
     * @param errorCode
     * @param errorText optional text string to search for
     * @throws MalformedURLException
     * @throws IOException
     * @throws SAXException
     */
    protected void expectErrorCode(HttpUriRequest request, int errorCode, String errorText)
        throws MalformedURLException, IOException, SAXException {
        String failureText = "Expected error " + errorCode + " from " + request.getRequestLine().getUri();

        try (CloseableHttpResponse response = newClient().execute(request)) {
            assertEquals(failureText, errorCode, response.getStatusLine().getStatusCode());
        }
    }
}
