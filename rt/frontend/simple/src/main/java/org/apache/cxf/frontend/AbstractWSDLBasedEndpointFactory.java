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

import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.BusException;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.binding.soap.SoapBindingConfiguration;
import org.apache.cxf.binding.soap.model.SoapBindingInfo;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.AbstractEndpointFactory;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.interceptor.AnnotationInterceptors;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.factory.AbstractServiceConfiguration;
import org.apache.cxf.service.factory.ReflectionServiceFactoryBean;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceModelUtil;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.local.LocalTransportFactory;
import org.apache.cxf.wsdl11.WSDLEndpointFactory;

public abstract class AbstractWSDLBasedEndpointFactory extends AbstractEndpointFactory {
    private static final Logger LOG = LogUtils.getL7dLogger(AbstractWSDLBasedEndpointFactory.class);

    private Class serviceClass;
    private ReflectionServiceFactoryBean serviceFactory;
    
    protected AbstractWSDLBasedEndpointFactory(ReflectionServiceFactoryBean sbean) {
        serviceFactory = sbean;
        serviceClass = sbean.getServiceClass();
        serviceName = sbean.getServiceQName(false);
        endpointName = sbean.getEndpointName(false);
        sbean.getServiceConfigurations().add(new SoapBindingServiceConfiguration());
    }
    protected AbstractWSDLBasedEndpointFactory() {
    }

    
    private class SoapBindingServiceConfiguration extends AbstractServiceConfiguration {
        public String getStyle() {
            if (getBindingConfig() instanceof SoapBindingConfiguration
                && ((SoapBindingConfiguration)getBindingConfig()).isSetStyle()) {
                return ((SoapBindingConfiguration)getBindingConfig()).getStyle();
            }
            return null;
        }
        public Boolean isWrapped() {
            if (getBindingConfig() instanceof SoapBindingConfiguration
                && ((SoapBindingConfiguration)getBindingConfig()).isSetStyle()
                && "rpc".equals(((SoapBindingConfiguration)getBindingConfig()).getStyle())) {
                return Boolean.FALSE;
            }
            return null;
        }
    }
    
