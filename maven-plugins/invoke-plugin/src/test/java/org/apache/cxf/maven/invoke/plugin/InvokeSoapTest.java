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
package org.apache.cxf.maven.invoke.plugin;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Properties;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.Service;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.feature.LoggingFeature;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.easymock.EasyMock;
import org.easymock.EasyMockRule;
import org.easymock.Mock;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Service.class)
public class InvokeSoapTest {

    private static final Document NOT_USED_DOCUMENT = null;

    private static final Service NOT_USED_SERVICE = null;

    @Rule
    public EasyMockRule easyMock = new EasyMockRule(this);

    @Rule
    public TemporaryFolder workdir = new TemporaryFolder();

    @Mock
    private Document document;

    @Mock
    private Node header;

    @Mock
    private Node node;

    @Mock
    private MavenProject project;

    @Mock
    private XPathExpression repeatUntilExpression;

    @Mock
    private Service service;

    private final QName somePort = new QName("test:namespace", "test-port");

    @Test
    public void shouldAddLoggingFeatureWhenInDebugModeAndCreatingServices()
            throws MojoExecutionException, MalformedURLException {
        final InvokeSoap invokeSoap = new InvokeSoap();
        final Log log = EasyMock.createMock(Log.class);
        invokeSoap.setLog(log);

        final URI wsdl = URI.create("file:uri:wsdl");

        invokeSoap.wsdl = wsdl;
        invokeSoap.namespace = "test:namespace";
        invokeSoap.serviceName = "test-service";

        mockStatic(Service.class);

        expect(log.isDebugEnabled()).andReturn(true);

        expect(Service.create(eq(wsdl.toURL()), eq(new QName("test:namespace", "test-service")),
                isA(LoggingFeature.class))).andReturn(service);

        PowerMock.replay(Service.class);

        replay(log);

        final Service created = invokeSoap.createService();

        assertSame(service, created);
    }

    @Test(expected = MojoExecutionException.class)
    public void shouldComplainIfMorePortsAreInServceAndNoSpecificPortIsDefined() throws MojoExecutionException {
        final InvokeSoap invokeSoap = new InvokeSoap();

        expect(service.getPorts()).andReturn(asList(somePort, somePort).iterator());

        replay(service);

        invokeSoap.determinePort(service);
    }

    @Test
    public void shouldCreateRequest() {
        final Source source = InvokeSoap.createRequest(node);

        assertThat("Should create DOMSource from given node", source, instanceOf(DOMSource.class));

        assertSame("Should contain the same given node", node, ((DOMSource) source).getNode());
    }

    @Test
    public void shouldCreateServices() throws MojoExecutionException, MalformedURLException {
        final InvokeSoap invokeSoap = new InvokeSoap();
        final URI wsdl = URI.create("file:uri:wsdl");

        invokeSoap.wsdl = wsdl;
        invokeSoap.namespace = "test:namespace";
        invokeSoap.serviceName = "test-service";

        mockStatic(Service.class);

        expect(Service.create(wsdl.toURL(), new QName("test:namespace", "test-service"))).andReturn(service);

        PowerMock.replay(Service.class);

        final Service created = invokeSoap.createService();

        assertSame(service, created);
    }

    @Test
    public void shouldCreateServicesWithHeaders() throws MojoExecutionException, MalformedURLException {
        final InvokeSoap invokeSoap = new InvokeSoap();
        final URI wsdl = URI.create("file:uri:wsdl");

        invokeSoap.wsdl = wsdl;
        invokeSoap.namespace = "test:namespace";
        invokeSoap.serviceName = "test-service";
        invokeSoap.headers = new Node[] {header};

        mockStatic(Service.class);

        expect(Service.create(wsdl.toURL(), new QName("test:namespace", "test-service"))).andReturn(service);
        service.setHandlerResolver(isA(HeadersHandlerResolver.class));
        expectLastCall().andVoid();

        PowerMock.replay(Service.class);

        replay(service);

        final Service created = invokeSoap.createService();

        assertSame(service, created);

        verify(service);
    }

    @Test
    public void shouldExtractPropertiesIfRequested() throws MojoExecutionException {
        final InvokeSoap invokeSoap = new InvokeSoap();
        invokeSoap.project = project;

        invokeSoap.properties.put("property1", "/element");
        invokeSoap.properties.put("property2", "/element/@attribute");

        final Document response = XmlUtil.document();
        final Element element = response.createElement("element");
        element.setAttribute("attribute", "value");
        element.appendChild(response.createTextNode("text"));
        response.appendChild(element);

        final Properties properties = new Properties();
        expect(project.getProperties()).andReturn(properties);

        replay(project);

        invokeSoap.extractProperties(response);

        assertEquals("Should extract two properties by the given XPaths", 2, properties.size());
        assertEquals("Should extract element property", properties.getProperty("property1"), "text");
        assertEquals("Should extract attribute property", properties.getProperty("property2"), "value");

        verify(project);
    }

    @Test
    public void shouldNotExtractPropertiesIfNoneRequested() throws MojoExecutionException {
        final InvokeSoap invokeSoap = new InvokeSoap();

        invokeSoap.extractProperties(NOT_USED_DOCUMENT);
    }

    @Test
    public void shouldNotRepeatIfNoRepeatExpressionIsGiven() throws MojoExecutionException {
        final InvokeSoap invokeSoap = new InvokeSoap();

        assertFalse("Should not repeat, because no repeatExpression was given",
                invokeSoap.shouldRepeat(NOT_USED_DOCUMENT));
    }

    @Test
    public void shouldNotRepeatIfRepeatExpressionYieldsFalse() throws MojoExecutionException, XPathExpressionException {
        final InvokeSoap invokeSoap = new InvokeSoap();
        invokeSoap.repeatUntilExpression = repeatUntilExpression;

        expect(repeatUntilExpression.evaluate(document, XPathConstants.BOOLEAN)).andReturn(false);

        replay(repeatUntilExpression);

        assertFalse("Should repeat, because repeatExpression yields true", invokeSoap.shouldRepeat(document));

        verify(repeatUntilExpression);
    }

    @Test
    public void shouldRepeatIfRepeatExpressionYieldsTrue() throws MojoExecutionException, XPathExpressionException {
        final InvokeSoap invokeSoap = new InvokeSoap();
        invokeSoap.repeatUntilExpression = repeatUntilExpression;

        expect(repeatUntilExpression.evaluate(document, XPathConstants.BOOLEAN)).andReturn(true);

        replay(repeatUntilExpression);

        assertTrue("Should repeat, because repeatExpression yields true", invokeSoap.shouldRepeat(document));

        verify(repeatUntilExpression);
    }

    @Test
    public void shouldUseTheGivenPortName() throws MojoExecutionException {
        final InvokeSoap invokeSoap = new InvokeSoap();

        invokeSoap.namespace = "test:namespace";
        invokeSoap.portName = "test-port";

        final QName port = invokeSoap.determinePort(NOT_USED_SERVICE);

        assertEquals("Should use the port specified by the parameter", port, port);
    }

    @Test
    public void shouldUseTheSinglePortInServceIfNoSpecificPortIsDefined() throws MojoExecutionException {
        final InvokeSoap invokeSoap = new InvokeSoap();

        expect(service.getPorts()).andReturn(singleton(somePort).iterator());

        replay(service);

        final QName port = invokeSoap.determinePort(service);

        assertEquals("Should use the only port defined in service", port, port);

        verify(service);
    }
}
