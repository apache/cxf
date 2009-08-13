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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Document;

import org.apache.cxf.BusException;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ClassHelper;
import org.apache.cxf.databinding.AbstractDataBinding;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerImpl;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.resource.URIResolver;
import org.apache.cxf.service.factory.FactoryBeanListener;
import org.apache.cxf.service.factory.ReflectionServiceFactoryBean;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.apache.cxf.service.invoker.BeanInvoker;
import org.apache.cxf.service.invoker.FactoryInvoker;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.service.invoker.SingletonFactory;


/**
 * This class helps take a {@link org.apache.cxf.service.Service} and
 * expose as a server side endpoint.
 * If there is no Service, it can create one for you using a
 * {@link ReflectionServiceFactoryBean}.
 * <p>
 * For most scenarios you'll want to just have the ServerFactoryBean handle everything
 * for you. In such a case, usage might look like this:
 * </p>
 * <pre>
 * ServerFactoryBean sf = new ServerFactoryBean();
 * sf.setServiceClass(MyService.class);
 * sf.setAddress("http://localhost:8080/MyService");
 * sf.create();
 * </pre>
 * <p>
 * You can also get more advanced and customize the service factory used:
 * <pre>
 * ReflectionServiceFactory serviceFactory = new ReflectionServiceFactory();
 * serviceFactory.setServiceClass(MyService.class);
 * ..
 * \/\/ Customize service factory here...
 * serviceFactory.setWrapped(false);
 * ...
 * ServerFactoryBean sf = new ServerFactoryBean();
 * sf.setServiceFactory(serviceFactory);
 * sf.setAddress("http://localhost:8080/MyService");
 * sf.create();
 * </pre>
 */
public class ServerFactoryBean extends AbstractWSDLBasedEndpointFactory {
    private static final Logger LOG = LogUtils.getL7dLogger(ServerFactoryBean.class);

    private Server server;
    private boolean start = true;
    private Object serviceBean;
    private List<String> schemaLocations;
    private Invoker invoker;

    public ServerFactoryBean() {
        this(new ReflectionServiceFactoryBean());
    }
    public ServerFactoryBean(ReflectionServiceFactoryBean sbean) {
        super(sbean);
    }

    public String getBeanName() {
        return this.getClass().getName();
    }

    public Server create() {
        try {
            applyExtraClass();
            if (serviceBean != null && getServiceClass() == null) {
                setServiceClass(ClassHelper.getRealClass(serviceBean));
            }
            if (invoker != null) {
                getServiceFactory().setInvoker(invoker);
            } else if (serviceBean != null) {
                invoker = createInvoker();
                getServiceFactory().setInvoker(invoker);
            }

            Endpoint ep = createEndpoint();
            server = new ServerImpl(getBus(),
                                    ep,
                                    getDestinationFactory(),
                                    getBindingFactory());

            if (ep.getService().getInvoker() == null) {
                if (invoker == null) {
                    ep.getService().setInvoker(createInvoker());
                } else {
                    ep.getService().setInvoker(invoker);
                }
            }

        } catch (EndpointException e) {
            throw new ServiceConstructionException(e);
        } catch (BusException e) {
            throw new ServiceConstructionException(e);
        } catch (IOException e) {
            throw new ServiceConstructionException(e);
        }

        if (serviceBean != null) {
            initializeAnnotationInterceptors(server.getEndpoint(),
                                             ClassHelper.getRealClass(getServiceBean()));
        } else if (getServiceClass() != null) {
            initializeAnnotationInterceptors(server.getEndpoint(), getServiceClass());
        }

        applyFeatures();

        getServiceFactory().sendEvent(FactoryBeanListener.Event.SERVER_CREATED, server, serviceBean,
                                      serviceBean == null 
                                      ? getServiceClass() == null 
                                          ? getServiceFactory().getServiceClass() : getServiceClass()
                                          : ClassHelper.getRealClass(getServiceBean()));
        
        if (start) {
            server.start();
        }
        return server;
    }


