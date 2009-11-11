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

package org.apache.cxf.connector;

import java.net.URL;

import javax.xml.namespace.QName;

import org.apache.cxf.jca.cxf.CXFConnectionRequestInfo;


public class CXFConnectionParam {
    private Class iface;
    private URL wsdlLocation;
    private QName serviceName;
    private QName portName;
    private String address;
    
    public CXFConnectionParam() {
    }

    public CXFConnectionParam(Class aIface, URL aWsdlLocation, 
                                       QName aServiceName, QName aPortName) {
        this.iface = aIface;
        this.wsdlLocation = aWsdlLocation;
        this.serviceName = aServiceName;
        this.portName = aPortName;
    }

    public Class<?> getInterface() {
        return iface;
    }

    public URL getWsdlLocation() {
        return wsdlLocation;
    }

    public QName getServiceName() {
        return serviceName;
    }

    public QName getPortName() {
        return portName;
    }

    public String getAddress() {
        return address;
    }


    public boolean equals(java.lang.Object other) {
        boolean result = false;
        if (other instanceof CXFConnectionRequestInfo) {
            CXFConnectionRequestInfo cri = (CXFConnectionRequestInfo)other; 
            result = areEquals(iface, cri.getInterface()) 
                     && areEquals(wsdlLocation, cri.getWsdlLocation())
                     && areEquals(serviceName, cri.getServiceName()) 
                     && areEquals(portName, cri.getPortName())
                     && areEquals(address, cri.getAddress());
        }
        return result;
    }
    
    
  
    public void setAddress(String address) {
        this.address = address;
    }

    public void setPortName(QName portName) {
        this.portName = portName;
    }

    public void setServiceName(QName serviceName) {
        this.serviceName = serviceName;
    }

    public void setWsdlLocation(URL wsdlLocation) {
        this.wsdlLocation = wsdlLocation;
    }
    
    public void setInterface(Class<?> aInterface) {
        this.iface = aInterface;
    }

    public int hashCode() {        
        if (getServiceName() != null) {
            return getInterface().hashCode() ^ getServiceName().hashCode();
        } else {
            return getInterface().hashCode() ^ (getAddress() != null ? getAddress().hashCode() : 1);
        }
    }  

    public String toString() {
        StringBuilder buf = new StringBuilder(256);
        buf.append("Interface [" + getInterface() + "] ");
        buf.append("PortName [" + getPortName() + "] ");
        buf.append("ServiceName [" + getServiceName() + "] ");
        buf.append("WsdlLocation [" + getWsdlLocation() + "] ");
        buf.append("Address [" + getAddress() + "] ");
        return buf.toString();
    }


    private boolean areEquals(Object obj1, Object obj2) {
        if (obj1 == null) {
            return obj1 == obj2; 
        } else {
            return obj1.equals(obj2);
        }            
    }
}
