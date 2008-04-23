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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.xml.namespace.QName;

import org.apache.cxf.configuration.Configurable;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.AbstractAttributedInterceptorProvider;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.workqueue.SynchronousExecutor;

public class JAXRSServiceImpl extends AbstractAttributedInterceptorProvider implements Service, Configurable {
    private List<ClassResourceInfo> classResourceInfos;
    private DataBinding dataBinding;
    private Executor executor;
    private Invoker invoker;
    private Map<QName, Endpoint> endpoints = new HashMap<QName, Endpoint>();
    
    public JAXRSServiceImpl() {
    }

    public JAXRSServiceImpl(List<ClassResourceInfo> cri) {
        this.classResourceInfos = cri;
        executor = SynchronousExecutor.getInstance();    
    }
    
    public String getBeanName() {
        return getName().toString();
    }

    public QName getName() {    
        Class primaryClass = classResourceInfos.get(0).getServiceClass();
        return new QName("jaxrs", primaryClass.getSimpleName());
    }

    public List<ClassResourceInfo> getClassResourceInfos() {
        return classResourceInfos;
    }
    
    public List<ServiceInfo> getServiceInfos() {
        //NOT APPLICABLE
        return null;
    }
    
    public EndpointInfo getEndpointInfo(QName endpoint) {        
        // For WSDL-based services, this is to construct an EndpointInfo
        // (transport, binding, address etc) from WSDL's physical part.
        // not applicable to JAX-RS services.
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
}
