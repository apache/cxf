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

package org.apache.cxf.service.model;

import javax.xml.namespace.QName;

import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.wsdl.EndpointReferenceUtils;

/**
 * The EndpointInfo contains the information for a web service 'port' inside of a service.
 */
public class EndpointInfo extends AbstractDescriptionElement implements NamedItem {
    String transportId;
    ServiceInfo service;
    BindingInfo binding;
    QName name;
    EndpointReferenceType address;
    
    public EndpointInfo() {
    }
    
    public EndpointInfo(ServiceInfo serv, String ns) {
        transportId = ns;
        service = serv;
    }
    
    
    public String getTransportId() {
        return transportId;
    }    
    
    public void setTransportId(String tid) {
        transportId = tid;
    }
    
    public InterfaceInfo getInterface() {
        if (service == null) {
            return null;
        }
        return service.getInterface();
    }
    
    public void setService(ServiceInfo s) {
        service = s;
    }
    public ServiceInfo getService() {
        return service;
    }
    
    public QName getName() {
        return name;
    }
    
    public void setName(QName n) {
        name = n;
    }

    public BindingInfo getBinding() {
        return binding;
    }
    
    public void setBinding(BindingInfo b) {
        binding = b;
    }    
    
    public String getAddress() {
        return (null != address) ? address.getAddress().getValue() : null;
    }
    
    public void setAddress(String addr) {
        if (null == address) {
            address = EndpointReferenceUtils.getEndpointReference(addr);
        } else {
            EndpointReferenceUtils.setAddress(address, addr);
        }
    }
    
    public void setAddress(EndpointReferenceType endpointReference) {
        address = endpointReference;
    }
    
    @Override
    public <T> T getTraversedExtensor(T defaultValue, Class<T> type) {
        T value = getExtensor(type);
        
        if (value == null) {
            if (value == null && binding != null) {
                value = binding.getExtensor(type);
            }
            
            if (service != null && value == null) {
                value = service.getExtensor(type);
            }
            
            if (value == null) {
                value = defaultValue;
            }
        }
        
        return value;
    }

    public EndpointReferenceType getTarget() {
        return address;
    }

    public boolean isSameAs(EndpointInfo epInfo) {
        if (this == epInfo) {
            return true;
        }
        if (epInfo == null) {
            return false;
        }
        return binding.getName().equals(epInfo.binding.getName()) 
            && service.getName().equals(epInfo.service.getName()) 
            && name.equals(epInfo.name);
    }

    public String toString() {
        return "BindingQName=" + (binding == null ? "" : (binding.getName()
                + ", ServiceQName=" + (binding.getService() == null ? "" : binding.getService().getName())))
                + ", QName=" + name;
    }
}
