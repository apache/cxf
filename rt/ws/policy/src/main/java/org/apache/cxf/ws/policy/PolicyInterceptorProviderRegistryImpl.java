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

package org.apache.cxf.ws.policy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.configuration.spring.MapProvider;
import org.apache.cxf.extension.BusExtension;
import org.apache.cxf.extension.RegistryImpl;
import org.apache.cxf.interceptor.Interceptor;

/**
 * 
 */
public class PolicyInterceptorProviderRegistryImpl 
    extends RegistryImpl<QName, PolicyInterceptorProvider> 
    implements PolicyInterceptorProviderRegistry, BusExtension {

    public PolicyInterceptorProviderRegistryImpl() {
        super(null);
    }

    public PolicyInterceptorProviderRegistryImpl(Map<QName, PolicyInterceptorProvider> interceptors) {
        super(interceptors);
    }    
    public PolicyInterceptorProviderRegistryImpl(MapProvider<QName, PolicyInterceptorProvider> interceptors) {
        super(interceptors.createMap());
    }    

    public void register(PolicyInterceptorProvider provider) {
        for (QName qn : provider.getAssertionTypes()) {
            super.register(qn, provider);
        }
    }

    public Class<?> getRegistrationType() {
        return PolicyInterceptorProviderRegistry.class;
    }
    
    public List<Interceptor> getInterceptors(Collection<PolicyAssertion> alternative, 
                                             boolean out, boolean fault) {
        List<Interceptor> interceptors = new ArrayList<Interceptor>();
        for (PolicyAssertion a : alternative) {
            if (a.isOptional()) {
                continue;
            }
            QName qn = a.getName();
            PolicyInterceptorProvider pp = get(qn);
            if (null != pp) {
                interceptors.addAll(out                
                    ? (fault ? pp.getOutFaultInterceptors() : pp.getOutInterceptors())       
                    : (fault ? pp.getInFaultInterceptors() : pp.getInInterceptors())); 
            }
        }
        return interceptors;
    }
}
