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
import java.util.List;


import org.apache.cxf.BusException;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.classloader.ClassLoaderUtils.ClassLoaderHolder;
import org.apache.cxf.common.util.ClassHelper;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerImpl;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.service.factory.FactoryBeanListener;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.apache.cxf.service.invoker.BeanInvoker;
import org.apache.cxf.service.invoker.FactoryInvoker;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.service.invoker.SingletonFactory;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.wsdl.service.factory.ReflectionServiceFactoryBean;
import org.apache.cxf.wsdl11.WSDLEndpointFactory;


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

    @Override
    protected String detectTransportIdFromAddress(String ad) {
        DestinationFactory df = getDestinationFactory();
        if (df == null) {
            DestinationFactoryManager dfm = getBus().getExtension(DestinationFactoryManager.class);
            df = dfm.getDestinationFactoryForUri(getAddress());
            if (df != null) {
                return df.getTransportIds().get(0);
            }
        }
        return null;
    }

    @Override
    protected WSDLEndpointFactory getWSDLEndpointFactory() {
        if (destinationFactory == null) {
            try {
                destinationFactory = getBus().getExtension(DestinationFactoryManager.class)
                    .getDestinationFactory(transportId);
            } catch (Throwable t) {
                try {
                    Object o = getBus().getExtension(ConduitInitiatorManager.class)
                        .getConduitInitiator(transportId);
                    if (o instanceof WSDLEndpointFactory) {
                        return (WSDLEndpointFactory)o;
                    }
                } catch (Throwable th) {
                    //ignore
                }
            }
        }
        if (destinationFactory instanceof WSDLEndpointFactory) {
            return (WSDLEndpointFactory)destinationFactory;
        }
        return null;
    }

    /**
     * For subclasses that hold onto the created Server, this will return the singleton server.
     * Default returns null as the default factories do not hold onto the server and will
     * create a new one for each call to create();
     * @return
     */
    public Server getServer() {
        return null;
    }

    public Server create() {
        ClassLoaderHolder orig = null;
        try {
            Server server = null;
            try {
                if (bus != null) {
                    ClassLoader loader = bus.getExtension(ClassLoader.class);
                    if (loader != null) {
                        orig = ClassLoaderUtils.setThreadContextClassloader(loader);
                    }
                }

                if (getServiceFactory().getProperties() == null) {
                    getServiceFactory().setProperties(getProperties());
                } else if (getProperties() != null) {
                    getServiceFactory().getProperties().putAll(getProperties());
                }
                if (serviceBean != null && getServiceClass() == null) {
                    setServiceClass(ClassHelper.getRealClass(bus, serviceBean));
                }
                if (invoker != null) {
                    getServiceFactory().setInvoker(invoker);
                } else if (serviceBean != null) {
                    invoker = createInvoker();
                    getServiceFactory().setInvoker(invoker);
                }

                Endpoint ep = createEndpoint();

                getServiceFactory().sendEvent(FactoryBeanListener.Event.PRE_SERVER_CREATE, server, serviceBean,
                                              serviceBean == null
                                              ? getServiceClass() == null
                                                  ? getServiceFactory().getServiceClass()
                                                  : getServiceClass()
                                              : getServiceClass() == null
                                                  ? ClassHelper.getRealClass(getBus(), getServiceBean())
                                                  : getServiceClass());

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

            } catch (EndpointException | BusException | IOException e) {
                throw new ServiceConstructionException(e);
            }

            if (serviceBean != null) {
                Class<?> cls = ClassHelper.getRealClass(getServiceBean());
                if (getServiceClass() == null || cls.equals(getServiceClass())) {
                    initializeAnnotationInterceptors(server.getEndpoint(), cls);
                } else {
                    initializeAnnotationInterceptors(server.getEndpoint(), cls, getServiceClass());
                }
            } else if (getServiceClass() != null) {
                initializeAnnotationInterceptors(server.getEndpoint(), getServiceClass());
            }

            applyFeatures(server);

            getServiceFactory().sendEvent(FactoryBeanListener.Event.SERVER_CREATED, server, serviceBean,
                                          serviceBean == null
                                          ? getServiceClass() == null
                                              ? getServiceFactory().getServiceClass()
                                              : getServiceClass()
                                          : getServiceClass() == null
                                              ? ClassHelper.getRealClass(getServiceBean())
                                              : getServiceClass());

            if (start) {
                try {
                    server.start();
                } catch (RuntimeException re) {
                    server.destroy(); // prevent resource leak
                    throw re;
                }
            }
            getServiceFactory().reset();

            return server;
        } finally {
            if (orig != null) {
                orig.reset();
            }
        }
    }

    @Override
    protected void initializeServiceFactory() {
        super.initializeServiceFactory();
        getServiceFactory().setSchemaLocations(schemaLocations);
    }

    protected void applyFeatures(Server server) {
        if (getFeatures() != null) {
            for (Feature feature : getFeatures()) {
                feature.initialize(server, getBus());
            }
        }
    }

    protected Invoker createInvoker() {
        if (getServiceBean() == null) {
            return new FactoryInvoker(new SingletonFactory(getServiceClass()));
        }
        return new BeanInvoker(getServiceBean());
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
            return ClassHelper.getRealClass(getBus(), serviceBean);
        }
        return getServiceFactory().getServiceClass();
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
     * @param location the URL of the WSDL defining the service interface
     */
    public void setWsdlLocation(String location) {
        setWsdlURL(location);
    }

    public String getWsdlLocation() {
        return getWsdlURL();
    }

}
