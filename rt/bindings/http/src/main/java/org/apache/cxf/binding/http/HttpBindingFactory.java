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
package org.apache.cxf.binding.http;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.AbstractBindingFactory;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.binding.http.interceptor.ContentTypeOutInterceptor;
import org.apache.cxf.binding.http.interceptor.DatabindingInSetupInterceptor;
import org.apache.cxf.binding.http.interceptor.DatabindingOutSetupInterceptor;
import org.apache.cxf.binding.http.strategy.ConventionStrategy;
import org.apache.cxf.binding.http.strategy.JRAStrategy;
import org.apache.cxf.binding.http.strategy.ResourceStrategy;
import org.apache.cxf.binding.xml.XMLBinding;
import org.apache.cxf.binding.xml.interceptor.XMLFaultInInterceptor;
import org.apache.cxf.binding.xml.interceptor.XMLFaultOutInterceptor;
import org.apache.cxf.frontend.MethodDispatcher;
import org.apache.cxf.interceptor.AttachmentInInterceptor;
import org.apache.cxf.interceptor.AttachmentOutInterceptor;
import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.factory.ReflectionServiceFactoryBean;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;

public class HttpBindingFactory extends AbstractBindingFactory {

    public static final String HTTP_BINDING_ID = "http://apache.org/cxf/binding/http";
    private List<ResourceStrategy> strategies = new ArrayList<ResourceStrategy>();

    public HttpBindingFactory() {
        strategies.add(new JRAStrategy());
        strategies.add(new ConventionStrategy());
    }

    public Binding createBinding(BindingInfo bi) {
        XMLBinding binding = new XMLBinding(bi);
        
        binding.getInInterceptors().add(new AttachmentInInterceptor());
        binding.getInInterceptors().add(new DatabindingInSetupInterceptor());

        binding.getOutInterceptors().add(new AttachmentOutInterceptor());
        binding.getOutInterceptors().add(new ContentTypeOutInterceptor());

        binding.getOutInterceptors().add(new DatabindingOutSetupInterceptor());
        
        binding.getInFaultInterceptors().add(new XMLFaultInInterceptor());
        
        binding.getOutFaultInterceptors().add(new ContentTypeOutInterceptor());
        binding.getOutFaultInterceptors().add(new StaxOutInterceptor());
        binding.getOutFaultInterceptors().add(new XMLFaultOutInterceptor());
        
        return binding;
    }
    
    
    public BindingInfo createBindingInfo(Service service, String namespace, Object obj) {
        URIMapper mapper = new URIMapper();
        
        ServiceInfo si = service.getServiceInfos().get(0);
        BindingInfo info = new BindingInfo(si, 
                                           HttpBindingFactory.HTTP_BINDING_ID);
        info.setName(new QName(si.getName().getNamespaceURI(), 
                               si.getName().getLocalPart() + "HttpBinding"));
        
        service.put(URIMapper.class.getName(), mapper);
        MethodDispatcher md = (MethodDispatcher) service.get(MethodDispatcher.class.getName()); 

        for (OperationInfo o : si.getInterface().getOperations()) {
            BindingOperationInfo bop = info.buildOperation(o.getName(), o.getInputName(), o.getOutputName());

            info.addOperation(bop);
            
            Method m = md.getMethod(bop);
            
            try {
                Class<?> c = (Class) service.get(ReflectionServiceFactoryBean.ENDPOINT_CLASS);
                if (c != null) {
                    m = c.getMethod(m.getName(), m.getParameterTypes());
                }
            } catch (SecurityException e) {
                throw new ServiceConstructionException(e);
            } catch (NoSuchMethodException e) {
                throw new ServiceConstructionException(e);
            }
            
            // attempt to map the method to a resource using different strategies
            for (ResourceStrategy s : strategies) {
                // Try different ones until we find one that succeeds
                if (s.map(bop, m, mapper)) {
                    break;
                }
            }
        }
        
        return info;
    }

    public List<ResourceStrategy> getStrategies() {
        return strategies;
    }

    public void setStrategies(List<ResourceStrategy> strategies) {
        this.strategies = strategies;
    }    

}
