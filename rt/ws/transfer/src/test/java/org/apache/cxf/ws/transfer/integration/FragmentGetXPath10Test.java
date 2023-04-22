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

import jakarta.xml.bind.JAXBElement;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.ws.addressing.ReferenceParametersType;
import org.apache.cxf.ws.transfer.Get;
import org.apache.cxf.ws.transfer.GetResponse;
import org.apache.cxf.ws.transfer.dialect.fragment.ExpressionType;
import org.apache.cxf.ws.transfer.dialect.fragment.FragmentDialectConstants;
import org.apache.cxf.ws.transfer.dialect.fragment.ObjectFactory;
import org.apache.cxf.ws.transfer.dialect.fragment.ValueType;
import org.apache.cxf.ws.transfer.manager.MemoryResourceManager;
import org.apache.cxf.ws.transfer.manager.ResourceManager;
import org.apache.cxf.ws.transfer.resource.Resource;

import org.junit.Assert;
import org.junit.Test;

public class FragmentGetXPath10Test extends IntegrationBaseTest {

    @Test
    public void getTest() throws XMLStreamException {
        String content = "<root><a><b>Text</b></a></root>";
        ResourceManager resourceManager = new MemoryResourceManager();
        ReferenceParametersType refParams = resourceManager.create(getRepresentation(content));
        Server resource = createLocalResource(resourceManager);
        Resource client = createClient(refParams);

        ObjectFactory objectFactory = new ObjectFactory();

        Get request = new Get();
        request.setDialect(FragmentDialectConstants.FRAGMENT_2011_03_IRI);
        ExpressionType expression = new ExpressionType();
        expression.setLanguage(FragmentDialectConstants.XPATH10_LANGUAGE_IRI);
        expression.getContent().add("/root/a/b");
        request.getAny().add(objectFactory.createExpression(expression));

        GetResponse response = client.get(request);
        ValueType value = getValue(response);
        Assert.assertEquals(1, value.getContent().size());
        Assert.assertEquals("b", ((Element)value.getContent().get(0)).getLocalName());

        resource.destroy();
    }

    @Test
    public void getImpliedLanguageTest() throws XMLStreamException {
        String content = "<root><a><b>Text</b></a></root>";
        ResourceManager resourceManager = new MemoryResourceManager();
        ReferenceParametersType refParams = resourceManager.create(getRepresentation(content));
        Server resource = createLocalResource(resourceManager);
        Resource client = createClient(refParams);

        ObjectFactory objectFactory = new ObjectFactory();

        Get request = new Get();
        request.setDialect(FragmentDialectConstants.FRAGMENT_2011_03_IRI);
        ExpressionType expression = new ExpressionType();
        expression.getContent().add("/root/a/b");
        request.getAny().add(objectFactory.createExpression(expression));

        GetResponse response = client.get(request);
        ValueType value = getValue(response);
        Assert.assertEquals(1, value.getContent().size());
        Assert.assertEquals("b", ((Element)value.getContent().get(0)).getLocalName());

        resource.destroy();
    }

    @Test
    public void getWithNamespaceTest() throws XMLStreamException {
        String content = "<ns:root xmlns:ns=\"www.example.org\"><ns:a><ns:b>Text</ns:b></ns:a></ns:root>";
        ResourceManager resourceManager = new MemoryResourceManager();
        ReferenceParametersType refParams = resourceManager.create(getRepresentation(content));
        Server resource = createLocalResource(resourceManager);
        Resource client = createClient(refParams);

        ObjectFactory objectFactory = new ObjectFactory();

        Get request = new Get();
        request.setDialect(FragmentDialectConstants.FRAGMENT_2011_03_IRI);
        ExpressionType expression = new ExpressionType();
        expression.setLanguage(FragmentDialectConstants.XPATH10_LANGUAGE_IRI);
        expression.getContent().add("/ns:root/ns:a/ns:b");
        request.getAny().add(objectFactory.createExpression(expression));

        GetResponse response = client.get(request);
        ValueType value = getValue(response);
        Assert.assertEquals(1, value.getContent().size());
        Assert.assertEquals("b", ((Element)value.getContent().get(0)).getLocalName());
        Assert.assertEquals("www.example.org", ((Element)value.getContent().get(0)).getNamespaceURI());

        resource.destroy();
    }

