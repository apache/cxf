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

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;

import org.apache.cxf.common.util.UrlUtils;

public abstract class AbstractServletController {
    
    protected static final String DEFAULT_LISTINGS_CLASSIFIER = "/services";
    protected boolean isHideServiceList;
    protected boolean disableAddressUpdates;
    protected String forcedBaseAddress;
    protected String serviceListStyleSheet;
    protected String title;
    protected String serviceListRelativePath = DEFAULT_LISTINGS_CLASSIFIER;
    
    protected AbstractServletController() {
        
    }
    
    protected AbstractServletController(ServletConfig config) {
        init(config);
    }
    
    public void setHideServiceList(boolean generate) {
        isHideServiceList = generate;
    }
    
    public void setServiceListRelativePath(String relativePath) {
        serviceListRelativePath = relativePath;
    }
    
    public void setDisableAddressUpdates(boolean noupdates) {
        disableAddressUpdates = noupdates;
    }
    public void setForcedBaseAddress(String s) {
        forcedBaseAddress = s;
    }
    public void setServiceListStyleSheet(String serviceListStyleSheet) {
        this.serviceListStyleSheet = serviceListStyleSheet;
    }
    public void setTitle(String t) {
        title = t;
    }
    
    private void init(ServletConfig servletConfig) {
        if (servletConfig == null) {
            return;
        }
        
        String hideServiceList = servletConfig.getInitParameter("hide-service-list-page");
        if (hideServiceList != null) {
            isHideServiceList = Boolean.valueOf(hideServiceList);
        }
        String isDisableAddressUpdates = servletConfig.getInitParameter("disable-address-updates");
        if (isDisableAddressUpdates != null) {
            disableAddressUpdates = Boolean.valueOf(isDisableAddressUpdates);
        }
        String isForcedBaseAddress = servletConfig.getInitParameter("base-address");
        if (isForcedBaseAddress != null) {
            forcedBaseAddress = isForcedBaseAddress;
        }
        String serviceListTransform = servletConfig.getInitParameter("service-list-stylesheet");
        if (serviceListTransform != null) {
            serviceListStyleSheet = serviceListTransform;
        }
        String serviceListPath = servletConfig.getInitParameter("service-list-path");
        if (serviceListPath != null) {
            serviceListRelativePath = serviceListPath;
        }
        String configTitle = servletConfig.getInitParameter("service-list-title");
        if (configTitle != null) {
            title = configTitle;
        }
    }
    
    protected String getBaseURL(HttpServletRequest request) {
        String reqPrefix = request.getRequestURL().toString();        
        String pathInfo = request.getPathInfo() == null ? "" : request.getPathInfo();
        //fix for CXF-898
        if (!"/".equals(pathInfo) || reqPrefix.endsWith("/")) {
            // needs to be done given that pathInfo is decoded
            // TODO : it's unlikely servlet path will contain encoded values so we're most 
            // likely safe however we need to ensure if it happens then this code works properly too
            reqPrefix = UrlUtils.pathDecode(reqPrefix);
            // pathInfo drops matrix parameters attached to a last path segment
            int offset = 0;
            int index = getMatrixParameterIndex(reqPrefix, pathInfo);
            if (index >= pathInfo.length()) {
                offset = reqPrefix.length() - index;
            }
            reqPrefix = reqPrefix.substring(0, reqPrefix.length() - pathInfo.length() - offset);
        }
        return reqPrefix;
    }
    
    private int getMatrixParameterIndex(String reqPrefix, String pathInfo) {
        int index = reqPrefix.lastIndexOf(';');
        int lastIndex = -1;
        while (index >= pathInfo.length()) {
            lastIndex = index;
            reqPrefix = reqPrefix.substring(0, index);
            if (reqPrefix.endsWith(pathInfo)) {
                break;
            }
            index = reqPrefix.lastIndexOf(';');
        }
        return lastIndex;
    }
   
}
