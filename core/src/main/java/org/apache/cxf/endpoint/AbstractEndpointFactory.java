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
package org.apache.cxf.endpoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.BindingConfiguration;
import org.apache.cxf.binding.BindingFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.AbstractBasicInterceptorProvider;
import org.apache.cxf.interceptor.AnnotationInterceptors;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

public abstract class AbstractEndpointFactory extends AbstractBasicInterceptorProvider {
    private static final Logger LOG = LogUtils.getL7dLogger(AbstractEndpointFactory.class);
    private static final String PRIVATE_ENDPOINT = "org.apache.cxf.endpoint.private";
    private static final String PRIVATE_ENDPOINTS = "org.apache.cxf.private.endpoints";

    protected Bus bus;
    protected String address;
    protected String transportId;
    protected String bindingId;
    protected DataBinding dataBinding;
    protected BindingFactory bindingFactory;
    protected DestinationFactory destinationFactory;
    protected String publishedEndpointUrl;
    protected QName endpointName;
    protected QName serviceName;
    protected Map<String, Object> properties;
    protected List<Feature> features;
    protected BindingConfiguration bindingConfig;
    protected EndpointReferenceType endpointReference;
    protected ConduitSelector conduitSelector;

    protected abstract Endpoint createEndpoint() throws BusException, EndpointException;

    protected abstract BindingInfo createBindingInfo();

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Bus getBus() {
        return getBus(true);
    }

    public Bus getBus(boolean createIfNeeded) {
        if (bus == null && createIfNeeded) {
            bus = BusFactory.getThreadDefaultBus();
        }
        return bus;
    }

    public void setBus(Bus bus) {
        this.bus = bus;
    }

    public String getTransportId() {
        return transportId;
    }

    public void setTransportId(String transportId) {
        this.transportId = transportId;
    }

    public void setBindingId(String bind) {
        bindingId = bind;
    }

    public String getBindingId() {
        return bindingId;
    }

    public void setBindingConfig(BindingConfiguration obj) {
        bindingConfig = obj;
    }

    public BindingConfiguration getBindingConfig() {
        return bindingConfig;
    }

    public DestinationFactory getDestinationFactory() {
        return destinationFactory;
    }

    public void setDestinationFactory(DestinationFactory destinationFactory) {
        this.destinationFactory = destinationFactory;
    }

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

    public void setServiceName(QName name) {
        serviceName = name;
    }

    public QName getServiceName() {
        return serviceName;
    }

    public void setEndpointReference(EndpointReferenceType epr) {
        endpointReference = epr;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
    public Map<String, Object> getProperties(boolean create) {
        if (create && properties == null) {
            properties = new HashMap<>();
        }
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public List<Feature> getFeatures() {
        if (features == null) {
            features = new ArrayList<>();
        }
        return features;
    }

    public void setFeatures(List<? extends Feature> features2) {
        this.features = CastUtils.cast(features2);
    }

    public BindingFactory getBindingFactory() {
        return bindingFactory;
    }

    public void setBindingFactory(BindingFactory bf) {
        this.bindingFactory = bf;
    }

    public ConduitSelector getConduitSelector() {
        return conduitSelector;
    }

    public void setConduitSelector(ConduitSelector selector) {
        conduitSelector = selector;
    }

    public DataBinding getDataBinding() {
        return dataBinding;
    }

    public void setDataBinding(DataBinding dataBinding) {
        this.dataBinding = dataBinding;
    }

    /**
     * Checks if a given endpoint has been marked as private.
     * If yes then its address will be added to a bus list property
     * Note that client factories might also check the endpoint, ex,
     * if the endpoint if private then it is likely no service contract
     * will be available if requested from the remote address hence it has to
     * be availbale locally or generated from the local source
     * @param ep endpoint
     */
    @SuppressWarnings("unchecked")
    protected boolean checkPrivateEndpoint(Endpoint ep) {
        if (PropertyUtils.isTrue(ep.get(PRIVATE_ENDPOINT))) {
            List<String> addresses =
                (List<String>)getBus().getProperty(PRIVATE_ENDPOINTS);
            if (addresses == null) {
                addresses = new LinkedList<>();
            }
            addresses.add(getAddress());
            bus.setProperty(PRIVATE_ENDPOINTS, addresses);
            return true;
        }
        return false;
    }
    /**
     * Add annotated Interceptors and Features to the Endpoint
     * @param ep
     */
    protected void initializeAnnotationInterceptors(Endpoint ep, Class<?> cls) {
        initializeAnnotationInterceptors(ep, new Class<?>[] {cls});
    }
    /**
     * Add annotationed Interceptors and Features to the Endpoint
     * @param ep
     */
    protected void initializeAnnotationInterceptors(Endpoint ep, Class<?> ... cls) {
        AnnotationInterceptors provider = new AnnotationInterceptors(cls);
        if (initializeAnnotationInterceptors(provider, ep)) {
            LOG.fine("Added annotation based interceptors and features");
        }
    }

    protected static void addToBeans(Collection<Object> beans, Object o) {
        if (o instanceof Collection) {
            for (Object o2: (Collection<?>)o) {
                addToBeans(beans, o2);
            }
        } else {
            beans.add(o);
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
}
