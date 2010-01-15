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

package org.apache.cxf.jaxrs;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.core.Context;

public class AbstractCustomer {
    
    @HeaderParam("AHeader")
    private String aHeaderValue;
    
    private String aHeaderValue2;
    
    @Context 
    private ServletContext sContext;
    
    private ServletConfig sConfig;
    private HttpServletRequest request;
    
    public ServletContext getSuperServletContext() {
        return sContext;
    }
    
    @Context
    public void setServletConfig(ServletConfig context) {
        sConfig = context;
    }
    
    public ServletConfig getSuperServletConfig() {
        return sConfig;
    }
    
    public String getAHeader() {
        return aHeaderValue;
    }
    
    @HeaderParam("AHeader2")
    public void setAHeader2(String value) {
        this.aHeaderValue2 = value;
    }
        
    public String getAHeader2() {
        return aHeaderValue2;
    }
    
    public void setHttpServletRequest(HttpServletRequest r) {
        request = r;    
    }
    
    public HttpServletRequest getHttpServletRequest() {
        return request;
    }
    
};
