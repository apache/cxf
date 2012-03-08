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

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.xml.ws.Endpoint;
import javax.xml.ws.soap.SOAPBinding;
import javax.xml.ws.spi.Provider;
import javax.xml.ws.wsaddressing.W3CEndpointReference;

import org.apache.cxf.wsn.AbstractEndpoint;
import org.apache.cxf.wsn.EndpointManager;
import org.apache.cxf.wsn.EndpointRegistrationException;
import org.apache.cxf.wsn.util.WSNHelper;

public class JaxwsEndpointManager implements EndpointManager {
    protected MBeanServer mbeanServer;
    


    public void setMBeanServer(MBeanServer s) {
        mbeanServer = s;
    }
    
    
    public Endpoint register(String address, Object service) throws EndpointRegistrationException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            if (WSNHelper.setClassLoader()) {
                Thread.currentThread().setContextClassLoader(JaxwsEndpointManager.class.getClassLoader());
            }
            String bindingId = SOAPBinding.SOAP11HTTP_BINDING;
            if (isCXF()) {
                bindingId = SOAPBinding.SOAP12HTTP_BINDING;
            }
            Endpoint endpoint = Endpoint.create(bindingId, service);
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
