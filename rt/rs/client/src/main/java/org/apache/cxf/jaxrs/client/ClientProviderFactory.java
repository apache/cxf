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
import java.util.List;

import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.client.RxInvokerProvider;
import jakarta.ws.rs.core.Configuration;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.util.ClassHelper;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.message.Message;

public final class ClientProviderFactory extends ProviderFactory {
    private List<ProviderInfo<ClientRequestFilter>> clientRequestFilters =
        new ArrayList<>(1);
    private List<ProviderInfo<ClientResponseFilter>> clientResponseFilters =
        new ArrayList<>(1);
    private List<ProviderInfo<ResponseExceptionMapper<?>>> responseExceptionMappers =
        new ArrayList<>(1);
    private RxInvokerProvider<?> rxInvokerProvider;
    private ClientProviderFactory(Bus bus) {
        super(bus);
    }

    public static ClientProviderFactory createInstance(Bus bus) {
        if (bus == null) {
            bus = BusFactory.getThreadDefaultBus();
        }
        ClientProviderFactory factory = new ClientProviderFactory(bus);
        ProviderFactory.initFactory(factory);
        factory.setBusProviders();
        return factory;
    }

    public static ClientProviderFactory getInstance(Message m) {
        return getInstance(m.getExchange().getEndpoint());
    }

    public static ClientProviderFactory getInstance(Endpoint e) {
        return (ClientProviderFactory)e.get(CLIENT_FACTORY_NAME);
    }


    @Override
    protected void setProviders(boolean custom, boolean busGlobal, Object... providers) {
        List<ProviderInfo<? extends Object>> theProviders =
            prepareProviders(custom, busGlobal, providers, null);
        super.setCommonProviders(theProviders, RuntimeType.CLIENT);
        for (ProviderInfo<? extends Object> provider : theProviders) {
            Class<?> providerCls = ClassHelper.getRealClass(getBus(), provider.getProvider());
            if (providerCls == Object.class) {
                // If the provider is a lambda, ClassHelper.getRealClass returns Object.class
                providerCls = provider.getProvider().getClass();
            }
            
            // Check if provider is constrained to client
            if (!constrainedTo(providerCls, RuntimeType.CLIENT)) {
                continue;
            }
            
            if (filterContractSupported(provider, providerCls, ClientRequestFilter.class)) {
                addProviderToList(clientRequestFilters, provider);
            }

            if (filterContractSupported(provider, providerCls, ClientResponseFilter.class)) {
                addProviderToList(clientResponseFilters, provider);
            }

            if (ResponseExceptionMapper.class.isAssignableFrom(providerCls)) {
                addProviderToList(responseExceptionMappers, provider);
            }

            if (RxInvokerProvider.class.isAssignableFrom(providerCls)) {
                this.rxInvokerProvider = RxInvokerProvider.class.cast(provider.getProvider());
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

        return (ResponseExceptionMapper<T>)responseExceptionMappers.stream()
                .filter(em -> handleMapper(em, paramType, m, ResponseExceptionMapper.class, true))
                .map(ProviderInfo::getProvider)
                .sorted(new ProviderFactory.ClassComparator(paramType))
                .findFirst()
                .orElse(null);
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

    public RxInvokerProvider<?> getRxInvokerProvider() {
        return rxInvokerProvider;
    }
}
