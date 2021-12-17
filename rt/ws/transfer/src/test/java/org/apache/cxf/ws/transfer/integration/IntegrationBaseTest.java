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

import javax.xml.stream.XMLStreamException;

import org.w3c.dom.Document;

import jakarta.xml.ws.BindingProvider;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.addressing.ReferenceParametersType;
import org.apache.cxf.ws.transfer.Representation;
import org.apache.cxf.ws.transfer.dialect.fragment.ExpressionType;
import org.apache.cxf.ws.transfer.manager.ResourceManager;
import org.apache.cxf.ws.transfer.resource.Resource;
import org.apache.cxf.ws.transfer.resource.ResourceLocal;
import org.apache.cxf.ws.transfer.resource.ResourceRemote;
import org.apache.cxf.ws.transfer.resourcefactory.ResourceFactory;
import org.apache.cxf.ws.transfer.resourcefactory.ResourceFactoryImpl;
import org.apache.cxf.ws.transfer.resourcefactory.resolver.SimpleResourceResolver;
import org.apache.cxf.ws.transfer.shared.TransferConstants;

import org.junit.After;
import org.junit.Before;

public class IntegrationBaseTest {

    public static final String RESOURCE_FACTORY_ADDRESS = "local://ResourceFactory";

    public static final String RESOURCE_ADDRESS = "local://ResourceLocal";

    public static final String RESOURCE_REMOTE_ADDRESS = "local://ResourceRemote";

    public static final String RESOURCE_REMOTE_MANAGER_ADDRESS = "local://ResourceRemote"
            + TransferConstants.RESOURCE_REMOTE_SUFFIX;

    public static final String RESOURCE_LOCAL_ADDRESS = "local://ResourceLocal";

    protected Bus bus;

    @Before
    public void before() {
        bus = BusFactory.getDefaultBus();
    }

    @After
    public void after() {
        bus.shutdown(true);
        bus = null;
    }

    protected Server createLocalResourceFactory(ResourceManager manager) {
        ResourceFactoryImpl implementor = new ResourceFactoryImpl();
        implementor.setResourceResolver(new SimpleResourceResolver(RESOURCE_ADDRESS, manager));
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setBus(bus);
        factory.setServiceClass(ResourceFactory.class);
        factory.setAddress(RESOURCE_FACTORY_ADDRESS);
        factory.setServiceBean(implementor);

        return factory.create();
    }

    protected Server createRemoteResourceFactory() {
        ResourceFactoryImpl implementor = new ResourceFactoryImpl();
        implementor.setResourceResolver(new SimpleResourceResolver(RESOURCE_REMOTE_ADDRESS, null));
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setBus(bus);
        factory.setServiceClass(ResourceFactory.class);
        factory.setAddress(RESOURCE_FACTORY_ADDRESS);
        factory.setServiceBean(implementor);
        return factory.create();
    }

    protected Server createRemoteResource(ResourceManager manager) {
        ResourceRemote implementor = new ResourceRemote();
        implementor.setManager(manager);
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();

        Map<String, Object> props = factory.getProperties(true);
        props.put("jaxb.additionalContextClasses",
                org.apache.cxf.ws.transfer.dialect.fragment.ExpressionType.class);
        factory.setProperties(props);

        factory.setBus(bus);
        factory.setServiceClass(ResourceFactory.class);
        factory.setAddress(RESOURCE_REMOTE_MANAGER_ADDRESS);
        factory.setServiceBean(implementor);
        return factory.create();
    }

    protected Server createLocalResource(ResourceManager manager) {
        ResourceLocal implementor = new ResourceLocal();
        implementor.setManager(manager);
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();

        Map<String, Object> props = factory.getProperties(true);
        props.put("jaxb.additionalContextClasses",
                org.apache.cxf.ws.transfer.dialect.fragment.ExpressionType.class);
        factory.setProperties(props);

        factory.setBus(bus);
        factory.setServiceClass(Resource.class);
        factory.setAddress(RESOURCE_LOCAL_ADDRESS);
        factory.setServiceBean(implementor);
        return factory.create();
    }

    protected Representation getRepresentation(String content) throws XMLStreamException {
        Document doc = StaxUtils.read(new StringReader(content));
        Representation representation = new Representation();
        representation.setAny(doc.getDocumentElement());
        return representation;
    }

    protected Resource createClient(ReferenceParametersType refParams) {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();

        Map<String, Object> props = factory.getProperties();
        if (props == null) {
            props = new HashMap<>();
        }
        props.put("jaxb.additionalContextClasses",
                ExpressionType.class);
        factory.setProperties(props);

        factory.setBus(bus);
        factory.setServiceClass(Resource.class);
        factory.setAddress(RESOURCE_ADDRESS);
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
}
