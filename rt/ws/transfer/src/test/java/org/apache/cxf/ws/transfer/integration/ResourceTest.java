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

import jakarta.xml.ws.BindingProvider;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.addressing.ReferenceParametersType;
import org.apache.cxf.ws.transfer.Delete;
import org.apache.cxf.ws.transfer.Get;
import org.apache.cxf.ws.transfer.GetResponse;
import org.apache.cxf.ws.transfer.Put;
import org.apache.cxf.ws.transfer.PutResponse;
import org.apache.cxf.ws.transfer.Representation;
import org.apache.cxf.ws.transfer.manager.MemoryResourceManager;
import org.apache.cxf.ws.transfer.manager.ResourceManager;
import org.apache.cxf.ws.transfer.resource.Resource;

import org.junit.Assert;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ResourceTest extends IntegrationBaseTest {

    private static final String UUID_VALUE = "123456";

    private static final String REPRESENTATION_NAME = "name1";

    private static final String REPRESENTATION_NAMESPACE = "test";

    private static final String REPRESENTATION_VALUE = "value1";

    protected Resource createClient(ReferenceParametersType refParams) {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setBus(bus);
        factory.setServiceClass(Resource.class);
        factory.setAddress(RESOURCE_LOCAL_ADDRESS);
        Resource proxy = (Resource) factory.create();

        // Add reference parameters
        AddressingProperties addrProps = new AddressingProperties();
        EndpointReferenceType endpoint = new EndpointReferenceType();
        endpoint.setReferenceParameters(refParams);
        endpoint.setAddress(ContextUtils.getAttributedURI(RESOURCE_ADDRESS));
        addrProps.setTo(endpoint);
        ((BindingProvider) proxy).getRequestContext().put(JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES, addrProps);

        return proxy;
    }

    @Test
    public void getRequestTest() {
        Element representationEl =
            DOMUtils.getEmptyDocument().createElementNS(REPRESENTATION_NAMESPACE, REPRESENTATION_NAME);
        representationEl.setTextContent(REPRESENTATION_VALUE);
        Representation representation = new Representation();
        representation.setAny(representationEl);

        ResourceManager manager = mock(ResourceManager.class);
        when(manager.get(isA(ReferenceParametersType.class))).thenReturn(representation);

        ReferenceParametersType refParams = new ReferenceParametersType();
        Element uuid = DOMUtils.getEmptyDocument().createElementNS(
                MemoryResourceManager.REF_NAMESPACE, MemoryResourceManager.REF_LOCAL_NAME);
        uuid.setTextContent(UUID_VALUE);
        refParams.getAny().add(uuid);

        Server server = createLocalResource(manager);
        Resource client = createClient(refParams);

        GetResponse response = client.get(new Get());
        verify(manager, times(1)).get(isA(ReferenceParametersType.class));

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
        ResourceManager manager = mock(ResourceManager.class);
        when(manager.get(isA(ReferenceParametersType.class))).thenReturn(new Representation());

        ReferenceParametersType refParams = new ReferenceParametersType();
        Element uuid = DOMUtils.getEmptyDocument().createElementNS(
                MemoryResourceManager.REF_NAMESPACE, MemoryResourceManager.REF_LOCAL_NAME);
        uuid.setTextContent(UUID_VALUE);
        refParams.getAny().add(uuid);

        Element representationEl =
            DOMUtils.getEmptyDocument().createElementNS(REPRESENTATION_NAMESPACE, REPRESENTATION_NAME);
        representationEl.setTextContent(REPRESENTATION_VALUE);
        Representation representation = new Representation();
        representation.setAny(representationEl);

        Server server = createLocalResource(manager);
        Resource client = createClient(refParams);

        Put putRequest = new Put();
        putRequest.setRepresentation(representation);

        PutResponse response = client.put(putRequest);
        verify(manager, times(1)).get(isA(ReferenceParametersType.class));
        verify(manager, times(1)).put(isA(ReferenceParametersType.class), isA(Representation.class));

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
        ResourceManager manager = mock(ResourceManager.class);

        ReferenceParametersType refParams = new ReferenceParametersType();
        Element uuid = DOMUtils.getEmptyDocument().createElementNS(
                MemoryResourceManager.REF_NAMESPACE, MemoryResourceManager.REF_LOCAL_NAME);
        uuid.setTextContent(UUID_VALUE);
        refParams.getAny().add(uuid);

        Server server = createLocalResource(manager);
        Resource client = createClient(refParams);

        client.delete(new Delete());
        verify(manager, times(1)).delete(isA(ReferenceParametersType.class));

        server.destroy();
    }
}