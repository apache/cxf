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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.xml.namespace.QName;

import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;

/**
 * 
 */
public class WrappedService implements Service {

    private Service wrappedService;
    private DataBinding dataBinding;
    private QName name;
    private List<ServiceInfo> serviceInfos;
    private Map<QName, Endpoint> endpoints;
    private Invoker invoker;
    
    WrappedService(Service wrapped, QName n, ServiceInfo info) {
        wrappedService = wrapped;
        name = n;
        serviceInfos = Collections.singletonList(info);
    }
    
    public Service getWrappedService() {
        return wrappedService;
    }
    
    public DataBinding getDataBinding() {
        return dataBinding;
    }
    
    public QName getName() {
        return name;
    }

    public List<ServiceInfo> getServiceInfos() {
        return serviceInfos;
    }
    public ServiceInfo getServiceInfo() {
        return serviceInfos.get(0);
    }

    public void setDataBinding(DataBinding arg0) {
        dataBinding = arg0;
    }
    
    public Map<QName, Endpoint> getEndpoints() {
        return endpoints;
    }
    
    public Invoker getInvoker() {
        return invoker;
    }
    
    public void setInvoker(Invoker arg0) {
        invoker = arg0;
    }
    
    // remaining APIs all wrapped

    public Executor getExecutor() {
        return wrappedService.getExecutor();
    }

    public void setExecutor(Executor arg0) {
        wrappedService.setExecutor(arg0);
    }

    public List<Interceptor> getInFaultInterceptors() {
        return wrappedService.getInFaultInterceptors();
    }

    public List<Interceptor> getInInterceptors() {
        return wrappedService.getInInterceptors();
    }

    public List<Interceptor> getOutFaultInterceptors() {
        return wrappedService.getOutFaultInterceptors();
    }

    public List<Interceptor> getOutInterceptors() {
        return wrappedService.getOutInterceptors();
    }

    public void clear() {
        wrappedService.clear();
    }

    public boolean containsKey(Object key) {
        return wrappedService.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return wrappedService.containsValue(value);
    }

    public Set<java.util.Map.Entry<String, Object>> entrySet() {
        return wrappedService.entrySet();
    }

    public Object get(Object key) {
        return wrappedService.get(key);
    }

    public boolean isEmpty() {
        return wrappedService.isEmpty();
    }

    public Set<String> keySet() {
        return wrappedService.keySet();
    }

    public Object put(String key, Object value) {
        return wrappedService.put(key, value);
    }

    public void putAll(Map<? extends String, ? extends Object> t) {
        wrappedService.putAll(t);
    }

    public Object remove(Object key) {
        return wrappedService.remove(key);
    }

    public int size() {
        return wrappedService.size();
    }

    public Collection<Object> values() {
        return wrappedService.values();
    }
    
    void setEndpoint(Endpoint e) {
        endpoints = Collections.singletonMap(e.getEndpointInfo().getName(), e);
    }

    public EndpointInfo getEndpointInfo(QName endpoint) {
        return serviceInfos.get(0).getEndpoint(endpoint);
    }


}
