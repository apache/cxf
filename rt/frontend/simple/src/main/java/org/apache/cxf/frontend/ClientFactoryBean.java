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
package org.apache.cxf.frontend;

import java.util.Map;

import org.apache.cxf.BusException;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.service.factory.FactoryBeanListener;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.wsdl.service.factory.ReflectionServiceFactoryBean;
import org.apache.cxf.wsdl11.WSDLEndpointFactory;

public class ClientFactoryBean extends AbstractWSDLBasedEndpointFactory {

    public ClientFactoryBean() {
        this(new ReflectionServiceFactoryBean());
    }
    public ClientFactoryBean(ReflectionServiceFactoryBean factory) {
        super(factory);
    }

    @Override
    protected String detectTransportIdFromAddress(String ad) {
        ConduitInitiatorManager cim = getBus().getExtension(ConduitInitiatorManager.class);
        ConduitInitiator ci = cim.getConduitInitiatorForUri(getAddress());
        if (ci != null) {
            return ci.getTransportIds().get(0);
        }
        return null;
    }
    @Override
    protected WSDLEndpointFactory getWSDLEndpointFactory() {
        if (destinationFactory instanceof WSDLEndpointFactory) {
            return (WSDLEndpointFactory)destinationFactory;
        }
        try {
            Object o = getBus().getExtension(ConduitInitiatorManager.class)
                .getConduitInitiator(transportId);
            if (o instanceof WSDLEndpointFactory) {
                return (WSDLEndpointFactory)o;
            }
        } catch (Throwable t) {
            //ignore
        }

        if (destinationFactory == null) {
            try {
                destinationFactory = getBus().getExtension(DestinationFactoryManager.class)
                    .getDestinationFactory(transportId);
            } catch (Throwable t) {
                //ignore
            }
        }
        return null;
    }
    public Client create() {
        getServiceFactory().reset();
        if (getServiceFactory().getProperties() == null) {
            getServiceFactory().setProperties(properties);
        } else if (properties != null) {
            getServiceFactory().getProperties().putAll(properties);
        }
        Client client = null;
        Endpoint ep = null;
        try {
            ep = createEndpoint();
            this.getServiceFactory().sendEvent(FactoryBeanListener.Event.PRE_CLIENT_CREATE, ep);
            applyProperties(ep);
            client = createClient(ep);
            initializeAnnotationInterceptors(ep, getServiceClass());
        } catch (EndpointException | BusException e) {
            throw new ServiceConstructionException(e);
        }
        applyFeatures(client);
        this.getServiceFactory().sendEvent(FactoryBeanListener.Event.CLIENT_CREATED, client, ep);
        return client;
    }

    protected Client createClient(Endpoint ep) {
        return new ClientImpl(getBus(), ep, getConduitSelector());
    }

    protected void applyFeatures(Client client) {
        if (getFeatures() != null) {
            for (Feature feature : getFeatures()) {
                feature.initialize(client, getBus());
            }
        }
    }

    protected void applyProperties(Endpoint ep) {
        //Apply the AuthorizationPolicy to the endpointInfo
        Map<String, Object> props = this.getProperties();
        if (props != null && props.get(AuthorizationPolicy.class.getName()) != null) {
            AuthorizationPolicy ap = (AuthorizationPolicy)props.get(AuthorizationPolicy.class.getName());
            ep.getEndpointInfo().addExtensor(ap);
        }
    }

}
