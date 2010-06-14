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

import java.util.logging.Logger;

import javax.jws.WebService;
import javax.xml.ws.Provider;
import javax.xml.ws.WebServiceProvider;

import org.apache.cxf.common.logging.LogUtils;

public final class EndpointUtils {

    private static final Logger LOG = LogUtils.getL7dLogger(EndpointUtils.class);

    private EndpointUtils() {
        // Utility class - never constructed
    }

    public static boolean hasWebServiceAnnotation(Class<?> cls) {
        if (cls == null) {
            return false;
        }
        if (null != cls.getAnnotation(WebService.class)) {
            return true;
        }
        for (Class<?> inf : cls.getInterfaces()) {
            if (null != inf.getAnnotation(WebService.class)) {
                return true;
            }
        }
        
        return hasWebServiceAnnotation(cls.getSuperclass());
    }
    
    private static boolean hasWebServiceProviderAnnotation(Class<?> cls) {
        if (cls == null) {
            return false;
        }
        if (null != cls.getAnnotation(WebServiceProvider.class)) {
            return true;
        }
        for (Class<?> inf : cls.getInterfaces()) {
            if (null != inf.getAnnotation(WebServiceProvider.class)) {
                return true;
            }
        }
        return hasWebServiceProviderAnnotation(cls.getSuperclass());
    }
    
    public static boolean isValidImplementor(Object implementor) {
        if (Provider.class.isAssignableFrom(implementor.getClass())
            && hasWebServiceProviderAnnotation(implementor.getClass())) {
            return true;
        }

        // implementor MUST be an instance of a class with a WebService
        // annotation
        // (that implements an SEI) OR a Provider

        if (hasWebServiceAnnotation(implementor.getClass())) {
            return true;
        }

        LOG.info("Implementor is not annotated with WebService annotation.");
        return false;
    } 
    
    
}
