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

package org.apache.cxf.ws.rm;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import org.apache.cxf.binding.Binding;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.ws.addressing.MAPAggregator;

public class WrappedEndpoint implements Endpoint {

    private Endpoint wrappedEndpoint;
    private EndpointInfo endpointInfo;
    private Service service;
    private Boolean usingAddressing;
    
    WrappedEndpoint(Endpoint wrapped, EndpointInfo info, Service s) {
        wrappedEndpoint = wrapped;
        endpointInfo = info;
        service = s;
    }
    
    public Endpoint getWrappedEndpoint() {
        return wrappedEndpoint;
    }
    
    public EndpointInfo getEndpointInfo() {
        return endpointInfo;
    }  
    
    public Service getService() {
        return service;
    }

    public Binding getBinding() {
        return wrappedEndpoint.getBinding();
    }

    public Executor getExecutor() {
        return wrappedEndpoint.getExecutor();
    }

    public MessageObserver getInFaultObserver() {
        return wrappedEndpoint.getInFaultObserver();
    }

    public MessageObserver getOutFaultObserver() {
        return wrappedEndpoint.getOutFaultObserver();
    }

    public void setExecutor(Executor arg0) {
        wrappedEndpoint.setExecutor(arg0);
    }

    public void setInFaultObserver(MessageObserver arg0) {
        wrappedEndpoint.setInFaultObserver(arg0);
    }

    public void setOutFaultObserver(MessageObserver arg0) {
        wrappedEndpoint.setOutFaultObserver(arg0);
    }

    public List<Interceptor> getInFaultInterceptors() {
        return wrappedEndpoint.getInFaultInterceptors();
    }

    public List<Interceptor> getInInterceptors() {
        return wrappedEndpoint.getInInterceptors();        
    }

    public List<Interceptor> getOutFaultInterceptors() {
        return wrappedEndpoint.getOutFaultInterceptors();
    }

    public List<Interceptor> getOutInterceptors() {
        return wrappedEndpoint.getOutInterceptors();
    }

    public void clear() {
        wrappedEndpoint.clear();
    }

    public boolean containsKey(Object key) {
        return wrappedEndpoint.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return wrappedEndpoint.containsValue(value);
    }

    public Set<Entry<String, Object>> entrySet() {
        return wrappedEndpoint.entrySet();
    }

    public Object get(Object key) {
        if (MAPAggregator.USING_ADDRESSING == key) {
            return usingAddressing;
        }
        return wrappedEndpoint.get(key);
    }

    public boolean isEmpty() {
        return wrappedEndpoint.isEmpty();
    }

    public Set<String> keySet() {
        return wrappedEndpoint.keySet();
    }

    public Object put(String key, Object value) {
        if (MAPAggregator.USING_ADDRESSING == key) {
            usingAddressing = (Boolean)value;
            return null;
        }
        return wrappedEndpoint.put(key, value);
    }

    public void putAll(Map<? extends String, ? extends Object> t) {
        wrappedEndpoint.putAll(t);
    }
    
    public Object remove(Object key) {
        return wrappedEndpoint.remove(key);
    }

    public int size() {
        return wrappedEndpoint.size();
    }

    public Collection<Object> values() {
        return wrappedEndpoint.values();
    }
    
    /**
     * @return the list of fearures <b>already</b> activated for this endpoint.
     */
    public List<AbstractFeature> getActiveFeatures() {
        return wrappedEndpoint.getActiveFeatures();
    }
}
