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

package org.apache.cxf.javascript;

import java.io.File;
import java.io.Reader;
import java.net.URL;
import java.util.Properties;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Document;

import org.apache.cxf.Bus;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.javascript.JavascriptTestUtilities.Notifier;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.test.AbstractCXFSpringTest;
import org.apache.cxf.test.XPathAssert;
import org.apache.cxf.testutil.common.TestUtil;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.support.GenericApplicationContext;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * This test is ignored by default as it is continually failing on Jenkins.
 */
public class JsHttpRequestTest extends AbstractCXFSpringTest {

    // shadow declaration from base class.
    private JavascriptTestUtilities testUtilities;

    public JsHttpRequestTest() throws Exception {
        testUtilities = new JavascriptTestUtilities(getClass());
        testUtilities.addDefaultNamespaces();
    }

    public void additionalSpringConfiguration(GenericApplicationContext applicationContext) throws Exception {
        // bring in some property values from a Properties file
        PropertyPlaceholderConfigurer cfg = new PropertyPlaceholderConfigurer();
        Properties properties = new Properties();
        properties.setProperty("staticResourceURL", getStaticResourceURL());
        cfg.setProperties(properties);
        // now actually do the replacement
        cfg.postProcessBeanFactory(applicationContext.getBeanFactory());

    }

    @Override
    protected String[] getConfigLocations() {
        TestUtil.getNewPortNumber(JsHttpRequestTest.class);
        return new String[] {"classpath:XMLHttpRequestTestBeans.xml"};
    }


    @Before
    public void setupRhino() throws Exception {
        testUtilities.setBus(getBean(Bus.class, "cxf"));
        testUtilities.initializeRhino();
        testUtilities.readResourceIntoRhino("/org/apache/cxf/javascript/XMLHttpRequestTests.js");
    }

    // just one test function to avoid muddles with engine startup/shutdown
    @Test
    public void runTests() throws Exception {
        testUtilities.rhinoCallExpectingExceptionInContext("SYNTAX_ERR", "testOpaqueURI");
        testUtilities.rhinoCallExpectingExceptionInContext("SYNTAX_ERR", "testNonAbsolute");
        testUtilities.rhinoCallExpectingExceptionInContext("SYNTAX_ERR", "testNonHttp");
        testUtilities.rhinoCallExpectingExceptionInContext("INVALID_STATE_ERR", "testSendNotOpenError");
        testUtilities.rhinoCallInContext("testStateNotificationSync");
        Notifier notifier = testUtilities.rhinoCallConvert("testAsyncHttpFetch1", Notifier.class);
        testUtilities.rhinoCallInContext("testAsyncHttpFetch2");
        boolean notified = notifier.waitForJavascript(2 * 10000);
        assertTrue(notified);
        assertTrue("HEADERS_RECEIVED", testUtilities.rhinoEvaluateConvert("asyncGotHeadersReceived", Boolean.class));
        assertTrue("LOADING", testUtilities.rhinoEvaluateConvert("asyncGotLoading", Boolean.class));
        assertTrue("DONE", testUtilities.rhinoEvaluateConvert("asyncGotDone", Boolean.class));
        String outOfOrder = testUtilities.rhinoEvaluateConvert("outOfOrderError", String.class);
        assertEquals("OutOfOrder", null, outOfOrder);
        assertEquals("status 200", Integer.valueOf(200),
                     testUtilities.rhinoEvaluateConvert("asyncStatus", Integer.class));
        assertEquals("status text", "OK",
                     testUtilities.rhinoEvaluateConvert("asyncStatusText", String.class));
        assertTrue("headers", testUtilities.rhinoEvaluateConvert("asyncResponseHeaders", String.class)
                   .contains("Content-Type: text/html"));
        Object httpObj = testUtilities.rhinoCallInContext("testSyncHttpFetch");
        assertNotNull(httpObj);
        assertTrue(httpObj instanceof String);
        String httpResponse = (String) httpObj;
        assertTrue(httpResponse.contains("Test"));
        Reader r = getResourceAsReader("/org/apache/cxf/javascript/XML_GreetMeDocLiteralReq.xml");
        String xml = IOUtils.toString(r);
        EndpointImpl endpoint = this.getBean(EndpointImpl.class, "greeter-service-endpoint");
        JsSimpleDomNode xmlResponse =
            testUtilities.rhinoCallConvert("testSyncXml",
                                           JsSimpleDomNode.class,
                                           testUtilities.javaToJS(endpoint.getAddress()),
                                           testUtilities.javaToJS(xml));
        assertNotNull(xmlResponse);
        Document doc = (Document)xmlResponse.getWrappedNode();
        testUtilities.addNamespace("t", "http://apache.org/hello_world_xml_http/wrapped/types");
        XPath textPath = XPathAssert.createXPath(testUtilities.getNamespaces());
        String nodeText = (String)textPath.evaluate("//t:responseType/text()", doc, XPathConstants.STRING);
        assertEquals("Hello \u05e9\u05dc\u05d5\u05dd", nodeText);
    }

    public String getStaticResourceURL() throws Exception {
        File staticFile = new File(this.getClass().getResource("test.html").toURI());
        staticFile = staticFile.getParentFile();
        staticFile = staticFile.getAbsoluteFile();
        URL furl = staticFile.toURI().toURL();
        return furl.toString();
    }
}
