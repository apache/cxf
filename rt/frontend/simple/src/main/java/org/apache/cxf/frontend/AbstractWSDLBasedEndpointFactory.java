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

import java.lang.reflect.Method;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.BusException;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.binding.soap.SoapBindingConfiguration;
import org.apache.cxf.binding.soap.SoapBindingFactory;
import org.apache.cxf.binding.soap.jms.interceptor.SoapJMSConstants;
import org.apache.cxf.binding.soap.model.SoapBindingInfo;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.AbstractEndpointFactory;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.factory.FactoryBeanListener;
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
import org.apache.cxf.wsdl.service.factory.AbstractServiceConfiguration;
import org.apache.cxf.wsdl.service.factory.ReflectionServiceFactoryBean;
import org.apache.cxf.wsdl11.WSDLEndpointFactory;

public abstract class AbstractWSDLBasedEndpointFactory extends AbstractEndpointFactory {
    private static final Logger LOG = LogUtils.getL7dLogger(AbstractWSDLBasedEndpointFactory.class);

    private Class<?> serviceClass;
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
            if ((transportId != null
                && !ei.getTransportId().equals(transportId))
                || (bindingId != null && !ei.getBinding().getBindingId().equals(bindingId))) {
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

                if (ei != null) {
                    BindingFactoryManager bfm = getBus().getExtension(BindingFactoryManager.class);
                    bindingFactory = bfm.getBindingFactory(ei.getBinding().getBindingId());
                }

                if (ei == null) {
                    LOG.warning("Could not find endpoint/port for "
                                + endpointName + " in wsdl. Creating default.");
                } else if (!ei.getName().equals(endpointName)) {
                    LOG.warning("Could not find endpoint/port for "
                                + endpointName + " in wsdl. Using "
                                + ei.getName() + ".");
                }
            }
            if (ei == null) {
                ei = createEndpointInfo(null);
            } else if (transportId != null
                    && !ei.getTransportId().equals(transportId)) {
                LOG.warning("Transport for endpoint/port "
                    + endpointName + " in wsdl doesn't match " + transportId + ".");
                BindingInfo bi = ei.getBinding();
                ei = createEndpointInfo(bi);
            } else if (bindingId != null && !ei.getBinding().getBindingId().equals(bindingId)
                //consider SoapBinding has multiple default namespace
                && !(SoapBindingFactory.DEFAULT_NAMESPACES.contains(bindingId)
                    && SoapBindingFactory.DEFAULT_NAMESPACES.contains(ei.getBinding().getBindingId()))) {
                LOG.warning("Binding for endpoint/port "
                    + endpointName + " in wsdl doesn't match " + bindingId + ".");
                ei = createEndpointInfo(null);
            } else if (getAddress() != null) {
                ei.setAddress(getAddress());
                if (ei.getAddress().startsWith("camel")
                        || ei.getAddress().startsWith("local")) {
                    modifyTransportIdPerAddress(ei);
                }

            }
        } else if (getAddress() != null) {
            ei.setAddress(getAddress());
        }

        if (publishedEndpointUrl != null && !"".equals(publishedEndpointUrl)) {
            ei.setProperty("publishedEndpointUrl", publishedEndpointUrl);
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
            ep.getInInterceptors().add(WSDLGetInterceptor.INSTANCE);
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
        serviceFactory.sendEvent(FactoryBeanListener.Event.ENDPOINT_SELECTED, ei, ep,
                                 serviceFactory.getServiceClass(), getServiceClass());
        return ep;
    }
    private void modifyTransportIdPerAddress(EndpointInfo ei) {
        //get chance to set transportId according to the the publish address prefix
        //this is useful for local & camel transport
        if (transportId == null && getAddress() != null) {
            DestinationFactory df = getDestinationFactory();
            if (df == null) {
                DestinationFactoryManager dfm = getBus().getExtension(
                        DestinationFactoryManager.class);
                df = dfm.getDestinationFactoryForUri(getAddress());
            }

            if (df != null) {
                transportId = df.getTransportIds().get(0);
            } else {
                // check conduits (the address could be supported on
                // client only)
                ConduitInitiatorManager cim = getBus().getExtension(
                        ConduitInitiatorManager.class);
                ConduitInitiator ci = cim
                        .getConduitInitiatorForUri(getAddress());
                if (ci != null) {
                    transportId = ci.getTransportIds().get(0);
                }
            }
        }
        if (transportId != null) {
            ei.setTransportId(transportId);
        }
    }

    protected void initializeServiceFactory() {
        Class<?> cls = getServiceClass();

        serviceFactory.setServiceClass(cls);
        serviceFactory.setBus(getBus());
        if (dataBinding != null) {
            serviceFactory.setDataBinding(dataBinding);
        }
    }

    protected abstract String detectTransportIdFromAddress(String ad);
    protected abstract WSDLEndpointFactory getWSDLEndpointFactory();

    protected EndpointInfo createEndpointInfo(BindingInfo bindingInfo) throws BusException {
        // setup the transport ID for the soap over jms if there is only address information
        if (transportId == null && getAddress() != null
            && getAddress().startsWith("jms:") && !"jms://".equals(getAddress())) {
            // Set the transportId to be soap over jms transport
            transportId = SoapJMSConstants.SOAP_JMS_SPECIFICIATION_TRANSPORTID;
        }
        // Get the Service from the ServiceFactory if specified
        Service service = serviceFactory.getService();
        if (bindingInfo == null) {
            // SOAP nonsense
            bindingInfo = createBindingInfo();
            if (bindingInfo instanceof SoapBindingInfo
                && (((SoapBindingInfo) bindingInfo).getTransportURI() == null
                || LocalTransportFactory.TRANSPORT_ID.equals(transportId))) {
                ((SoapBindingInfo) bindingInfo).setTransportURI(transportId);
                transportId = "http://schemas.xmlsoap.org/wsdl/soap/";
            }
            service.getServiceInfos().get(0).addBinding(bindingInfo);
        }
        if (transportId == null) {
            if (bindingInfo instanceof SoapBindingInfo) {
                transportId = ((SoapBindingInfo)bindingInfo).getTransportURI();
            }
            if (transportId == null
                && getAddress() != null
                && getAddress().contains("://")) {
                transportId = detectTransportIdFromAddress(getAddress());
            }
            if (transportId == null) {
                transportId = "http://schemas.xmlsoap.org/wsdl/http/";
            }
        }

        setTransportId(transportId);

        WSDLEndpointFactory wsdlEndpointFactory = getWSDLEndpointFactory();
        EndpointInfo ei;
        if (wsdlEndpointFactory != null) {
            ei = wsdlEndpointFactory.createEndpointInfo(bus, service.getServiceInfos().get(0), bindingInfo, null);
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

        if (wsdlEndpointFactory != null) {
            wsdlEndpointFactory.createPortExtensors(bus, ei, service);
        }
        service.getServiceInfos().get(0).addEndpoint(ei);

        serviceFactory.sendEvent(FactoryBeanListener.Event.ENDPOINTINFO_CREATED, ei);
        return ei;
    }


    protected SoapBindingConfiguration createSoapBindingConfig() {
        return new SoapBindingConfiguration();
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
                    bindingConfig = createSoapBindingConfig();
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
                Method m = serviceFactory.getMethodDispatcher().getMethod(boi);
                serviceFactory.sendEvent(FactoryBeanListener.Event.BINDING_OPERATION_CREATED, inf, boi, m);
            }
            serviceFactory.sendEvent(FactoryBeanListener.Event.BINDING_CREATED, inf);
            return inf;
        } catch (BusException ex) {
            throw new ServiceConstructionException(
                   new Message("COULD.NOT.RESOLVE.BINDING", LOG, bindingId), ex);
        }
    }

    public Class<?> getServiceClass() {
        return serviceClass;
    }

    /**
     * Specifies the class implementing the service.
     *
     * @param serviceClass the service's implementaiton class
     */
    public void setServiceClass(Class<?> serviceClass) {
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
