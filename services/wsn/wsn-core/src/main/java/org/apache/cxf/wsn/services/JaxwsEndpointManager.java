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
package org.apache.cxf.wsn.services;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.soap.SOAPBinding;
import jakarta.xml.ws.spi.Provider;
import jakarta.xml.ws.wsaddressing.W3CEndpointReference;
import org.apache.cxf.message.Message;
import org.apache.cxf.wsn.AbstractEndpoint;
import org.apache.cxf.wsn.EndpointManager;
import org.apache.cxf.wsn.EndpointRegistrationException;
import org.apache.cxf.wsn.util.WSNHelper;

public class JaxwsEndpointManager implements EndpointManager {
    protected MBeanServer mbeanServer;



    public void setMBeanServer(MBeanServer s) {
        mbeanServer = s;
    }


    public Endpoint register(String address, Object service, URL wsdlLocation)
        throws EndpointRegistrationException {

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            if (WSNHelper.getInstance().setClassLoader()) {
                Thread.currentThread().setContextClassLoader(JaxwsEndpointManager.class.getClassLoader());
            }
            Endpoint endpoint = createEndpoint(service);
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

            try {
                if (mbeanServer != null
                    && service instanceof AbstractEndpoint) {
                    ObjectName on = ((AbstractEndpoint)service).getMBeanName();
                    if (on != null) {
                        mbeanServer.registerMBean(service, on);
                    }
                }
            } catch (Exception ex) {
                //ignore for now
            }
            return endpoint;
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    protected Endpoint createEndpoint(Object service) {
        String bindingId = SOAPBinding.SOAP11HTTP_BINDING;
        if (isCXF()) {
            bindingId = SOAPBinding.SOAP12HTTP_BINDING;
        }
        return Endpoint.create(bindingId, service);
    }

    private boolean isCXF() {
        return Provider.provider().getClass().getName().contains(".cxf");
    }
    public void unregister(Endpoint endpoint, Object service) throws EndpointRegistrationException {
        try {
            if (mbeanServer != null
                && service instanceof AbstractEndpoint) {
                ObjectName on = ((AbstractEndpoint)service).getMBeanName();
                if (on != null) {
                    mbeanServer.unregisterMBean(on);
                }
            }
        } catch (Exception ex) {
            //ignore for now
        }
        endpoint.stop();
    }

    public W3CEndpointReference getEpr(Endpoint endpoint) {
        return endpoint.getEndpointReference(W3CEndpointReference.class);
    }
}
