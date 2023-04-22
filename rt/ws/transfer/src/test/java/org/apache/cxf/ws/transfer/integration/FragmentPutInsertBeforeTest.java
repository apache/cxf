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
package org.apache.cxf.ws.transfer.integration;

import javax.xml.stream.XMLStreamException;

import org.w3c.dom.Element;

import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.addressing.ReferenceParametersType;
import org.apache.cxf.ws.transfer.Put;
import org.apache.cxf.ws.transfer.PutResponse;
import org.apache.cxf.ws.transfer.Representation;
import org.apache.cxf.ws.transfer.dialect.fragment.ExpressionType;
import org.apache.cxf.ws.transfer.dialect.fragment.Fragment;
import org.apache.cxf.ws.transfer.dialect.fragment.FragmentDialectConstants;
import org.apache.cxf.ws.transfer.dialect.fragment.ValueType;
import org.apache.cxf.ws.transfer.manager.MemoryResourceManager;
import org.apache.cxf.ws.transfer.manager.ResourceManager;
import org.apache.cxf.ws.transfer.resource.Resource;

import org.junit.Assert;
import org.junit.Test;

public class FragmentPutInsertBeforeTest extends IntegrationBaseTest {

    @Test
    public void insertBefore1Test() throws XMLStreamException {
        String content = "<a><b/><c/></a>";
        ResourceManager resourceManager = new MemoryResourceManager();
        ReferenceParametersType refParams = resourceManager.create(getRepresentation(content));
        Server resource = createLocalResource(resourceManager);
        Resource client = createClient(refParams);

        Put request = new Put();
        request.setDialect(FragmentDialectConstants.FRAGMENT_2011_03_IRI);
        Fragment fragment = new Fragment();
        ExpressionType expression = new ExpressionType();
        expression.setLanguage(FragmentDialectConstants.XPATH10_LANGUAGE_IRI);
        expression.setMode(FragmentDialectConstants.FRAGMENT_MODE_INSERT_BEFORE);
        expression.getContent().add("/a/b");
        Element addedElement = DOMUtils.getEmptyDocument().createElement("d");
        ValueType value = new ValueType();
        value.getContent().add(addedElement);
        fragment.setExpression(expression);
        fragment.setValue(value);
        request.getAny().add(fragment);

        PutResponse response = client.put(request);
        Element rootEl = (Element) response.getRepresentation().getAny();
        Element child0 = (Element) rootEl.getChildNodes().item(0);
        Element child1 = (Element) rootEl.getChildNodes().item(1);
        Element child2 = (Element) rootEl.getChildNodes().item(2);
        Assert.assertEquals("a", rootEl.getNodeName());
        Assert.assertEquals("d", child0.getNodeName());
        Assert.assertEquals("b", child1.getNodeName());
        Assert.assertEquals("c", child2.getNodeName());

        resource.destroy();
    }

    @Test
    public void insertBefore2Test() throws XMLStreamException {
        String content = "<a><b/><b/></a>";
        ResourceManager resourceManager = new MemoryResourceManager();
        ReferenceParametersType refParams = resourceManager.create(getRepresentation(content));
        Server resource = createLocalResource(resourceManager);
        Resource client = createClient(refParams);

        Put request = new Put();
        request.setDialect(FragmentDialectConstants.FRAGMENT_2011_03_IRI);
        Fragment fragment = new Fragment();
        ExpressionType expression = new ExpressionType();
        expression.setLanguage(FragmentDialectConstants.XPATH10_LANGUAGE_IRI);
        expression.setMode(FragmentDialectConstants.FRAGMENT_MODE_INSERT_BEFORE);
        expression.getContent().add("/a/b[1]");
        Element addedElement = DOMUtils.getEmptyDocument().createElement("c");
        ValueType value = new ValueType();
        value.getContent().add(addedElement);
        fragment.setExpression(expression);
        fragment.setValue(value);
        request.getAny().add(fragment);

        PutResponse response = client.put(request);
        Element rootEl = (Element) response.getRepresentation().getAny();
        Element child0 = (Element) rootEl.getChildNodes().item(0);
        Element child1 = (Element) rootEl.getChildNodes().item(1);
        Element child2 = (Element) rootEl.getChildNodes().item(2);
        Assert.assertEquals("a", rootEl.getNodeName());
        Assert.assertEquals("c", child0.getNodeName());
        Assert.assertEquals("b", child1.getNodeName());
        Assert.assertEquals("b", child2.getNodeName());

        resource.destroy();
    }