    @Test
    public void qetEmptyResultTest() throws XMLStreamException {
        String content = "<root><a><b>Text</b></a></root>";
        ResourceManager resourceManager = new MemoryResourceManager();
        ReferenceParametersType refParams = resourceManager.create(getRepresentation(content));
        Server resource = createLocalResource(resourceManager);
        Resource client = createClient(refParams);

        ObjectFactory objectFactory = new ObjectFactory();

        Get request = new Get();
        request.setDialect(FragmentDialectConstants.FRAGMENT_2011_03_IRI);
        ExpressionType expression = new ExpressionType();
        expression.setLanguage(FragmentDialectConstants.XPATH10_LANGUAGE_IRI);
        expression.getContent().add("//c");
        request.getAny().add(objectFactory.createExpression(expression));

        GetResponse response = client.get(request);
        ValueType value = getValue(response);
        Assert.assertEquals(0, value.getContent().size());

        resource.destroy();
    }

    @Test
    public void getMoreValuesTest() throws XMLStreamException {
        String content = "<root><a><b>Text1</b><b>Text2</b><b>Text3</b></a></root>";
        ResourceManager resourceManager = new MemoryResourceManager();
        ReferenceParametersType refParams = resourceManager.create(getRepresentation(content));
        Server resource = createLocalResource(resourceManager);
        Resource client = createClient(refParams);

        ObjectFactory objectFactory = new ObjectFactory();

        Get request = new Get();
        request.setDialect(FragmentDialectConstants.FRAGMENT_2011_03_IRI);
        ExpressionType expression = new ExpressionType();
        expression.setLanguage(FragmentDialectConstants.XPATH10_LANGUAGE_IRI);
        expression.getContent().add("//b");
        request.getAny().add(objectFactory.createExpression(expression));

        GetResponse response = client.get(request);
        ValueType value = getValue(response);
        Assert.assertEquals(3, value.getContent().size());
        Assert.assertEquals("b", ((Element)value.getContent().get(0)).getLocalName());
        Assert.assertEquals("b", ((Element)value.getContent().get(1)).getLocalName());
        Assert.assertEquals("b", ((Element)value.getContent().get(2)).getLocalName());

        resource.destroy();
    }

    @Test
    public void getMoreValues2Test() throws XMLStreamException {
        String content = "<root><a><b>Text1</b><b>Text2</b><b><b>Text3</b></b></a></root>";
        ResourceManager resourceManager = new MemoryResourceManager();
        ReferenceParametersType refParams = resourceManager.create(getRepresentation(content));
        Server resource = createLocalResource(resourceManager);
        Resource client = createClient(refParams);

        ObjectFactory objectFactory = new ObjectFactory();

        Get request = new Get();
        request.setDialect(FragmentDialectConstants.FRAGMENT_2011_03_IRI);
        ExpressionType expression = new ExpressionType();
        expression.setLanguage(FragmentDialectConstants.XPATH10_LANGUAGE_IRI);
        expression.getContent().add("//b");
        request.getAny().add(objectFactory.createExpression(expression));

        GetResponse response = client.get(request);
        ValueType value = getValue(response);
        Assert.assertEquals(1, value.getContent().size());
        Assert.assertEquals("b", ((Element)value.getContent().get(0)).getLocalName());

        resource.destroy();
    }

    @Test
    public void getAttrTest() throws XMLStreamException {
        String content = "<root><a><b attr1=\"value1\">Text</b></a></root>";
        ResourceManager resourceManager = new MemoryResourceManager();
        ReferenceParametersType refParams = resourceManager.create(getRepresentation(content));
        Server resource = createLocalResource(resourceManager);
        Resource client = createClient(refParams);

        ObjectFactory objectFactory = new ObjectFactory();

        Get request = new Get();
        request.setDialect(FragmentDialectConstants.FRAGMENT_2011_03_IRI);
        ExpressionType expression = new ExpressionType();
        expression.setLanguage(FragmentDialectConstants.XPATH10_LANGUAGE_IRI);
        expression.getContent().add("/root/a/b/@attr1");
        request.getAny().add(objectFactory.createExpression(expression));

        GetResponse response = client.get(request);
        ValueType value = getValue(response);
        Assert.assertEquals(1, value.getContent().size());
        Assert.assertTrue(value.getContent().get(0) instanceof Element);
        Element attrEl = (Element) value.getContent().get(0);
        Assert.assertEquals(FragmentDialectConstants.FRAGMENT_ATTR_NODE_NAME, attrEl.getLocalName());
        Assert.assertEquals(FragmentDialectConstants.FRAGMENT_2011_03_IRI, attrEl.getNamespaceURI());
        Assert.assertEquals("attr1", attrEl.getAttribute(FragmentDialectConstants.FRAGMENT_ATTR_NODE_NAME_ATTR));
        Assert.assertEquals("value1", attrEl.getTextContent());

        resource.destroy();
    }

