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

import org.w3c.dom.Element;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.addressing.ReferenceParametersType;
import org.apache.cxf.ws.transfer.Delete;
import org.apache.cxf.ws.transfer.Get;
import org.apache.cxf.ws.transfer.GetResponse;
import org.apache.cxf.ws.transfer.Put;
import org.apache.cxf.ws.transfer.PutResponse;
import org.apache.cxf.ws.transfer.Representation;
import org.apache.cxf.ws.transfer.manager.ResourceManager;
import org.apache.cxf.ws.transfer.resource.Resource;
import org.apache.cxf.ws.transfer.shared.handlers.ReferenceParameterAddingHandler;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author erich
 */
public class ResourceTest extends IntegrationBaseTest {
    
    private static final String UUID_NAME = "UUID";
    
    private static final String UUID_NAMESPACE = "test";
    
    private static final String UUID_VALUE = "123456";
    
    private static final String REPRESENTATION_NAME = "name1";
    
    private static final String REPRESENTATION_NAMESPACE = "test";
    
    private static final String REPRESENTATION_VALUE = "value1";
    
    private Resource createClient(ReferenceParametersType refParams) {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setBus(bus);
        factory.setServiceClass(Resource.class);
        factory.setAddress(RESOURCE_LOCAL_ADDRESS);
        factory.getInInterceptors().add(logInInterceptor);
        factory.getOutInterceptors().add(logOutInterceptor);
        factory.getHandlers().add(new ReferenceParameterAddingHandler(refParams));
        return (Resource) factory.create();
    }
    
    @Test
    public void getRequestTest() {
        Element representationEl = document.createElementNS(REPRESENTATION_NAMESPACE, REPRESENTATION_NAME);
        representationEl.setTextContent(REPRESENTATION_VALUE);
        Representation representation = new Representation();
        representation.setAny(representationEl);
        
        ResourceManager manager = EasyMock.createMock(ResourceManager.class);
        EasyMock.expect(manager.get(EasyMock.isA(ReferenceParametersType.class))).andReturn(representation);
        EasyMock.expectLastCall().once();
        EasyMock.replay(manager);
        
        ReferenceParametersType refParams = new ReferenceParametersType();
        Element uuid = document.createElementNS(UUID_NAMESPACE, UUID_NAME);
        uuid.setTextContent(UUID_VALUE);
        refParams.getAny().add(uuid);
        
        Server server = createLocalResource(manager);
        Resource client = createClient(refParams);
        
        GetResponse response = client.get(new Get());
        EasyMock.verify(manager);
        
        representationEl = (Element) response.getRepresentation().getAny();
        Assert.assertEquals("Namespace is other than expected.",
                REPRESENTATION_NAMESPACE, representationEl.getNamespaceURI());
        Assert.assertEquals("Element name is other than expected",
                REPRESENTATION_NAME, representationEl.getLocalName());
        Assert.assertEquals("Value is other than expected.",
                REPRESENTATION_VALUE, representationEl.getTextContent());
        
        server.destroy();
    }
    
    @Test
    public void putRequestTest() {
        ResourceManager manager = EasyMock.createMock(ResourceManager.class);
        manager.put(EasyMock.isA(ReferenceParametersType.class), EasyMock.isA(Representation.class));
        EasyMock.expectLastCall().once();
        EasyMock.replay(manager);
        
        ReferenceParametersType refParams = new ReferenceParametersType();
        Element uuid = document.createElementNS(UUID_NAMESPACE, UUID_NAME);
        uuid.setTextContent(UUID_VALUE);
        refParams.getAny().add(uuid);
        
        Element representationEl = document.createElementNS(REPRESENTATION_NAMESPACE, REPRESENTATION_NAME);
        representationEl.setTextContent(REPRESENTATION_VALUE);
        Representation representation = new Representation();
        representation.setAny(representationEl);
        
        Server server = createLocalResource(manager);
        Resource client = createClient(refParams);
        
        Put putRequest = new Put();
        putRequest.setRepresentation(representation);
        
        PutResponse response = client.put(putRequest);
        EasyMock.verify(manager);
        
        representationEl = (Element) response.getRepresentation().getAny();
        Assert.assertEquals("Namespace is other than expected.",
                REPRESENTATION_NAMESPACE, representationEl.getNamespaceURI());
        Assert.assertEquals("Element name is other than expected",
                REPRESENTATION_NAME, representationEl.getLocalName());
        Assert.assertEquals("Value is other than expected.",
                REPRESENTATION_VALUE, representationEl.getTextContent());
        
        server.destroy();
    }
    
    @Test
    public void deleteRequestTest() {
        ResourceManager manager = EasyMock.createMock(ResourceManager.class);
        manager.delete(EasyMock.isA(ReferenceParametersType.class));
        EasyMock.expectLastCall().once();
        EasyMock.replay(manager);
        
        ReferenceParametersType refParams = new ReferenceParametersType();
        Element uuid = document.createElementNS(UUID_NAMESPACE, UUID_NAME);
        uuid.setTextContent(UUID_VALUE);
        refParams.getAny().add(uuid);
        
        Server server = createLocalResource(manager);
        Resource client = createClient(refParams);
        
        client.delete(new Delete());
        EasyMock.verify(manager);
        
        server.destroy();
    }
}
