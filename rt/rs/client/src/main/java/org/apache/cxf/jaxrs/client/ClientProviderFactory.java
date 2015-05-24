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
package org.apache.cxf.jaxrs.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.Configuration;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.util.ClassHelper;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.message.Message;

public final class ClientProviderFactory extends ProviderFactory {
    private static final String SHARED_CLIENT_FACTORY = "jaxrs.shared.client.factory";
    
    private List<ProviderInfo<ClientRequestFilter>> clientRequestFilters = 
        new ArrayList<ProviderInfo<ClientRequestFilter>>(1);
    private List<ProviderInfo<ClientResponseFilter>> clientResponseFilters = 
        new ArrayList<ProviderInfo<ClientResponseFilter>>(1);
    private List<ProviderInfo<ResponseExceptionMapper<?>>> responseExceptionMappers = 
        new ArrayList<ProviderInfo<ResponseExceptionMapper<?>>>(1);
    
    private ClientProviderFactory(ProviderFactory baseFactory, Bus bus) {
        super(baseFactory, bus);
    }
    
    public static ClientProviderFactory createInstance(Bus bus) {
        if (bus == null) {
            bus = BusFactory.getThreadDefaultBus();
        }
        ClientProviderFactory baseFactory = initBaseFactory(bus);
        ClientProviderFactory factory = new ClientProviderFactory(baseFactory, bus);
        factory.setBusProviders();
        return factory;
    }
    
    public static ClientProviderFactory getInstance(Message m) {
        Endpoint e = m.getExchange().getEndpoint();
        return (ClientProviderFactory)e.get(CLIENT_FACTORY_NAME);
    }
    
    public static ClientProviderFactory getInstance(Endpoint e) {
        return (ClientProviderFactory)e.get(CLIENT_FACTORY_NAME);
    }
       
    private static synchronized ClientProviderFactory initBaseFactory(Bus bus) {
        ClientProviderFactory factory = (ClientProviderFactory)bus.getProperty(SHARED_CLIENT_FACTORY);
        if (factory != null) {
            return factory;
        }
        factory = new ClientProviderFactory(null, bus);
        ProviderFactory.initBaseFactory(factory);
        bus.setProperty(SHARED_CLIENT_FACTORY, factory);
        return factory;
    }
    @Override
    protected void setProviders(Object... providers) {
        List<ProviderInfo<? extends Object>> theProviders = 
            prepareProviders((Object[])providers, null);
        super.setCommonProviders(theProviders);
        for (ProviderInfo<? extends Object> provider : theProviders) {
            Class<?> providerCls = ClassHelper.getRealClass(getBus(), provider.getProvider());
            if (filterContractSupported(provider, providerCls, ClientRequestFilter.class)) {
                addProviderToList(clientRequestFilters, provider);
            }
            
            if (filterContractSupported(provider, providerCls, ClientResponseFilter.class)) {
                addProviderToList(clientResponseFilters, provider);
            }
            
            if (ResponseExceptionMapper.class.isAssignableFrom(providerCls)) {
                addProviderToList(responseExceptionMappers, provider);
            }
        }
        Collections.sort(clientRequestFilters, 
                         new BindingPriorityComparator(ClientRequestFilter.class, true));
        Collections.sort(clientResponseFilters, 
                         new BindingPriorityComparator(ClientResponseFilter.class, false));
        
        injectContextProxies(responseExceptionMappers, clientRequestFilters, clientResponseFilters);
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Throwable> ResponseExceptionMapper<T> createResponseExceptionMapper(
                                 Message m, Class<?> paramType) {
        
        List<ResponseExceptionMapper<?>> candidates = new LinkedList<ResponseExceptionMapper<?>>();
        
        for (ProviderInfo<ResponseExceptionMapper<?>> em : responseExceptionMappers) {
            handleMapper(candidates, em, paramType, m, ResponseExceptionMapper.class, true);
        }
        if (candidates.size() == 0) {
            return null;
        }
        Collections.sort(candidates, new ProviderFactory.ClassComparator(paramType));
        return (ResponseExceptionMapper<T>) candidates.get(0);
    }
    
    @Override
    public void clearProviders() {
        super.clearProviders();
        responseExceptionMappers.clear();
        clientRequestFilters.clear();
        clientResponseFilters.clear();
    }
    
    public List<ProviderInfo<ClientRequestFilter>> getClientRequestFilters() {
        return Collections.unmodifiableList(clientRequestFilters);
    }
    
    public List<ProviderInfo<ClientResponseFilter>> getClientResponseFilters() {
        return Collections.unmodifiableList(clientResponseFilters);
    }

    @Override
    public Configuration getConfiguration(Message m) {
        return (Configuration)m.getExchange().getOutMessage()
            .getContextualProperty(Configuration.class.getName());
    }
    
}