    @Test
    public void insertBeforeEmptyDocTest() {
        ResourceManager resourceManager = new MemoryResourceManager();
        ReferenceParametersType refParams = resourceManager.create(new Representation());
        Server resource = createLocalResource(resourceManager);
        Resource client = createClient(refParams);

        Put request = new Put();
        request.setDialect(FragmentDialectConstants.FRAGMENT_2011_03_IRI);
        Fragment fragment = new Fragment();
        ExpressionType expression = new ExpressionType();
        expression.setLanguage(FragmentDialectConstants.XPATH10_LANGUAGE_IRI);
        expression.setMode(FragmentDialectConstants.FRAGMENT_MODE_INSERT_BEFORE);
        expression.getContent().add("/");
        Element addedElement = DOMUtils.getEmptyDocument().createElement("a");
        ValueType value = new ValueType();
        value.getContent().add(addedElement);
        fragment.setExpression(expression);
        fragment.setValue(value);
        request.getAny().add(fragment);

        PutResponse response = client.put(request);
        Element rootEl = (Element) response.getRepresentation().getAny();
        Assert.assertEquals("a", rootEl.getNodeName());

        resource.destroy();
    }

    @Test(expected = SOAPFaultException.class)
    public void insertBeforeRootTest() throws XMLStreamException {
        String content = "<a/>";
        ResourceManager resourceManager = new MemoryResourceManager();
        ReferenceParametersType refParams = resourceManager.create(getRepresentation(content));
        Server resource = createLocalResource(resourceManager);
        Resource client = createClient(refParams);

        Put request = new Put();
        request.setDialect(FragmentDialectConstants.FRAGMENT_2011_03_IRI);
        Fragment fragment = new Fragment();
        ExpressionType expression = new ExpressionType();
        expression.setLanguage(FragmentDialectConstants.XPATH10_LANGUAGE_IRI);
        expression.setMode(FragmentDialectConstants.FRAGMENT_MODE_INSERT_BEFORE);
        expression.getContent().add("/");
        Element addedElement = DOMUtils.getEmptyDocument().createElement("b");
        ValueType value = new ValueType();
        value.getContent().add(addedElement);
        fragment.setExpression(expression);
        fragment.setValue(value);
        request.getAny().add(fragment);

        client.put(request);

        resource.destroy();
    }

    @Test(expected = SOAPFaultException.class)
    public void insertBeforeAttrTest() throws XMLStreamException {
        String content = "<a foo=\"1\"/>";
        ResourceManager resourceManager = new MemoryResourceManager();
        ReferenceParametersType refParams = resourceManager.create(getRepresentation(content));
        Server resource = createLocalResource(resourceManager);
        Resource client = createClient(refParams);

        Put request = new Put();
        request.setDialect(FragmentDialectConstants.FRAGMENT_2011_03_IRI);
        Fragment fragment = new Fragment();
        ExpressionType expression = new ExpressionType();
        expression.setLanguage(FragmentDialectConstants.XPATH10_LANGUAGE_IRI);
        expression.setMode(FragmentDialectConstants.FRAGMENT_MODE_INSERT_BEFORE);
        expression.getContent().add("/a/@f");
        Element addedElement = DOMUtils.getEmptyDocument().createElement("b");
        ValueType value = new ValueType();
        value.getContent().add(addedElement);
        fragment.setExpression(expression);
        fragment.setValue(value);
        request.getAny().add(fragment);

        client.put(request);

        resource.destroy();
    }
}
