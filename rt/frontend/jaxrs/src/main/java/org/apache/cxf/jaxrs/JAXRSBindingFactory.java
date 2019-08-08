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


import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.AbstractBindingFactory;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxrs.interceptor.JAXRSDefaultFaultOutInterceptor;
import org.apache.cxf.jaxrs.interceptor.JAXRSInInterceptor;
import org.apache.cxf.jaxrs.interceptor.JAXRSOutInterceptor;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.transport.Destination;

/**
 * The CXF BindingFactory implementation which is used to register
 * CXF JAX-RS interceptors with the runtime.
 */
@NoJSR250Annotations(unlessNull = { "bus" })
public class JAXRSBindingFactory extends AbstractBindingFactory {
    public static final String JAXRS_BINDING_ID = "http://apache.org/cxf/binding/jaxrs";

    private static final Logger LOG = LogUtils.getL7dLogger(JAXRSBindingFactory.class);


    public JAXRSBindingFactory() {
    }
    public JAXRSBindingFactory(Bus b) {
        super(b, Collections.unmodifiableList(Arrays.asList(
                                                            JAXRS_BINDING_ID)));
    }

    public Binding createBinding(BindingInfo bi) {
        JAXRSBinding binding = new JAXRSBinding(bi);

        binding.getInInterceptors().add(new JAXRSInInterceptor());

        binding.getOutInterceptors().add(new JAXRSOutInterceptor());

        binding.getOutFaultInterceptors().add(new JAXRSDefaultFaultOutInterceptor());

        return binding;
    }



    /*
     * The concept of Binding can not be applied to JAX-RS. Here we use
     * Binding merely to make this JAX-RS impl compatible with CXF framework
     */
    public BindingInfo createBindingInfo(Service service, String namespace, Object obj) {
        BindingInfo info = new BindingInfo(null, JAXRSBindingFactory.JAXRS_BINDING_ID);
        info.setName(new QName(JAXRSBindingFactory.JAXRS_BINDING_ID, "binding"));
        return info;
    }

    public void addListener(Destination d, Endpoint e) {
        synchronized (d) {
            if (d.getMessageObserver() != null) {
                throw new ServiceConstructionException(new Message("ALREADY_RUNNING", LOG,
                                                                   e.getEndpointInfo().getAddress()));
            }
            super.addListener(d, e);
        }
    }


}
