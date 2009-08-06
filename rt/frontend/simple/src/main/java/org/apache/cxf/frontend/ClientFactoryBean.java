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
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.service.factory.FactoryBeanListener;
import org.apache.cxf.service.factory.ReflectionServiceFactoryBean;
import org.apache.cxf.service.factory.ServiceConstructionException;

public class ClientFactoryBean extends AbstractWSDLBasedEndpointFactory {
    private Client client;

    public ClientFactoryBean() {
        this(new ReflectionServiceFactoryBean());
    }
    public ClientFactoryBean(ReflectionServiceFactoryBean factory) {
        super(factory);
    }

    public Client create() {

        if (client != null) {
            return client;
        }
        applyExtraClass();
        try {
            Endpoint ep = createEndpoint();
            applyProperties(ep);
            createClient(ep);
            initializeAnnotationInterceptors(ep, getServiceClass());
        } catch (EndpointException e) {
            throw new ServiceConstructionException(e);
        } catch (BusException e) {
            throw new ServiceConstructionException(e);
        }
        applyFeatures();
        return client;
    }

    protected void createClient(Endpoint ep) {
        client = new ClientImpl(getBus(), ep, getConduitSelector());
        this.getServiceFactory().sendEvent(FactoryBeanListener.Event.CLIENT_CREATED, client, ep);
    }

    protected void applyFeatures() {
        if (getFeatures() != null) {
            for (AbstractFeature feature : getFeatures()) {
                feature.initialize(client, getBus());
            }
        }
    }

    protected void applyExtraClass() {
        DataBinding dataBinding = getServiceFactory().getDataBinding();
        if (dataBinding instanceof JAXBDataBinding) {
            Map props = this.getProperties();
            if (props != null && props.get("jaxb.additionalContextClasses") != null) {
                Class[] extraClass = (Class[])this.getProperties().get("jaxb.additionalContextClasses");
                ((JAXBDataBinding)dataBinding).setExtraClass(extraClass);
            }
        }
    }


    protected void applyProperties(Endpoint ep) {
        //Apply the AuthorizationPolicy to the endpointInfo
        Map props = this.getProperties();
        if (props != null && props.get(AuthorizationPolicy.class.getName()) != null) {
            AuthorizationPolicy ap = (AuthorizationPolicy)props.get(AuthorizationPolicy.class.getName());
            ep.getEndpointInfo().addExtensor(ap);
        }
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }
}
