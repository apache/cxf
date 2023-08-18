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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.addressing.ReferenceParametersType;
import org.apache.cxf.ws.transfer.Create;
import org.apache.cxf.ws.transfer.CreateResponse;
import org.apache.cxf.ws.transfer.Representation;
import org.apache.cxf.ws.transfer.manager.ResourceManager;
import org.apache.cxf.ws.transfer.resourcefactory.ResourceFactory;

import org.junit.Assert;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ResourceFactoryTest extends IntegrationBaseTest {

    private static final String RESOURCE_UUID = "123456";

    private static final String REF_PARAM_NAMESPACE = "org.apache.cxf.transfer/manager";

    private static final String REF_PARAM_LOCAL_NAME = "UUID";

    private ReferenceParametersType createReferenceParameters() {
        ReferenceParametersType refParam = new ReferenceParametersType();
        Element uuidEl = DOMUtils.getEmptyDocument().createElementNS(
                REF_PARAM_NAMESPACE, REF_PARAM_LOCAL_NAME);
        uuidEl.setTextContent(RESOURCE_UUID);
        refParam.getAny().add(uuidEl);
        return refParam;
    }

    private static Element createXMLRepresentation() {
        Document document = DOMUtils.getEmptyDocument();
        Element root = document.createElement("root");
        Element child1 = document.createElement("child1");
        Element child2 = document.createElement("child2");
        root.appendChild(child1);
        root.appendChild(child2);
        return root;
    }

    private ResourceFactory createClient() {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setBus(bus);
        factory.setServiceClass(ResourceFactory.class);
        factory.setAddress(RESOURCE_FACTORY_ADDRESS);
        return (ResourceFactory) factory.create();
    }

    @Test
    public void createLocalResourceTest() {
        ReferenceParametersType refParams = createReferenceParameters();
        ResourceManager manager = mock(ResourceManager.class);
        when(manager.create(isA(Representation.class)))
                .thenReturn(refParams);

        Server localResourceFactory = createLocalResourceFactory(manager);
        ResourceFactory client = createClient();

        Create createRequest = new Create();
        Representation representation = new Representation();
        representation.setAny(createXMLRepresentation());
        createRequest.setRepresentation(representation);

        CreateResponse response = client.create(createRequest);
        verify(manager, times(1)).create(isA(Representation.class));

        Assert.assertEquals("ResourceAddress is other than expected.", RESOURCE_ADDRESS,
                response.getResourceCreated().getAddress().getValue());
        Element refParamEl = (Element) response.getResourceCreated().getReferenceParameters().getAny().get(0);
        Assert.assertEquals(REF_PARAM_NAMESPACE, refParamEl.getNamespaceURI());
        Assert.assertEquals(REF_PARAM_LOCAL_NAME, refParamEl.getLocalName());
        Assert.assertEquals(RESOURCE_UUID, refParamEl.getTextContent());
        Assert.assertEquals("root", ((Element) response.getRepresentation().getAny()).getLocalName());
        Assert.assertEquals(2, ((Element) response.getRepresentation().getAny()).getChildNodes().getLength());

        localResourceFactory.destroy();
    }

    @Test
    public void createRemoteResourceTest() {
        ReferenceParametersType refParams = createReferenceParameters();
        ResourceManager manager = mock(ResourceManager.class);
        when(manager.create(isA(Representation.class)))
                .thenReturn(refParams);

        Server remoteResourceFactory = createRemoteResourceFactory();
        Server remoteResource = createRemoteResource(manager);
        ResourceFactory client = createClient();

        Create createRequest = new Create();
        Representation representation = new Representation();
        representation.setAny(createXMLRepresentation());
        createRequest.setRepresentation(representation);

        CreateResponse response = client.create(createRequest);
        verify(manager, times(1)).create(isA(Representation.class));

        Assert.assertEquals("ResourceAddress is other than expected.", RESOURCE_REMOTE_ADDRESS,
                response.getResourceCreated().getAddress().getValue());
        Element refParamEl = (Element) response.getResourceCreated().getReferenceParameters().getAny().get(0);
        Assert.assertEquals(REF_PARAM_NAMESPACE, refParamEl.getNamespaceURI());
        Assert.assertEquals(REF_PARAM_LOCAL_NAME, refParamEl.getLocalName());
        Assert.assertEquals(RESOURCE_UUID, refParamEl.getTextContent());
        Assert.assertEquals("root", ((Element) response.getRepresentation().getAny()).getLocalName());
        Assert.assertEquals(2, ((Element) response.getRepresentation().getAny()).getChildNodes().getLength());

        remoteResourceFactory.destroy();
        remoteResource.destroy();
    }
}
