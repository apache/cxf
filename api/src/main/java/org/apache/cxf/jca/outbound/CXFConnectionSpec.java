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
package org.apache.cxf.jca.outbound;

import java.net.URL;

import javax.resource.spi.ConnectionRequestInfo;
import javax.xml.namespace.QName;

import org.apache.commons.lang.ObjectUtils;

/**
 *
 * The CXFConnectionSpec contains properties that needed for obtaining
 * a {@link CXFConnection}.
 */
public class CXFConnectionSpec implements ConnectionRequestInfo {
        
    private Class<?> serviceClass;
    private URL wsdlURL;
    private URL busConfigURL;
    private QName serviceName;
    private QName endpointName;
    private String address;
    
    /**
     * @return the serviceClass
     */
    public Class<?> getServiceClass() {
        return serviceClass;
    }
    /**
     * @param serviceClass the serviceClass to set
     */
    public void setServiceClass(Class<?> serviceClass) {
        this.serviceClass = serviceClass;
    }
    /**
     * @return the wsdlURL
     */
    public URL getWsdlURL() {
        return wsdlURL;
    }
    /**
     * @param wsdlURL the wsdlURL to set
     */
    public void setWsdlURL(URL wsdlURL) {
        this.wsdlURL = wsdlURL;
    }
    /**
     * @return the busConfigURL
     */
    public URL getBusConfigURL() {
        return busConfigURL;
    }
    /**
     * @param busConfigURL the busConfigURL to set
     */
    public void setBusConfigURL(URL busConfigURL) {
        this.busConfigURL = busConfigURL;
    }
    /**
     * @return the serviceName
     */
    public QName getServiceName() {
        return serviceName;
    }
    /**
     * @param serviceName the serviceName to set
     */
    public void setServiceName(QName serviceName) {
        this.serviceName = serviceName;
    }
    /**
     * @return the endpointName
     */
    public QName getEndpointName() {
        return endpointName;
    }
    /**
     * @param endpointName the endpointName to set
     */
    public void setEndpointName(QName endpointName) {
        this.endpointName = endpointName;
    }
    /**
     * @return the address
     */
    public String getAddress() {
        return address;
    }
    /**
     * @param address the address to set
     */
    public void setAddress(String address) {
        this.address = address;
    }
    
    // Required by JCA Spec 
    public boolean equals(Object other) {
        if (!this.getClass().isAssignableFrom(other.getClass())) {
            return false;
        }
        
        CXFConnectionSpec that = (CXFConnectionSpec)other;
        
        if (!ObjectUtils.equals(that.getWsdlURL(), wsdlURL)) {
            return false;
        }
        
        if (!ObjectUtils.equals(that.getBusConfigURL(), busConfigURL)) {
            return false;
        }
        
        if (!ObjectUtils.equals(that.getServiceClass(), serviceClass)) {
            return false;
        }
        
        if (!ObjectUtils.equals(that.getServiceName(), serviceName)) {
            return false;
        }
        
        if (!ObjectUtils.equals(that.getEndpointName(), endpointName)) {
            return false;
        }
        
        if (!ObjectUtils.equals(that.getAddress(), address)) {
            return false;
        }
 
        return true;
    }

    // Required by JCA Spec
    public int hashCode() {
        int retval = 0;
        
        if (wsdlURL != null) {
            retval += wsdlURL.hashCode();
        }
        
        if (busConfigURL != null) {
            retval += busConfigURL.hashCode();
        }
        
        if (serviceClass != null) {
            retval += serviceClass.hashCode();
        }
        
        if (serviceName != null) {
            retval += serviceName.hashCode();
        }
        
        if (endpointName != null) {
            retval += endpointName.hashCode();
        }
        
        if (address != null) {
            retval += address.hashCode();
        }
        
        return retval;
    
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("serviceName=" + serviceName);
        buf.append(",endpointName=" + endpointName);
        buf.append(",wsdlURL=" + wsdlURL);
        buf.append(",busConfigURL=" + busConfigURL);
        buf.append(",serviceClass=" + serviceClass);
        buf.append(",address=" + address);
        return buf.toString();
    }
    
}
