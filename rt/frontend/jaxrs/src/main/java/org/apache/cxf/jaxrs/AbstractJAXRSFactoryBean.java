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

import org.apache.cxf.BusException;
import org.apache.cxf.binding.BindingConfiguration;
import org.apache.cxf.binding.BindingFactory;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.endpoint.AbstractEndpointFactory;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.endpoint.EndpointImpl;
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
    
    protected JAXRSServiceFactoryBean serviceFactory;
    
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
    

}