    protected Endpoint createEndpoint() throws BusException, EndpointException {        
        serviceFactory.setFeatures(getFeatures());
        if (serviceName != null) {
            serviceFactory.setServiceName(serviceName);
        }
        
        if (endpointName != null) {
            serviceFactory.setEndpointName(endpointName);    
        }
        
        Service service = serviceFactory.getService();
        
        if (service == null) {
            initializeServiceFactory();
            service = serviceFactory.create();
        }
        
        if (endpointName == null) {
            endpointName = serviceFactory.getEndpointName();
        }
        EndpointInfo ei = service.getEndpointInfo(endpointName);
        
        if (ei != null) {
            if (transportId != null
                && !ei.getTransportId().equals(transportId)) {
                ei = null;
            } else {
                BindingFactoryManager bfm = getBus().getExtension(BindingFactoryManager.class);
                bindingFactory = bfm.getBindingFactory(ei.getBinding().getBindingId());
            }
        }
        
        if (ei == null) {
            if (getAddress() == null) {
                ei = ServiceModelUtil.findBestEndpointInfo(serviceFactory.getInterfaceName(), service
                    .getServiceInfos());
            }
            if (ei == null && !serviceFactory.isPopulateFromClass()) {
                ei = ServiceModelUtil.findBestEndpointInfo(serviceFactory.getInterfaceName(), service
                                                           .getServiceInfos());
                if (ei != null
                    && transportId != null
                    && !ei.getTransportId().equals(transportId)) {
                    ei = null;
                }
                if (ei != null) {
                    BindingFactoryManager bfm = getBus().getExtension(BindingFactoryManager.class);
                    bindingFactory = bfm.getBindingFactory(ei.getBinding().getBindingId());
                }

                if (ei == null) {
                    LOG.warning("Could not find endpoint/port for " 
                                + endpointName + " in wsdl. Creating default.");
                } else {
                    LOG.warning("Could not find endpoint/port for " 
                                + endpointName + " in wsdl. Using " 
                                + ei.getName() + ".");                        
                }
            }
            if (ei == null) {
                ei = createEndpointInfo();
            } else if (getAddress() != null) {
                ei.setAddress(getAddress()); 
            }
        } else if (getAddress() != null) {
            ei.setAddress(getAddress()); 
        }

        if (endpointReference != null) {
            ei.setAddress(endpointReference);
        }
        Endpoint ep = service.getEndpoints().get(ei.getName());
        
        if (ep == null) {
            ep = serviceFactory.createEndpoint(ei);
            ((EndpointImpl)ep).initializeActiveFeatures(getFeatures());
        } else {
            serviceFactory.setEndpointName(ei.getName());
            if (ep.getActiveFeatures() == null) {
                ((EndpointImpl)ep).initializeActiveFeatures(getFeatures());
            }
        }
        
        if (properties != null) {
            ep.putAll(properties);
        }
        
        service.getEndpoints().put(ep.getEndpointInfo().getName(), ep);
        
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

    protected void initializeServiceFactory() {
        Class cls = getServiceClass();

        serviceFactory.setServiceClass(cls);
        serviceFactory.setBus(getBus());
        if (dataBinding != null) {
            serviceFactory.setDataBinding(dataBinding);
        }
    }

    protected EndpointInfo createEndpointInfo() throws BusException {
        if (transportId == null 
            && getAddress() != null) {
            DestinationFactory df = getDestinationFactory();
            if (df == null) {
                DestinationFactoryManager dfm = getBus().getExtension(DestinationFactoryManager.class);
                df = dfm.getDestinationFactoryForUri(getAddress());
            }
            
            if (df != null) {
                transportId = df.getTransportIds().get(0);
            } else {
                //check conduits (the address could be supported on client only)
                ConduitInitiatorManager cim = getBus().getExtension(ConduitInitiatorManager.class);
                ConduitInitiator ci = cim.getConduitInitiatorForUri(getAddress());
                if (ci != null) {
                    transportId = ci.getTransportIds().get(0);
                }    
            }
        }
        
        // Get the Service from the ServiceFactory if specified
        Service service = serviceFactory.getService();
        // SOAP nonsense
        BindingInfo bindingInfo = createBindingInfo();
        if (bindingInfo instanceof SoapBindingInfo
            && (((SoapBindingInfo) bindingInfo).getTransportURI() == null
            || LocalTransportFactory.TRANSPORT_ID.equals(transportId))) {
            ((SoapBindingInfo) bindingInfo).setTransportURI(transportId);
            transportId = "http://schemas.xmlsoap.org/wsdl/soap/";
        }
        
        
        if (transportId == null) {
            if (bindingInfo instanceof SoapBindingInfo) {
                // TODO: we shouldn't have to do this, but the DF is null because the
                // LocalTransport doesn't return for the http:// uris
                // People also seem to be supplying a null JMS getAddress(), which is worrying
                transportId = "http://schemas.xmlsoap.org/wsdl/soap/";                
            } else {
                transportId = "http://schemas.xmlsoap.org/wsdl/http/";
            }
        }
        
        service.getServiceInfos().get(0).addBinding(bindingInfo);

        setTransportId(transportId);
        
        if (destinationFactory == null) {
            DestinationFactoryManager dfm = getBus().getExtension(DestinationFactoryManager.class);
            destinationFactory = dfm.getDestinationFactory(transportId);
        }
        
        EndpointInfo ei;
        if (destinationFactory instanceof WSDLEndpointFactory) {
            ei = ((WSDLEndpointFactory)destinationFactory)
                .createEndpointInfo(service.getServiceInfos().get(0), bindingInfo, null);
            ei.setTransportId(transportId);
        } else {
            ei = new EndpointInfo(service.getServiceInfos().get(0), transportId);
        }
        int count = 1;
        while (service.getEndpointInfo(endpointName) != null) {
            endpointName = new QName(endpointName.getNamespaceURI(), 
                                     endpointName.getLocalPart() + count);
            count++;
        }
        ei.setName(endpointName);
        ei.setAddress(getAddress());
        ei.setBinding(bindingInfo);
        
        if (publishedEndpointUrl != null && !"".equals(publishedEndpointUrl)) {
            ei.setProperty("publishedEndpointUrl", publishedEndpointUrl);
        }

        if (destinationFactory instanceof WSDLEndpointFactory) {
            WSDLEndpointFactory we = (WSDLEndpointFactory) destinationFactory;
            
            we.createPortExtensors(ei, service);
        } else {
            // ?
        }
        service.getServiceInfos().get(0).addEndpoint(ei);
        return ei;
    }

    /**
     * Add annotationed Interceptors and Features to the Endpoint
     * @param ep
     */
    protected void initializeAnnotationInterceptors(Endpoint ep, Class<?> cls) {
        AnnotationInterceptors provider = new AnnotationInterceptors(cls);
        if (initializeAnnotationInterceptors(provider, ep)) {
            LOG.fine("Added annotation based interceptors and features");
        }
    }    
    
    protected boolean initializeAnnotationInterceptors(AnnotationInterceptors provider, Endpoint ep) {
        boolean hasAnnotation = false;
        if (provider.getInFaultInterceptors() != null) {
            ep.getInFaultInterceptors().addAll(provider.getInFaultInterceptors());
            hasAnnotation = true;
        }
        if (provider.getInInterceptors() != null) {
            ep.getInInterceptors().addAll(provider.getInInterceptors());
            hasAnnotation = true;
        }
        if (provider.getOutFaultInterceptors() != null) {
            ep.getOutFaultInterceptors().addAll(provider.getOutFaultInterceptors());
            hasAnnotation = true;
        }
        if (provider.getOutInterceptors() != null) {
            ep.getOutInterceptors().addAll(provider.getOutInterceptors());
            hasAnnotation = true;
        }
        if (provider.getFeatures() != null) {
            getFeatures().addAll(provider.getFeatures());
            hasAnnotation = true;
        }
        
        return hasAnnotation;
    }

    protected BindingInfo createBindingInfo() {
        BindingFactoryManager mgr = bus.getExtension(BindingFactoryManager.class);
        String binding = bindingId;
        
        if (binding == null && bindingConfig != null) {
            binding = bindingConfig.getBindingId();
        }
        
        if (binding == null) {
            // default to soap binding
            binding = "http://schemas.xmlsoap.org/soap/";
        }
        
        try {
            if (binding.contains("/soap")) {
                if (bindingConfig == null) {
                    bindingConfig = new SoapBindingConfiguration();
                }
                if (bindingConfig instanceof SoapBindingConfiguration
                    && !((SoapBindingConfiguration)bindingConfig).isSetStyle()) {
                    ((SoapBindingConfiguration)bindingConfig).setStyle(serviceFactory.getStyle());
                }
            }

            bindingFactory = mgr.getBindingFactory(binding);
            
            BindingInfo inf = bindingFactory.createBindingInfo(serviceFactory.getService(),
                                                    binding, bindingConfig);
            
            for (BindingOperationInfo boi : inf.getOperations()) {
                serviceFactory.updateBindingOperation(boi);
            }
            return inf;
        } catch (BusException ex) {
            throw new ServiceConstructionException(
                   new Message("COULD.NOT.RESOLVE.BINDING", LOG, bindingId), ex);
        }
    }

    public Class getServiceClass() {
        return serviceClass;
    }

    public void setServiceClass(Class serviceClass) {
        this.serviceClass = serviceClass;
    }

    public ReflectionServiceFactoryBean getServiceFactory() {
        return serviceFactory;
    }


    public void setServiceFactory(ReflectionServiceFactoryBean serviceFactory) {
        this.serviceFactory = serviceFactory;
    }

    public String getWsdlURL() {
        return getServiceFactory().getWsdlURL();
    }

    public void setWsdlURL(String wsdlURL) {
        getServiceFactory().setWsdlURL(wsdlURL);
    }
}