    @Override
    protected void initializeServiceFactory() {
        super.initializeServiceFactory();

        DataBinding db = getServiceFactory().getDataBinding();
        if (db instanceof AbstractDataBinding && schemaLocations != null) {
            ResourceManager rr = getBus().getExtension(ResourceManager.class);

            List<DOMSource> schemas = new ArrayList<DOMSource>();
            for (String l : schemaLocations) {
                URL url = rr.resolveResource(l, URL.class);

                if (url == null) {
                    URIResolver res;
                    try {
                        res = new URIResolver(l);
                    } catch (IOException e) {
                        throw new ServiceConstructionException(new Message("INVALID_SCHEMA_URL", LOG), e);
                    }

                    if (!res.isResolved()) {
                        throw new ServiceConstructionException(new Message("INVALID_SCHEMA_URL", LOG));
                    }
                    url = res.getURL();
                }

                Document d;
                try {
                    d = DOMUtils.readXml(url.openStream());
                } catch (Exception e) {
                    throw new ServiceConstructionException(
                        new Message("ERROR_READING_SCHEMA", LOG, l), e);
                }
                schemas.add(new DOMSource(d, url.toString()));
            }

            ((AbstractDataBinding)db).setSchemas(schemas);
        }
    }

    protected void applyFeatures() {
        if (getFeatures() != null) {
            for (AbstractFeature feature : getFeatures()) {
                feature.initialize(server, getBus());
            }
        }
    }

    protected void applyExtraClass() {
        Map props = this.getProperties();
        if (props != null && props.get("jaxb.additionalContextClasses") != null) {
            Class[] extraClass = (Class[])this.getProperties().get("jaxb.additionalContextClasses");
            DataBinding dataBinding = getServiceFactory().getDataBinding();
            if (dataBinding instanceof JAXBDataBinding) {
                ((JAXBDataBinding)dataBinding).setExtraClass(extraClass);
            }
        }
    }


    protected Invoker createInvoker() {
        if (getServiceBean() == null) {
            return new FactoryInvoker(new SingletonFactory(getServiceClass()));
        }
        return new BeanInvoker(getServiceBean());
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    /**
     * Whether or not the Server should be started upon creation.
     *
     * @return <code>false</code> if the server should not be started upon creation
     */
    public boolean isStart() {
        return start;
    }

    /**
     * Specifies if the Server should be started upon creation. The
     * default is for Servers to be started upon creation. Passing
     * <code>false</code> tells the factory that the Server will be
     * started manually using the start method.
     *
     * @param start <code>false</code> specifies that the Server will not be started upon creation
     */
    public void setStart(boolean start) {
        this.start = start;
    }

    public Object getServiceBean() {
        return serviceBean;
    }

    public Class<?> getServiceBeanClass() {
        if (serviceBean != null) {
            return ClassHelper.getRealClass(serviceBean);
        } else {
            return getServiceFactory().getServiceClass();
        }
    }

    /**
     * Sets the bean implementing the service. If this is set a
     * <code>BeanInvoker</code> is created for the provided bean.
     *
     * @param serviceBean an instantiated implementation object
     */
    public void setServiceBean(Object serviceBean) {
        this.serviceBean = serviceBean;
    }

    public List<String> getSchemaLocations() {
        return schemaLocations;
    }

    public void setSchemaLocations(List<String> schemaLocations) {
        this.schemaLocations = schemaLocations;
    }

    public Invoker getInvoker() {
        return invoker;
    }

    public void setInvoker(Invoker invoker) {
        this.invoker = invoker;
    }

    /**
     * Specifies the location of the WSDL defining the service interface
     * used by the factory to create services. Typically, the WSDL
     * location is specified as a URL.
     *
     * @param locaiton the URL of the WSDL defining the service interface
     */
    public void setWsdlLocation(String location) {
        setWsdlURL(location);
    }

    public String getWsdlLocation() {
        return getWsdlURL();
    }

}