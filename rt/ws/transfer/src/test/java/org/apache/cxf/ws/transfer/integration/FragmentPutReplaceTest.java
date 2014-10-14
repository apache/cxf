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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
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
import org.apache.cxf.ws.transfer.shared.TransferTools;
import org.apache.cxf.ws.transfer.shared.handlers.ReferenceParameterAddingHandler;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Erich Duda
 */
public class FragmentPutReplaceTest extends IntegrationBaseTest {
    
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
    public void replaceElementTest() {
        String content = "<root><a>Text</a></root>";
        ResourceManager resourceManager = new MemoryResourceManager();
        ReferenceParametersType refParams = resourceManager.create(getRepresentation(content));
        Server resource = createLocalResource(resourceManager);
        Resource client = createClient(refParams);
        
        Put request = new Put();
        request.setDialect(FragmentDialectConstants.FRAGMENT_2011_03_IRI);
        Fragment fragment = new Fragment();
        ExpressionType expression = new ExpressionType();
        expression.setLanguage(FragmentDialectConstants.XPATH10_LANGUAGE_IRI);
        expression.getContent().add("/root/a");
        Element replacedElement = TransferTools.createElement("b");
        replacedElement.setTextContent("Better text");
        ValueType value = new ValueType();
        value.getContent().add(replacedElement);
        fragment.setExpression(expression);
        fragment.setValue(value);
        request.getAny().add(fragment);
        
        PutResponse response = client.put(request);
        Element rootEl = (Element) response.getRepresentation().getAny();
        Assert.assertEquals("b", rootEl.getChildNodes().item(0).getNodeName());
        Assert.assertEquals("Better text", rootEl.getChildNodes().item(0).getTextContent());
        
        resource.destroy();
    }
    
    @Test
    public void replaceTextContentTest() {
        String content = "<root><a>Text</a></root>";
        ResourceManager resourceManager = new MemoryResourceManager();
        ReferenceParametersType refParams = resourceManager.create(getRepresentation(content));
        Server resource = createLocalResource(resourceManager);
        Resource client = createClient(refParams);
        
        //ObjectFactory objectFactory = new ObjectFactory();
        
        Put request = new Put();
        request.setDialect(FragmentDialectConstants.FRAGMENT_2011_03_IRI);
        Fragment fragment = new Fragment();
        ExpressionType expression = new ExpressionType();
        expression.setLanguage(FragmentDialectConstants.XPATH10_LANGUAGE_IRI);
        expression.getContent().add("/root/a/text()");
        ValueType value = new ValueType();
        value.getContent().add("Better text");
        fragment.setExpression(expression);
        fragment.setValue(value);
        request.getAny().add(fragment);
        
        PutResponse response = client.put(request);
        Element rootEl = (Element) response.getRepresentation().getAny();
        Assert.assertEquals("a", rootEl.getChildNodes().item(0).getNodeName());
        Assert.assertEquals("Better text", rootEl.getChildNodes().item(0).getTextContent());
        
        resource.destroy();
    }
    
    @Test
    public void replaceAttributeTest() {
        String content = "<root><a foo=\"1\">Text</a></root>";
        ResourceManager resourceManager = new MemoryResourceManager();
        ReferenceParametersType refParams = resourceManager.create(getRepresentation(content));
        Server resource = createLocalResource(resourceManager);
        Resource client = createClient(refParams);
        
        //ObjectFactory objectFactory = new ObjectFactory();
        
        Put request = new Put();
        request.setDialect(FragmentDialectConstants.FRAGMENT_2011_03_IRI);
        Fragment fragment = new Fragment();
        ExpressionType expression = new ExpressionType();
        expression.setLanguage(FragmentDialectConstants.XPATH10_LANGUAGE_IRI);
        expression.getContent().add("/root/a/@foo");
        Element replacedAttr = TransferTools.createElementNS(
                FragmentDialectConstants.FRAGMENT_2011_03_IRI,
                FragmentDialectConstants.FRAGMENT_ATTR_NODE_NAME
        );
        replacedAttr.setAttributeNS(
                FragmentDialectConstants.FRAGMENT_2011_03_IRI,
                FragmentDialectConstants.FRAGMENT_ATTR_NODE_NAME_ATTR,
                "bar"
        );
        replacedAttr.setTextContent("2");
        ValueType value = new ValueType();
        value.getContent().add(replacedAttr);
        fragment.setExpression(expression);
        fragment.setValue(value);
        request.getAny().add(fragment);
        
        PutResponse response = client.put(request);
        Element rootEl = (Element) response.getRepresentation().getAny();
        Element aEl = (Element) rootEl.getChildNodes().item(0);
        Assert.assertNotNull(aEl);
        String attribute = aEl.getAttributeNS(
                FragmentDialectConstants.FRAGMENT_2011_03_IRI,
                "bar"
        );
        Assert.assertEquals("2", attribute);
        
        resource.destroy();
    }
    
    @Test
    public void replaceEmptyDocumentTest() {
        ResourceManager resourceManager = new MemoryResourceManager();
        ReferenceParametersType refParams = resourceManager.create(new Representation());
        Server resource = createLocalResource(resourceManager);
        Resource client = createClient(refParams);
        
        Put request = new Put();
        request.setDialect(FragmentDialectConstants.FRAGMENT_2011_03_IRI);
        Fragment fragment = new Fragment();
        ExpressionType expression = new ExpressionType();
        expression.setLanguage(FragmentDialectConstants.XPATH10_LANGUAGE_IRI);
        expression.getContent().add("/");
        Element replacedElement = TransferTools.createElement("a");
        ValueType value = new ValueType();
        value.getContent().add(replacedElement);
        fragment.setExpression(expression);
        fragment.setValue(value);
        request.getAny().add(fragment);
        
        PutResponse response = client.put(request);
        Element rootEl = (Element) response.getRepresentation().getAny();
        Assert.assertEquals("a", rootEl.getNodeName());
        
        resource.destroy();
    }
    
    @Test
    public void replaceDocumentTest() {
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
        expression.getContent().add("/");
        Element replacedElement = TransferTools.createElement("b");
        ValueType value = new ValueType();
        value.getContent().add(replacedElement);
        fragment.setExpression(expression);
        fragment.setValue(value);
        request.getAny().add(fragment);
        
        PutResponse response = client.put(request);
        Element rootEl = (Element) response.getRepresentation().getAny();
        Assert.assertEquals("b", rootEl.getNodeName());
        
        resource.destroy();
    }
    
    @Test
    public void replaceDocument2Test() {
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
        expression.getContent().add("/*");
        Element replacedElement = TransferTools.createElement("b");
        ValueType value = new ValueType();
        value.getContent().add(replacedElement);
        fragment.setExpression(expression);
        fragment.setValue(value);
        request.getAny().add(fragment);
        
        PutResponse response = client.put(request);
        Element rootEl = (Element) response.getRepresentation().getAny();
        Assert.assertEquals("b", rootEl.getNodeName());
        
        resource.destroy();
    }
}
