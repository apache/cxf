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

package org.apache.cxf.wsn.util;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.soap.SOAPBinding;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.wsn.wsdl.WSNWSDLLocator;

/**
 *
 */
public class CXFWSNHelper extends WSNHelper {

    public boolean supportsExtraClasses() {
        return true;
    }

    @Override
    public <T> T getPort(String address,
                         Class<T> serviceInterface,
                         Class<?>... extraClasses) {

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            if (setClassLoader) {
                Thread.currentThread().setContextClassLoader(WSNHelper.class.getClassLoader());
            }

            JaxWsProxyFactoryBean jwfb = new JaxWsProxyFactoryBean();
            jwfb.getClientFactoryBean().setWsdlURL(WSNWSDLLocator.getWSDLUrl().toExternalForm());
            jwfb.setServiceName(new QName("http://cxf.apache.org/wsn/jaxws",
                                          serviceInterface.getSimpleName() + "Service"));
            jwfb.setEndpointName(new QName("http://cxf.apache.org/wsn/jaxws",
                                           serviceInterface.getSimpleName() + "Port"));
            jwfb.setAddress(address);
            if (extraClasses != null && extraClasses.length > 0) {
                Map<String, Object> props = new HashMap<>();
                props.put("jaxb.additionalContextClasses", extraClasses);
                jwfb.getClientFactoryBean().getServiceFactory().setProperties(props);
            }
            return jwfb.create(serviceInterface);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }
    public Endpoint publish(String address, Object o, Class<?> ... extraClasses) {
        Endpoint endpoint = Endpoint.create(SOAPBinding.SOAP12HTTP_BINDING, o);
        if (extraClasses != null && extraClasses.length > 0) {
            Map<String, Object> props = new HashMap<>();
            props.put("jaxb.additionalContextClasses", extraClasses);
            endpoint.setProperties(props);
        }
        URL wsdlLocation = WSNWSDLLocator.getWSDLUrl();
        if (wsdlLocation != null) {
            try {
                if (endpoint.getProperties() == null) {
                    endpoint.setProperties(new HashMap<String, Object>());
                }
                endpoint.getProperties().put(Message.WSDL_DESCRIPTION, wsdlLocation.toExternalForm());
                List<Source> mt = new ArrayList<>();
                StreamSource src = new StreamSource(wsdlLocation.openStream(), wsdlLocation.toExternalForm());
                mt.add(src);
                endpoint.setMetadata(mt);
            } catch (IOException e) {
                //ignore, no wsdl really needed
            }
        }
        endpoint.publish(address);
        return endpoint;
    }
}
