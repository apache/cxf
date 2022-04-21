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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.binding.BindingConfiguration;
import org.apache.cxf.binding.BindingFactory;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.databinding.DataBinding;
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
import org.apache.cxf.service.factory.ServiceConstructionException;
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
    protected List<Object> entityProviders = new LinkedList<>();
    private Comparator<?> providerComparator;

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
        serviceFactory.setBus(bus);
    }

    /*
     * EndpointInfo contains information form WSDL's physical part such as
     * endpoint address, binding, transport etc. For JAX-RS based EndpointInfo,
     * as there is no WSDL, these information are set manually, eg, default
     * transport is http, binding is JAX-RS binding, endpoint address is from
     * server mainline.
     */
    protected EndpointInfo createEndpointInfo(Service service) throws BusException {
        String transportId = getTransportId();
        if (transportId == null && getAddress() != null) {
            DestinationFactory df = getDestinationFactory();
            if (df == null) {
                DestinationFactoryManager dfm = getBus().getExtension(DestinationFactoryManager.class);
                df = dfm.getDestinationFactoryForUri(getAddress());
                super.setDestinationFactory(df);
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

        //EndpointInfo ei = new EndpointInfo(service.getServiceInfos().get(0), transportId);
        EndpointInfo ei = new EndpointInfo();
        ei.setTransportId(transportId);
        ei.setName(serviceFactory.getService().getName());
        ei.setAddress(getAddress());

        BindingInfo bindingInfo = createBindingInfo();
        ei.setBinding(bindingInfo);

        if (!StringUtils.isEmpty(publishedEndpointUrl)) {
            ei.setProperty("publishedEndpointUrl", publishedEndpointUrl);
        }
        ei.setName(service.getName());
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
                serviceFactory.sendEvent(FactoryBeanListener.Event.BINDING_OPERATION_CREATED, bi, boi, null);
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

        EndpointInfo ei = createEndpointInfo(service);
        Endpoint ep = new EndpointImpl(getBus(), service, ei);

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
                                     cri.getServiceClass(), null);
        }
        ep.put(JAXRSServiceFactoryBean.class.getName(), serviceFactory);
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
     * @param schemas the schema locations
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
     * Add custom JAX-RS providers to the list of providers
     *
     * @param providers the entity providers
     */
    public void setProviders(List<? extends Object> providers) {
        List<Object> newBeans = new ArrayList<>();
        addToBeans(newBeans, providers);
        this.entityProviders.addAll(newBeans);
    }

    /**
     * Add custom JAX-RS provider to the list of providers
     *
     * @param provider the custom provider.
     */
    public void setProvider(Object provider) {
        entityProviders.add(provider);
    }

    protected void checkResources(boolean server) {
        List<ClassResourceInfo> list = serviceFactory.getRealClassResourceInfo();
        if (server) {
            for (Iterator<ClassResourceInfo> it = list.iterator(); it.hasNext();) {
                ClassResourceInfo cri = it.next();
                if (!isValidClassResourceInfo(cri)) {
                    it.remove();
                }
            }
        }
        if (list.isEmpty()) {
            org.apache.cxf.common.i18n.Message msg =
                new org.apache.cxf.common.i18n.Message("NO_RESOURCES_AVAILABLE",
                                                       BUNDLE);
            LOG.severe(msg.toString());
            throw new ServiceConstructionException(msg);
        }
    }

    protected boolean isValidClassResourceInfo(ClassResourceInfo cri) {
        Class<?> serviceCls = cri.getServiceClass();
        return !(cri.isCreatedFromModel() && serviceCls == cri.getResourceClass()
            && !InjectionUtils.isConcreteClass(serviceCls));
    }

    protected void setupFactory(ProviderFactory factory, Endpoint ep) {
        if (providerComparator != null) {
            factory.setProviderComparator(providerComparator);
        }
        if (entityProviders != null) {
            factory.setUserProviders(entityProviders);
        }
        setDataBindingProvider(factory, ep.getService());

        factory.setBus(getBus());
        factory.initProviders(serviceFactory.getRealClassResourceInfo());
        if (schemaLocations != null) {
            factory.setSchemaLocations(schemaLocations);
        }

        ep.put(factory.getClass().getName(), factory);
    }

    protected void setDataBindingProvider(ProviderFactory factory, Service s) {

        List<ClassResourceInfo> cris = serviceFactory.getRealClassResourceInfo();
        if (getDataBinding() == null && !cris.isEmpty()) {
            org.apache.cxf.annotations.DataBinding ann =
                cris.get(0).getServiceClass().getAnnotation(org.apache.cxf.annotations.DataBinding.class);
            if (ann != null) {
                try {
                    setDataBinding(ann.value().getDeclaredConstructor().newInstance());
                } catch (Exception ex) {
                    LOG.warning("DataBinding " + ann.value() + " can not be loaded");
                }
            }
        }
        DataBinding db = getDataBinding();
        if (db == null) {
            return;
        }
        if (s instanceof JAXRSServiceImpl) {
            ((JAXRSServiceImpl)s).setCreateServiceModel(true);
        }
        db.initialize(s);
        factory.setUserProviders(Collections.singletonList(new DataBindingProvider<Object>(db)));
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
    public void setProviderComparator(Comparator<?> providerComparator) {
        this.providerComparator = providerComparator;
    }


}
