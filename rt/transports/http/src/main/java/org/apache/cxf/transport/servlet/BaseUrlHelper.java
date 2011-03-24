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
package org.apache.cxf.transport.servlet;

import javax.servlet.http.HttpServletRequest;

import org.apache.cxf.transport.AbstractDestination;
import org.apache.cxf.wsdl.WSDLLibrary;
import org.apache.cxf.wsdl.http.AddressType;

public final class BaseUrlHelper {
    private BaseUrlHelper() {
    }

    public static String getBaseURL(HttpServletRequest request) {
        String reqPrefix = request.getRequestURL().toString();        
        String pathInfo = request.getPathInfo() == null ? "" : request.getPathInfo();
        //fix for CXF-898
        if (!"/".equals(pathInfo) || reqPrefix.endsWith("/")) {
            String basePath = request.getContextPath() + request.getServletPath();
            int index;
            if (basePath.length() == 0) {
                index = reqPrefix.indexOf(request.getRequestURI());
            } else {
                index = reqPrefix.indexOf(basePath);
            }
            reqPrefix = reqPrefix.substring(0, index + basePath.length());
        }
        return reqPrefix;
    }
    
    public static void makeAddressesAbsolute(HttpServletRequest request, String baseAddress, 
                                              AbstractDestination[] destinations) {
        for (AbstractDestination dest : destinations) {
            String addr = dest.getEndpointInfo().getAddress();
            if (addr == null || addr.length() == 0) {
                addr = "/";
            }
            if (addr != null && !addr.startsWith("http")) {
                String base = baseAddress == null ? BaseUrlHelper.getBaseURL(request) : baseAddress;
                setAddress(dest, base + addr);
            }
        }
    }

    public static void setAddress(AbstractDestination dest, String absAddress) {
        synchronized (dest) {
            dest.getEndpointInfo().setAddress(absAddress);
            if (WSDLLibrary.isAvailable() 
                && dest.getEndpointInfo().getExtensor(AddressType.class) != null) {
                dest.getEndpointInfo().getExtensor(AddressType.class).setLocation(absAddress);
            }
        }
    }
}
