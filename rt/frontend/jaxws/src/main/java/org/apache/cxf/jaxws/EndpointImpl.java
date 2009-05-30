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

package org.apache.cxf.jaxws;

import java.security.AccessController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.ws.Binding;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServicePermission;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.http.HTTPBinding;
import javax.xml.ws.wsaddressing.W3CEndpointReference;
import javax.xml.ws.wsaddressing.W3CEndpointReferenceBuilder;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.BindingConfiguration;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ClassHelper;
import org.apache.cxf.common.util.ModCountCopyOnWriteArrayList;
import org.apache.cxf.configuration.Configurable;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerImpl;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.jaxws.support.JaxWsEndpointImpl;
import org.apache.cxf.jaxws.support.JaxWsImplementorInfo;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.service.model.EndpointInfo;

public class EndpointImpl extends javax.xml.ws.Endpoint 
    implements InterceptorProvider, Configurable {
    /**
     * This property controls whether the 'publishEndpoint' permission is checked 
     * using only the AccessController (i.e. when SecurityManager is not installed).
     * By default this check is not done as the system property is not set.
     */
    public static final String CHECK_PUBLISH_ENDPOINT_PERMISSON_PROPERTY =
        "org.apache.cxf.jaxws.checkPublishEndpointPermission";

    private static final WebServicePermission PUBLISH_PERMISSION =
        new WebServicePermission("publishEndpoint");
    private static final Logger LOG = LogUtils.getL7dLogger(EndpointImpl.class);
    
    private Bus bus;
    private Object implementor;
    private Server server;
    private JaxWsServerFactoryBean serverFactory;
    private JaxWsServiceFactoryBean serviceFactory;
    private Service service;
    private Map<String, Object> properties;
    private List<Source> metadata;
    private Invoker invoker;
    private Executor executor;
    private String bindingUri;
    private String wsdlLocation;
    private String address;
    private String publishedEndpointUrl;
    private QName endpointName;
    private QName serviceName;
    private Class implementorClass;
    
    private List<String> schemaLocations;
    private List<AbstractFeature> features;
    private List<Interceptor> in = new ModCountCopyOnWriteArrayList<Interceptor>();
    private List<Interceptor> out = new ModCountCopyOnWriteArrayList<Interceptor>();
    private List<Interceptor> outFault  = new ModCountCopyOnWriteArrayList<Interceptor>();
    private List<Interceptor> inFault  = new ModCountCopyOnWriteArrayList<Interceptor>();
    private List<Handler> handlers = new ModCountCopyOnWriteArrayList<Handler>();

    public EndpointImpl(Object implementor) {
        this(BusFactory.getThreadDefaultBus(), implementor);
    }
   
    public EndpointImpl(Bus b, Object implementor, 
                        JaxWsServerFactoryBean sf) {
        this.bus = b;
        this.serverFactory = sf;
        this.implementor = implementor;
    }
    
    /**
     * 
     * @param b
     * @param i The implementor object.
     * @param bindingUri The URI of the Binding being used. Optional.
     * @param wsdl The URL of the WSDL for the service, if different than the URL specified on the
     * WebService annotation. Optional.
     */
    public EndpointImpl(Bus b, Object i, String bindingUri, String wsdl) {
        bus = b;
        implementor = i;
        this.bindingUri = bindingUri;
        wsdlLocation = wsdl == null ? null : new String(wsdl);
        serverFactory = new JaxWsServerFactoryBean();
    }
        
    public EndpointImpl(Bus b, Object i, String bindingUri) {
        this(b, i, bindingUri, (String)null);
    }
   
    public EndpointImpl(Bus bus, Object implementor) {
        this(bus, implementor, (String) null);
    }
    
    public void setBus(Bus b) {
        bus = b;
    }
    public Bus getBus() {
        return bus;
    }
    
    public Binding getBinding() {
        return ((JaxWsEndpointImpl) getEndpoint()).getJaxwsBinding();
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public Executor getExecutor() {
        return executor;
    }

    public Service getService() {
        return service;
    }
    
    public JaxWsServiceFactoryBean getServiceFactory() {
        return serviceFactory;
    }

    
    @Override
    public Object getImplementor() {
        return implementor;
    }

    /**
     * Gets the class of the implementor.
     * @return the class of the implementor object
     */
    public Class getImplementorClass() {
        return implementorClass != null ? implementorClass : ClassHelper.getRealClass(implementor);
    }

    public List<Source> getMetadata() {
        return metadata;
    }

    @Override
    public Map<String, Object> getProperties() {
        if (server != null) {
            return server.getEndpoint();
        }
        if (properties == null) {
            properties = new HashMap<String, Object>();
        }
        return properties;
    }

    @Override
    public boolean isPublished() {
        return server != null;
    }

    @Override
    public void publish(Object arg0) {
        // Since this does not do anything now, just check the permission
        checkPublishPermission();
    }

    @Override
    public void publish(String addr) {
        doPublish(addr);
    }

    public void setServiceFactory(JaxWsServiceFactoryBean sf) {
        serviceFactory = sf;
    }
    
    public void setMetadata(List<Source> metadata) {
        this.metadata = metadata;
    }

    @Override
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
        
        if (server != null) {
            server.getEndpoint().putAll(properties);
        }
    }

    @Override
    public void stop() {
        if (null != server) {
            server.stop();
            server = null;
        }
    }    
   
    public String getBeanName() {
        return endpointName.toString() + ".jaxws-endpoint";
    }

    protected void checkProperties() {
        if (properties != null) {
            if (properties.containsKey("javax.xml.ws.wsdl.description")) {
                wsdlLocation = properties.get("javax.xml.ws.wsdl.description").toString();
            }
            if (properties.containsKey(javax.xml.ws.Endpoint.WSDL_PORT)) {
                endpointName = (QName)properties.get(javax.xml.ws.Endpoint.WSDL_PORT);
            }
            if (properties.containsKey(javax.xml.ws.Endpoint.WSDL_SERVICE)) {
                serviceName = (QName)properties.get(javax.xml.ws.Endpoint.WSDL_SERVICE);
            }
        }
    }
    
    protected void doPublish(String addr) {
        checkPublishPermission();
        
        try {
            ServerImpl serv = getServer(addr);
            if (addr != null) {            
                EndpointInfo endpointInfo = serv.getEndpoint().getEndpointInfo();
                if (!endpointInfo.getAddress().contains(addr)) {
                    endpointInfo.setAddress(addr);
                }
                if (publishedEndpointUrl != null) {
                    // TODO is there a good place to put this key-string as a constant?
                    endpointInfo.setProperty("publishedEndpointUrl", publishedEndpointUrl);
                }
                this.address = endpointInfo.getAddress();
            }
            serv.start();
        } catch (WebServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new WebServiceException(ex);
        }
    }
    
    public ServerImpl getServer() {
        return getServer(null);
    }
    public synchronized ServerImpl getServer(String addr) {
        if (server == null) {
            checkProperties();

            // Initialize the endpointName so we can do configureObject
            QName origEpn = endpointName;
            if (endpointName == null) {
                JaxWsImplementorInfo implInfo = new JaxWsImplementorInfo(getImplementorClass());
                endpointName = implInfo.getEndpointName();
            }
            
            if (serviceFactory != null) {
                serverFactory.setServiceFactory(serviceFactory);
            }

            /*if (serviceName != null) {
                serverFactory.getServiceFactory().setServiceName(serviceName);
            }*/

            configureObject(this);
            endpointName = origEpn;
            
            // Set up the server factory
            serverFactory.setAddress(addr);
            serverFactory.setStart(false);
            serverFactory.setEndpointName(endpointName);
            serverFactory.setServiceBean(implementor);
            serverFactory.setBus(bus);
            serverFactory.setFeatures(getFeatures());
            serverFactory.setInvoker(invoker);
            serverFactory.setSchemaLocations(schemaLocations);
            if (serverFactory.getProperties() != null) {
                serverFactory.getProperties().putAll(properties);
            } else {
                serverFactory.setProperties(properties);
            }
            
            // Be careful not to override any serverfactory settings as a user might
            // have supplied their own.
            if (getWsdlLocation() != null) {
                serverFactory.setWsdlURL(getWsdlLocation());
            }
            
            if (bindingUri != null) {
                serverFactory.setBindingId(bindingUri);
            }

            if (serviceName != null) {
                serverFactory.getServiceFactory().setServiceName(serviceName);
            }
            
            if (implementorClass != null) {
                serverFactory.setServiceClass(implementorClass);
            }
            
            if (executor != null) {
                serverFactory.getServiceFactory().setExecutor(executor);
            }
            if (handlers.size() > 0) {
                serverFactory.addHandlers(handlers);
            }

            configureObject(serverFactory);
            
            server = serverFactory.create();
            
            org.apache.cxf.endpoint.Endpoint endpoint = getEndpoint();
            if (getInInterceptors() != null) {
                endpoint.getInInterceptors().addAll(getInInterceptors());
            }
            if (getOutInterceptors() != null) {
                endpoint.getOutInterceptors().addAll(getOutInterceptors());
            }
            if (getInFaultInterceptors() != null) {
                endpoint.getInFaultInterceptors().addAll(getInFaultInterceptors());
            }
            if (getOutFaultInterceptors() != null) {
                endpoint.getOutFaultInterceptors().addAll(getOutFaultInterceptors());
            }
            
            if (properties != null) {
                endpoint.putAll(properties);
            }
            
            configureObject(endpoint.getService());
            configureObject(endpoint);
            this.service = endpoint.getService();
            
            if (getWsdlLocation() == null) {
                //hold onto the wsdl location so cache won't clear till we go away
                setWsdlLocation(serverFactory.getWsdlURL());
            }
            
            if (serviceName == null) {
                setServiceName(serverFactory.getServiceFactory().getServiceQName());
            }

        }
        return (ServerImpl) server;
    }
    
    org.apache.cxf.endpoint.Endpoint getEndpoint() {
        return ((ServerImpl)getServer(null)).getEndpoint();
    }
    
    private void configureObject(Object instance) {
        Configurer configurer = bus.getExtension(Configurer.class);
        if (null != configurer) {
            configurer.configureBean(instance);
        }
    }
    
    protected void checkPublishPermission() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(PUBLISH_PERMISSION);
        } else if (Boolean.getBoolean(CHECK_PUBLISH_ENDPOINT_PERMISSON_PROPERTY)) {
            AccessController.checkPermission(PUBLISH_PERMISSION);
        }
    }

    public void publish() {
        publish(getAddress());
    }
    
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
    
    /**
    * The published endpoint url is used for excplicitely specifying the url of the
    * endpoint that would show up the generated wsdl definition, when the service is
    * brought on line.
    * @return
    */
    public String getPublishedEndpointUrl() {
        return publishedEndpointUrl;
    }
    
    public void setPublishedEndpointUrl(String publishedEndpointUrl) {
        this.publishedEndpointUrl = publishedEndpointUrl;
    }

    public QName getEndpointName() {
        return endpointName;
    }

    public void setEndpointName(QName endpointName) {
        this.endpointName = endpointName;
    }

    public QName getServiceName() {
        return serviceName;
    }

    public void setServiceName(QName serviceName) {
        this.serviceName = serviceName;
    }

    public String getWsdlLocation() {
        return wsdlLocation;
    }

    public void setWsdlLocation(String wsdlLocation) {
        this.wsdlLocation = wsdlLocation;
    }

    public void setBindingUri(String binding) {
        this.bindingUri = binding;
    }
    
    public String getBindingUri() {
        return this.bindingUri;
    }
    
    public void setDataBinding(DataBinding dataBinding) {
        serverFactory.setDataBinding(dataBinding);
    }
    
    public DataBinding getDataBinding() {
        return serverFactory.getDataBinding();
    }

    public List<Interceptor> getOutFaultInterceptors() {
        return outFault;
    }

    public List<Interceptor> getInFaultInterceptors() {
        return inFault;
    }

    public List<Interceptor> getInInterceptors() {
        return in;
    }

    public List<Interceptor> getOutInterceptors() {
        return out;
    }

    public void setInInterceptors(List<Interceptor> interceptors) {
        in = interceptors;
    }

    public void setInFaultInterceptors(List<Interceptor> interceptors) {
        inFault = interceptors;
    }

    public void setOutInterceptors(List<Interceptor> interceptors) {
        out = interceptors;
    }

    public void setOutFaultInterceptors(List<Interceptor> interceptors) {
        outFault = interceptors;
    }
    public void setHandlers(List<Handler> h) {
        handlers.clear();
        handlers.addAll(h);
    }
    public List<Handler> getHandlers() {
        return handlers;
    }

    public List<AbstractFeature> getFeatures() {
        if (features == null) {
            features = new ArrayList<AbstractFeature>();
        }
        return features;
    }

    public void setFeatures(List<AbstractFeature> features) {
        this.features = features;
    }

    public Invoker getInvoker() {
        return invoker;
    }

    public void setInvoker(Invoker invoker) {
        this.invoker = invoker;
    }

    public void setImplementorClass(Class implementorClass) {
        this.implementorClass = implementorClass;
    }
    
    public void setTransportId(String transportId) {        
        serverFactory.setTransportId(transportId);
    }
    
    public String getTransportId() {
        return serverFactory.getTransportId();
    }
    
    public void setBindingConfig(BindingConfiguration config) {
        serverFactory.setBindingConfig(config);
    }
    
    public BindingConfiguration getBindingConfig() {
        return serverFactory.getBindingConfig();
    }

    public List<String> getSchemaLocations() {
        return schemaLocations;
    }

    public void setSchemaLocations(List<String> schemaLocations) {
        this.schemaLocations = schemaLocations;
    }
    
    public EndpointReference getEndpointReference(Element... referenceParameters) {
        if (!isPublished()) {
            throw new WebServiceException(new Message("ENDPOINT_NOT_PUBLISHED", LOG).toString());
        }

        if (getBinding() instanceof HTTPBinding) {        
            throw new UnsupportedOperationException(new Message("GET_ENDPOINTREFERENCE_UNSUPPORTED_BINDING",
                                                                LOG).toString());
        }        
        
        W3CEndpointReferenceBuilder builder = new W3CEndpointReferenceBuilder();
        builder.address(address);
        builder.serviceName(serviceName);
        builder.endpointName(endpointName);
        if (referenceParameters != null) {
            for (Element referenceParameter : referenceParameters) {
                builder.referenceParameter(referenceParameter);
            }
        }
        builder.wsdlDocumentLocation(wsdlLocation);        
        
        return builder.build();
    }

    public <T extends EndpointReference> T getEndpointReference(Class<T> clazz,
                                                                Element... referenceParameters) {
        if (W3CEndpointReference.class.isAssignableFrom(clazz)) {
            return clazz.cast(getEndpointReference(referenceParameters));
        } else {
            throw new WebServiceException(new Message("ENDPOINTREFERENCE_TYPE_NOT_SUPPORTED", LOG, clazz
                .getName()).toString());
        }
    }
}
