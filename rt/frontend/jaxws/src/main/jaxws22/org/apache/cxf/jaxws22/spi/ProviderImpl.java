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

package org.apache.cxf.jaxws22.spi;

import java.util.Arrays;

import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.spi.Invoker;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.jaxws.EndpointUtils;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.jaxws.context.WebServiceContextImpl;
import org.apache.cxf.jaxws22.EndpointImpl;
import org.apache.cxf.jaxws22.JAXWS22Invoker;

public class ProviderImpl extends org.apache.cxf.jaxws.spi.ProviderImpl {
    @Override
    protected org.apache.cxf.jaxws.EndpointImpl createEndpointImpl(Bus bus,
                                              String bindingId,
                                              Object implementor,
                                              WebServiceFeature ... features) {
        if (isJaxWs22()) {
            return new EndpointImpl(bus, implementor, bindingId, features);
        }
        //couldn't find the 2.2 stuff, assume 2.1 behavior
        return super.createEndpointImpl(bus, bindingId, implementor, features);
    }

    //new in 2.2, but introduces a new class not found in 2.1
    public Endpoint createEndpoint(String bindingId, Class<?> implementorClass,
                                   Invoker invoker, WebServiceFeature ... features) {
        if (EndpointUtils.isValidImplementor(implementorClass)) {
            Bus bus = BusFactory.getThreadDefaultBus();
            JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
            if (features != null) {
                factory.getJaxWsServiceFactory().setWsFeatures(Arrays.asList(features));
            }
            if (invoker != null) {
                factory.setInvoker(new JAXWS22Invoker(invoker));
                try {
                    invoker.inject(new WebServiceContextImpl());
                } catch (Exception e) {
                    throw new WebServiceException(new Message("ENDPOINT_CREATION_FAILED_MSG",
                                                              LOG).toString(), e);
                }
            }
            EndpointImpl ep = new EndpointImpl(bus, null, factory);
            ep.setImplementorClass(implementorClass);
            return ep;
        } else {
            throw new WebServiceException(new Message("INVALID_IMPLEMENTOR_EXC", LOG).toString());
        }
    }
}
