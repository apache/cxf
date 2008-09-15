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

package org.apache.cxf.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.xml.namespace.QName;

import org.apache.cxf.configuration.Configurable;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.AbstractAttributedInterceptorProvider;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.workqueue.SynchronousExecutor;

public class ServiceImpl extends AbstractAttributedInterceptorProvider implements Service, Configurable {
    private List<ServiceInfo> serviceInfos;
    private DataBinding dataBinding;
    private Executor executor;
    private Invoker invoker;
    private Map<QName, Endpoint> endpoints = new HashMap<QName, Endpoint>();
    
    public ServiceImpl() {
        this((ServiceInfo)null);
    }
    
    public ServiceImpl(ServiceInfo si) {
        serviceInfos = new ArrayList<ServiceInfo>();
        if (si != null) {
            serviceInfos.add(si);
        }
        executor = SynchronousExecutor.getInstance();    
    }
    public ServiceImpl(List<ServiceInfo> si) {
        serviceInfos = si;
        executor = SynchronousExecutor.getInstance();    
    }
    
    public String getBeanName() {
        return getName().toString();
    }

    public QName getName() {
        return serviceInfos.get(0).getName();
    }

    public List<ServiceInfo> getServiceInfos() {
        return serviceInfos;
    }
    
    public EndpointInfo getEndpointInfo(QName endpoint) {
        for (ServiceInfo inf : serviceInfos) {
            EndpointInfo ef = inf.getEndpoint(endpoint);
            if (ef != null) {
                return ef;
            }
        }
        return null;
    }
    

    public Executor getExecutor() {
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public Invoker getInvoker() {
        return invoker;
    }

    public void setInvoker(Invoker invoker) {
        this.invoker = invoker;
    }

    public DataBinding getDataBinding() {
        return dataBinding;
    }

    public void setDataBinding(DataBinding dataBinding) {
        this.dataBinding = dataBinding;
    }

    public Map<QName, Endpoint> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Map<QName, Endpoint> endpoints) {
        this.endpoints = endpoints;
    }

    public void setProperties(Map<String, Object> properties) {
        this.putAll(properties);
    }
    
    @Override
    public String toString() {
        return "[ServiceImpl " + getName() + "]";
    }
}
