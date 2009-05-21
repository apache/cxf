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
package org.apache.cxf.jaxrs;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.cxf.BusException;
import org.apache.cxf.binding.BindingConfiguration;
import org.apache.cxf.binding.BindingFactory;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.AbstractEndpointFactory;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.jaxrs.model.UserResource;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;


/**
 * Bean to help easily create Server endpoints for JAX-RS. Example:
 * <pre>
 * JAXRSServerFactoryBean sf = JAXRSServerFactoryBean();
 * sf.setResourceClasses(Book.class);
 * sf.setBindingId(JAXRSBindingFactory.JAXRS_BINDING_ID);
 * sf.setAddress("http://localhost:9080/");
 * sf.create();
 * </pre>
 * This will start a server for you and register it with the ServerManager.
 */
public class AbstractJAXRSFactoryBean extends AbstractEndpointFactory {
    
    private static final Logger LOG = LogUtils.getL7dLogger(AbstractJAXRSFactoryBean.class);
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(AbstractJAXRSFactoryBean.class);
    
    protected List<String> schemaLocations;
    protected JAXRSServiceFactoryBean serviceFactory;
    protected List<?> entityProviders;
    
    protected AbstractJAXRSFactoryBean() {
        this(new JAXRSServiceFactoryBean());
    }
    
    protected AbstractJAXRSFactoryBean(JAXRSServiceFactoryBean serviceFactory) {
        this.serviceFactory = serviceFactory;
        setBindingId(JAXRSBindingFactory.JAXRS_BINDING_ID);
    }
    
    
    /*
     * EndpointInfo contains information form WSDL's physical part such as
     * endpoint address, binding, transport etc. For JAX-RS based EndpointInfo,
     * as there is no WSDL, these information are set manually, eg, default
     * transport is http, binding is JAX-RS binding, endpoint address is from
     * server mainline.
     */    
    protected EndpointInfo createEndpointInfo() throws BusException {
        String transportId = getTransportId();
        if (transportId == null && getAddress() != null) {
            DestinationFactory df = getDestinationFactory();
            if (df == null) {
                DestinationFactoryManager dfm = getBus().getExtension(DestinationFactoryManager.class);
                df = dfm.getDestinationFactoryForUri(getAddress());
            }

            if (df != null) {
                transportId = df.getTransportIds().get(0);
            }
        }

        //default to http transport
        if (transportId == null) {
            transportId = "http://schemas.xmlsoap.org/wsdl/soap/http";
        }

        setTransportId(transportId);

        EndpointInfo ei = new EndpointInfo();
        ei.setTransportId(transportId);
        ei.setName(serviceFactory.getService().getName());
        ei.setAddress(getAddress());        

        BindingInfo bindingInfo = createBindingInfo();
        ei.setBinding(bindingInfo);

        return ei;
    }

    protected BindingInfo createBindingInfo() {
        BindingFactoryManager mgr = getBus().getExtension(BindingFactoryManager.class);
        String binding = getBindingId();
        BindingConfiguration bindingConfig = getBindingConfig();

        if (binding == null && bindingConfig != null) {
            binding = bindingConfig.getBindingId();
        }

        if (binding == null) {
            binding = JAXRSBindingFactory.JAXRS_BINDING_ID;
        }

        try {
            BindingFactory bindingFactory = mgr.getBindingFactory(binding);
            setBindingFactory(bindingFactory);
            return bindingFactory.createBindingInfo(serviceFactory.getService(),
                                                    binding, bindingConfig);
        } catch (BusException ex) {
            ex.printStackTrace();
            //do nothing
        }
        return null;
    }

    public JAXRSServiceFactoryBean getServiceFactory() {
        return serviceFactory;
    }

    public void setServiceFactory(JAXRSServiceFactoryBean serviceFactory) {
        this.serviceFactory = serviceFactory;
    }

    protected Endpoint createEndpoint() throws BusException, EndpointException {
        Service service = serviceFactory.getService();

        if (service == null) {
            service = serviceFactory.create();
        }

        EndpointInfo ei = createEndpointInfo();
        Endpoint ep = new EndpointImpl(getBus(), getServiceFactory().getService(), ei);
        
        if (properties != null) {
            ep.putAll(properties);
        }
        
        if (getInInterceptors() != null) {
            ep.getInInterceptors().addAll(getInInterceptors());
        }
        if (getOutInterceptors() != null) {
            ep.getOutInterceptors().addAll(getOutInterceptors());
        }
        if (getInFaultInterceptors() != null) {
            ep.getInFaultInterceptors().addAll(getInFaultInterceptors());
        }
        if (getOutFaultInterceptors() != null) {
            ep.getOutFaultInterceptors().addAll(getOutFaultInterceptors());
        }
        return ep;
    }
    
    public void setSchemaLocation(String schema) {
        setSchemaLocations(Collections.singletonList(schema));    
    }
    
    public void setSchemaLocations(List<String> schemas) {
        this.schemaLocations = schemas;    
    }
    
    /**
     * @return the entityProviders
     */
    public List<?> getProviders() {
        return entityProviders;
    }

    /**
     * @param entityProviders the entityProviders to set
     */
    public void setProviders(List<? extends Object> providers) {
        this.entityProviders = providers;
    }
    
    public void setProvider(Object provider) {
        setProviders(Collections.singletonList(provider));
    }

    protected void checkResources() {
        if (!serviceFactory.resourcesAvailable()) {
            org.apache.cxf.common.i18n.Message msg = 
                new org.apache.cxf.common.i18n.Message("NO_RESOURCES_AVAILABLE", 
                                                       BUNDLE);
            LOG.severe(msg.toString());
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
    }
    
    protected ProviderFactory setupFactory(Endpoint ep) { 
        ProviderFactory factory = ProviderFactory.getInstance(); 
        if (entityProviders != null) {
            factory.setUserProviders(entityProviders); 
        }
        if (schemaLocations != null) {
            factory.setSchemaLocations(schemaLocations);
        }
        ep.put(ProviderFactory.class.getName(), factory);
        return factory;
    }

    public void setModelBeans(UserResource... resources) {
        setModelBeans(Arrays.asList(resources));
    }
    
    public void setModelBeans(List<UserResource> resources) {
        serviceFactory.setUserResources(resources);
    }
    
    public void setModelRef(String modelRef) {
        List<UserResource> resources = ResourceUtils.getUserResources(modelRef);
        if (resources != null) {
            serviceFactory.setUserResources(resources);
        }
    }
    
}