    @Test
    public void getAttrNSTest() throws XMLStreamException {
        String content = "<root xmlns:ns=\"www.example.org\"><a><b ns:attr1=\"value1\">Text</b></a></root>";
        ResourceManager resourceManager = new MemoryResourceManager();
        ReferenceParametersType refParams = resourceManager.create(getRepresentation(content));
        Server resource = createLocalResource(resourceManager);
        Resource client = createClient(refParams);

        ObjectFactory objectFactory = new ObjectFactory();

        Get request = new Get();
        request.setDialect(FragmentDialectConstants.FRAGMENT_2011_03_IRI);
        ExpressionType expression = new ExpressionType();
        expression.setLanguage(FragmentDialectConstants.XPATH10_LANGUAGE_IRI);
        expression.getContent().add("/root/a/b/@ns:attr1");
        request.getAny().add(objectFactory.createExpression(expression));

        GetResponse response = client.get(request);
        ValueType value = getValue(response);
        Assert.assertEquals(1, value.getContent().size());
        Assert.assertTrue(value.getContent().get(0) instanceof Element);
        Element attrEl = (Element) value.getContent().get(0);
        Assert.assertEquals(FragmentDialectConstants.FRAGMENT_ATTR_NODE_NAME, attrEl.getLocalName());
        Assert.assertEquals(FragmentDialectConstants.FRAGMENT_2011_03_IRI, attrEl.getNamespaceURI());
        Assert.assertEquals("ns:attr1", attrEl.getAttribute(FragmentDialectConstants.FRAGMENT_ATTR_NODE_NAME_ATTR));
        Assert.assertEquals("value1", attrEl.getTextContent());

        resource.destroy();
    }

    @Test
    public void getNumberTest() throws XMLStreamException {
        String content = "<root><a><b>Text</b><b>Text2</b></a></root>";
        ResourceManager resourceManager = new MemoryResourceManager();
        ReferenceParametersType refParams = resourceManager.create(getRepresentation(content));
        Server resource = createLocalResource(resourceManager);
        Resource client = createClient(refParams);

        ObjectFactory objectFactory = new ObjectFactory();

        Get request = new Get();
        request.setDialect(FragmentDialectConstants.FRAGMENT_2011_03_IRI);
        ExpressionType expression = new ExpressionType();
        expression.setLanguage(FragmentDialectConstants.XPATH10_LANGUAGE_IRI);
        expression.getContent().add("count(//b)");
        request.getAny().add(objectFactory.createExpression(expression));

        GetResponse response = client.get(request);
        ValueType value = getValue(response);
        Assert.assertEquals(1, value.getContent().size());
        Assert.assertEquals("2", value.getContent().get(0));

        resource.destroy();
    }

    @Test
    public void getBooleanTrueTest() throws XMLStreamException {
        String content = "<root><a><b>Text</b><b>Text2</b></a></root>";
        ResourceManager resourceManager = new MemoryResourceManager();
        ReferenceParametersType refParams = resourceManager.create(getRepresentation(content));
        Server resource = createLocalResource(resourceManager);
        Resource client = createClient(refParams);

        ObjectFactory objectFactory = new ObjectFactory();

        Get request = new Get();
        request.setDialect(FragmentDialectConstants.FRAGMENT_2011_03_IRI);
        ExpressionType expression = new ExpressionType();
        expression.setLanguage(FragmentDialectConstants.XPATH10_LANGUAGE_IRI);
        expression.getContent().add("count(//b) = 2");
        request.getAny().add(objectFactory.createExpression(expression));

        GetResponse response = client.get(request);
        ValueType value = getValue(response);
        Assert.assertEquals(1, value.getContent().size());
        Assert.assertEquals("true", value.getContent().get(0));

        resource.destroy();
    }

    @Test
    public void getBooleanFalseTest() throws XMLStreamException {
        String content = "<root><a><b>Text</b><b>Text2</b></a></root>";
        ResourceManager resourceManager = new MemoryResourceManager();
        ReferenceParametersType refParams = resourceManager.create(getRepresentation(content));
        Server resource = createLocalResource(resourceManager);
        Resource client = createClient(refParams);

        ObjectFactory objectFactory = new ObjectFactory();

        Get request = new Get();
        request.setDialect(FragmentDialectConstants.FRAGMENT_2011_03_IRI);
        ExpressionType expression = new ExpressionType();
        expression.setLanguage(FragmentDialectConstants.XPATH10_LANGUAGE_IRI);
        expression.getContent().add("count(//b) != 2");
        request.getAny().add(objectFactory.createExpression(expression));

        GetResponse response = client.get(request);
        ValueType value = getValue(response);
        Assert.assertEquals(1, value.getContent().size());
        Assert.assertEquals("false", value.getContent().get(0));

        resource.destroy();
    }

    private static ValueType getValue(GetResponse response) {
        @SuppressWarnings("unchecked")
        JAXBElement<ValueType> jaxb = (JAXBElement<ValueType>) response.getAny().get(0);
        return jaxb.getValue();
    }

}
