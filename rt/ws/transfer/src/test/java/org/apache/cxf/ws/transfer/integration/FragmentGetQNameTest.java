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

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.JAXBElement;
import javax.xml.ws.soap.SOAPFaultException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.addressing.ReferenceParametersType;
import org.apache.cxf.ws.transfer.Get;
import org.apache.cxf.ws.transfer.GetResponse;
import org.apache.cxf.ws.transfer.Representation;
import org.apache.cxf.ws.transfer.dialect.fragment.ExpressionType;
import org.apache.cxf.ws.transfer.dialect.fragment.FragmentDialectConstants;
import org.apache.cxf.ws.transfer.dialect.fragment.ObjectFactory;
import org.apache.cxf.ws.transfer.dialect.fragment.ValueType;
import org.apache.cxf.ws.transfer.manager.MemoryResourceManager;
import org.apache.cxf.ws.transfer.manager.ResourceManager;
import org.apache.cxf.ws.transfer.resource.Resource;
import org.apache.cxf.ws.transfer.shared.TransferTools;
import org.apache.cxf.ws.transfer.shared.handlers.ReferenceParameterAddingHandler;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author erich
 */
public class FragmentGetQNameTest extends IntegrationBaseTest {
    
    private Resource createClient(ReferenceParametersType refParams) {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        
        Map<String, Object> props = factory.getProperties();
        if (props == null) {
            props = new HashMap<String, Object>();
        }
        props.put("jaxb.additionalContextClasses",
                ExpressionType.class);
        factory.setProperties(props);
        
        factory.setBus(bus);
        factory.setServiceClass(Resource.class);
        factory.setAddress(RESOURCE_ADDRESS);
        factory.getHandlers().add(new ReferenceParameterAddingHandler(refParams));
        factory.getInInterceptors().add(logInInterceptor);
        factory.getOutInterceptors().add(logOutInterceptor);
        return (Resource) factory.create();
    }
    
    private Representation getRepresentation(String content) {
        Document doc = TransferTools.parse(new InputSource(new StringReader(content)));
        Representation representation = new Representation();
        representation.setAny(doc.getDocumentElement());
        return representation;
    }
    
    @Test
    public void getTest() {
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
        ValueType value = getValue(response.getRepresentation());
        Assert.assertEquals(1, value.getAny().size());
        Assert.assertEquals("a", ((Element)value.getAny().get(0)).getLocalName());
        
        resource.destroy();
    }
    
    @Test
    public void getWithNamespaceTest() {
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
        ValueType value = getValue(response.getRepresentation());
        Assert.assertEquals(1, value.getAny().size());
        Assert.assertEquals("a", ((Element)value.getAny().get(0)).getLocalName());
        Assert.assertEquals("www.example.org", ((Element)value.getAny().get(0)).getNamespaceURI());
        
        resource.destroy();
    }
    
    @Test
    public void qetEmptyResultTest() {
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
        ValueType value = getValue(response.getRepresentation());
        Assert.assertEquals(0, value.getAny().size());
        
        resource.destroy();
    }
    
    @Test
    public void getMoreValuesTest() {
        String content = "<root><a><b>Text1</b><b>Text2</b><b>Text3</b></a></root>";
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
        ValueType value = getValue(response.getRepresentation());
        Assert.assertEquals(3, value.getAny().size());
        Assert.assertEquals("b", ((Element)value.getAny().get(0)).getLocalName());
        Assert.assertEquals("b", ((Element)value.getAny().get(1)).getLocalName());
        Assert.assertEquals("b", ((Element)value.getAny().get(2)).getLocalName());
        
        resource.destroy();
    }
    
    @Test(expected = SOAPFaultException.class)
    public void getWrongQNameTest() {
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
    
    private static ValueType getValue(Representation representation) {
        @SuppressWarnings("unchecked")
        JAXBElement<ValueType> jaxb = (JAXBElement<ValueType>) representation.getAny();
        return jaxb.getValue();
    }
    
}
