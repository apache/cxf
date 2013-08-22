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

package org.apache.cxf.jaxws;

import java.lang.reflect.Field;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.spi.ServiceDelegate;

import org.apache.cxf.Bus;
import org.apache.cxf.common.util.ReflectionUtil;

/**
 * 
 */
public abstract class CXFService extends Service {
    ServiceImpl impl;
    
    protected CXFService(URL wsdlURL, QName serviceName) {
        super(wsdlURL, serviceName);
        impl = findDelegate();
        impl.initialize(null, wsdlURL);
    } 
    protected CXFService(Bus b, URL wsdlURL, QName serviceName) {
        super(wsdlURL, serviceName);
        impl = findDelegate();
        impl.initialize(b, wsdlURL);
    } 
    protected CXFService(URL wsdlURL, QName serviceName, WebServiceFeature ... f) {
        super(wsdlURL, serviceName);
        impl = findDelegate();
        impl.initialize(null, wsdlURL, f);
    } 
    protected CXFService(Bus b, URL wsdlURL, QName serviceName, WebServiceFeature ... f) {
        super(wsdlURL, serviceName);
        impl = findDelegate();
        impl.initialize(b, wsdlURL, f);
    } 
    
    private ServiceImpl findDelegate() {
        for (Field f : ReflectionUtil.getDeclaredFields(Service.class)) {
            if (ServiceDelegate.class.equals(f.getType())) { 
                return ReflectionUtil.accessDeclaredField(f, this, ServiceImpl.class);
            }
        }
        throw null;
    }

}
