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

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.binding.BindingConfiguration;
import org.apache.cxf.binding.BindingFactory;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.databinding.PropertiesAwareDataBinding;
import org.apache.cxf.endpoint.AbstractEndpointFactory;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.UserResource;
import org.apache.cxf.jaxrs.provider.DataBindingProvider;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.factory.FactoryBeanListener;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;


/**
 * Abstract bean holding functionality common for creating 
 * JAX-RS Server and Client objects.
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
    
    /**
     * {@inheritDoc}
     */
    public Bus getBus() {
        Bus b = super.getBus();
        checkBindingFactory(b);
        return b;
    }

    /**
     * {@inheritDoc}
     */
    public void setServiceName(QName name) {
        super.setServiceName(name);
        serviceFactory.setServiceName(name);
    }
    
    private void checkBindingFactory(Bus bus) {
        BindingFactoryManager bfm = bus.getExtension(BindingFactoryManager.class);
        try {
            bfm.getBindingFactory(JAXRSBindingFactory.JAXRS_BINDING_ID);
        } catch (Throwable b) {
            //not registered, let's register one
            bfm.registerBindingFactory(JAXRSBindingFactory.JAXRS_BINDING_ID, 
                                       new JAXRSBindingFactory(bus));
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setBus(Bus bus) {
        super.setBus(bus);
        checkBindingFactory(bus);
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
            transportId = "http://cxf.apache.org/transports/http";
        }

        setTransportId(transportId);

        EndpointInfo ei = new EndpointInfo();
        ei.setTransportId(transportId);
        ei.setName(serviceFactory.getService().getName());
        ei.setAddress(getAddress());        

        BindingInfo bindingInfo = createBindingInfo();
        ei.setBinding(bindingInfo);
        serviceFactory.sendEvent(FactoryBeanListener.Event.ENDPOINTINFO_CREATED, ei);

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
            BindingInfo bi = bindingFactory.createBindingInfo(serviceFactory.getService(),
                                                              binding, bindingConfig);
            for (BindingOperationInfo boi : bi.getOperations()) {
                serviceFactory.sendEvent(FactoryBeanListener.Event.BINDING_OPERATION_CREATED, boi, boi);
            }

            serviceFactory.sendEvent(FactoryBeanListener.Event.BINDING_CREATED, bi);
            return bi;
        } catch (BusException ex) {
            ex.printStackTrace();
            //do nothing
        }
        return null;
    }

    /**
     * Returns the service factory
     * @return the factory
     */
    public JAXRSServiceFactoryBean getServiceFactory() {
        return serviceFactory;
    }

    /**
     * Sets the custom service factory which processes 
     * the registered classes and providers 
     * @param serviceFactory the factory
     */
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
        
        List<ClassResourceInfo> list = serviceFactory.getRealClassResourceInfo();
        for (ClassResourceInfo cri : list) {
            initializeAnnotationInterceptors(ep, cri.getServiceClass());
            serviceFactory.sendEvent(FactoryBeanListener.Event.ENDPOINT_SELECTED, ei, ep,
                                     cri.getServiceClass());
        }
        return ep;
    }
    
    /**
     * Sets the location of the schema which can be used to validate
     * the incoming XML or JAXB-driven JSON. JAX-RS MessageBodyReader implementations
     * which have the setSchemaLocations method accepting a list of schema locations 
     * will be injected with this value.
     * 
     * @param schema the schema location
     */
    public void setSchemaLocation(String schema) {
        setSchemaLocations(Collections.singletonList(schema));    
    }
    
    /**
     * Sets the locations of the schemas which can be used to validate
     * the incoming XML or JAXB-driven JSON. JAX-RS MessageBodyReader implementations
     * which have the setSchemaLocations method accepting a list of schema locations 
     * will be injected with this value.
     * 
     * For example, if A.xsd imports B.xsd then both A.xsd and B.xsd need to be referenced.
     * 
     * @param schema the schema locations
     */
    public void setSchemaLocations(List<String> schemas) {
        this.schemaLocations = schemas;    
    }
    
    /**
     * @return the list of custom JAX-RS providers
     */
    public List<?> getProviders() {
        return entityProviders;
    }

    /**
     * Sets custom JAX-RS providers.
     * 
     * @param entityProviders the entityProviders
     */
    public void setProviders(List<? extends Object> providers) {
        this.entityProviders = providers;
    }
    
    /**
     * Sets a custom JAX-RS provider.
     * 
     * @param provider the custom provider.
     */
    public void setProvider(Object provider) {
        setProviders(Collections.singletonList(provider));
    }

    protected void checkResources(boolean server) {
        List<ClassResourceInfo> list = serviceFactory.getRealClassResourceInfo();
        if (server) {
            for (Iterator<ClassResourceInfo> it = list.iterator(); it.hasNext();) {
                ClassResourceInfo cri = it.next();
                if (cri.isCreatedFromModel() && cri.getServiceClass() == cri.getResourceClass() 
                    && !InjectionUtils.isConcreteClass(cri.getServiceClass())) {
                    it.remove();
                }
            }
        }
        if (list.size() == 0) {
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
        setDataBindingProvider(factory, ep.getService());
        
        factory.setBus(getBus());
        if (schemaLocations != null) {
            factory.setSchemaLocations(schemaLocations);
        }
        factory.initProviders(serviceFactory.getRealClassResourceInfo());
        ep.put(ProviderFactory.class.getName(), factory);
        return factory;
    }

    protected void setDataBindingProvider(ProviderFactory factory, Service s) {
        
        List<ClassResourceInfo> cris = serviceFactory.getRealClassResourceInfo();
        if (getDataBinding() == null && cris.size() > 0) {
            org.apache.cxf.annotations.DataBinding ann = 
                cris.get(0).getServiceClass().getAnnotation(org.apache.cxf.annotations.DataBinding.class);
            if (ann != null) {
                try {
                    setDataBinding(ann.value().newInstance());
                } catch (Exception ex) {
                    LOG.warning("DataBinding " + ann.value() + " can not be loaded");
                }
            }
        }
        DataBinding db = getDataBinding();
        if (db == null) {
            return;
        }
        if (db instanceof PropertiesAwareDataBinding) {
            Map<Class<?>, Type> allClasses = ResourceUtils.getAllRequestResponseTypes(cris, false);
            Map<String, Object> props = new HashMap<String, Object>();
            props.put(PropertiesAwareDataBinding.TYPES_PROPERTY, allClasses);
            ((PropertiesAwareDataBinding)db).initialize(props);
        } else { 
            if (s instanceof JAXRSServiceImpl) {
                ((JAXRSServiceImpl)s).setCreateServiceModel(true);
            }
            db.initialize(s);
        }
        factory.setUserProviders(Collections.singletonList(new DataBindingProvider(db)));
    }
    
    /**
     * Sets the description of root resources.
     * Can be used to 'attach' the JAX-RS like description to the application
     * classes without adding JAX-RS annotations.
     *   
     * @param resources root resource descriptions 
     */
    public void setModelBeans(UserResource... resources) {
        setModelBeans(Arrays.asList(resources));
    }
    
    /**
     * Sets the description of root resources.
     * Can be used to 'attach' the JAX-RS like description to the application
     * classes without adding JAX-RS annotations.
     *   
     * @param resources root resource descriptions 
     */
    public void setModelBeans(List<UserResource> resources) {
        serviceFactory.setUserResources(resources);
    }
    
    /**
     * Sets the description of root resources with the list of concrete classes.
     * Can be used to 'attach' the JAX-RS like description to the application
     * classes without adding JAX-RS annotations. Some models may only reference
     * interfaces, thus providing a list of concrete classes that will be
     * instantiated is required in such cases.
     *   
     * @param resources root resource descriptions.
     * @param sClasses concrete root resource classes
     */
    public void setModelBeansWithServiceClass(List<UserResource> resources, Class<?>... sClasses) {
        serviceFactory.setUserResourcesWithServiceClass(resources, sClasses);
    }
    
    /**
     * Sets a reference to the external user model, 
     * Example: "classpath:/model/resources.xml"
     * 
     * @param modelRef the reference to the external model resource.
     */
    public void setModelRef(String modelRef) {
        List<UserResource> resources = ResourceUtils.getUserResources(modelRef, getBus());
        if (resources != null) {
            serviceFactory.setUserResources(resources);
        }
    }
    
    /**
     * Sets a reference to the external user model, 
     * Example: "classpath:/model/resources.xml".
     * Some models may only reference interfaces, thus providing a list of 
     * concrete classes that will be instantiated is required in such cases.
     * 
     * @param modelRef the reference to the external model resource.
     * @param sClasses concrete root resource classes
     */
    public void setModelRefWithServiceClass(String modelRef, Class<?>... sClasses) {
        List<UserResource> resources = ResourceUtils.getUserResources(modelRef, getBus());
        if (resources != null) {
            serviceFactory.setUserResourcesWithServiceClass(resources, sClasses);
        }
    }
    
    
}
