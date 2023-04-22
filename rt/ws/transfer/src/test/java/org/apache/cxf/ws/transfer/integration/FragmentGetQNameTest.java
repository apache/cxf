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
import jakarta.xml.ws.soap.SOAPFaultException;
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

public class FragmentGetQNameTest extends IntegrationBaseTest {

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
        expression.setLanguage(FragmentDialectConstants.QNAME_LANGUAGE_IRI);
        expression.getContent().add("a");
        request.getAny().add(objectFactory.createExpression(expression));

        GetResponse response = client.get(request);
        ValueType value = getValue(response);
        Assert.assertEquals(1, value.getContent().size());
        Assert.assertEquals("a", ((Element)value.getContent().get(0)).getLocalName());

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
        expression.setLanguage(FragmentDialectConstants.QNAME_LANGUAGE_IRI);
        expression.getContent().add("ns:a");
        request.getAny().add(objectFactory.createExpression(expression));

        GetResponse response = client.get(request);
        ValueType value = getValue(response);
        Assert.assertEquals(1, value.getContent().size());
        Assert.assertEquals("a", ((Element)value.getContent().get(0)).getLocalName());
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
        expression.setLanguage(FragmentDialectConstants.QNAME_LANGUAGE_IRI);
        expression.getContent().add("c");
        request.getAny().add(objectFactory.createExpression(expression));

        GetResponse response = client.get(request);
        ValueType value = getValue(response);
        Assert.assertEquals(0, value.getContent().size());

        resource.destroy();
    }

    @Test
    public void getMoreValuesTest() throws XMLStreamException {
        String content = "<root><b>Text1</b><b>Text2</b><b>Text3</b></root>";
        ResourceManager resourceManager = new MemoryResourceManager();
        ReferenceParametersType refParams = resourceManager.create(getRepresentation(content));
        Server resource = createLocalResource(resourceManager);
        Resource client = createClient(refParams);

        ObjectFactory objectFactory = new ObjectFactory();

        Get request = new Get();
        request.setDialect(FragmentDialectConstants.FRAGMENT_2011_03_IRI);
        ExpressionType expression = new ExpressionType();
        expression.setLanguage(FragmentDialectConstants.QNAME_LANGUAGE_IRI);
        expression.getContent().add("b");
        request.getAny().add(objectFactory.createExpression(expression));

        GetResponse response = client.get(request);
        ValueType value = getValue(response);
        Assert.assertEquals(3, value.getContent().size());
        Assert.assertEquals("b", ((Element)value.getContent().get(0)).getLocalName());
        Assert.assertEquals("b", ((Element)value.getContent().get(1)).getLocalName());
        Assert.assertEquals("b", ((Element)value.getContent().get(2)).getLocalName());

        resource.destroy();
    }

    @Test(expected = SOAPFaultException.class)
    public void getWrongQNameTest() throws XMLStreamException {
        String content = "<root><a><b>Text1</b><b>Text2</b><b>Text3</b></a></root>";
        ResourceManager resourceManager = new MemoryResourceManager();
        ReferenceParametersType refParams = resourceManager.create(getRepresentation(content));
        createLocalResource(resourceManager);
        Resource client = createClient(refParams);

        ObjectFactory objectFactory = new ObjectFactory();

        Get request = new Get();
        request.setDialect(FragmentDialectConstants.FRAGMENT_2011_03_IRI);
        ExpressionType expression = new ExpressionType();
        expression.setLanguage(FragmentDialectConstants.QNAME_LANGUAGE_IRI);
        expression.getContent().add("//b");
        request.getAny().add(objectFactory.createExpression(expression));

        client.get(request);
    }

    private static ValueType getValue(GetResponse response) {
        @SuppressWarnings("unchecked")
        JAXBElement<ValueType> jaxb = (JAXBElement<ValueType>) response.getAny().get(0);
        return jaxb.getValue();
    }

}
