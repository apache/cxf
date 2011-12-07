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
package org.apache.cxf.jaxws.spring;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;

@WebService()
public class PostConstructCalledCount {
    private static int count;
    private static int injectedCount;

    @Resource
    WebServiceContext context;
    
    public static int getCount() {
        return count;
    }
    public static int getInjectedCount() {
        return injectedCount;
    }
    public static void reset() {
        count = 0;
        injectedCount = 0;
    }
    
    
    @PostConstruct
    public void postConstruct() {
        count++;
        if (context != null) {
            injectedCount++;
        }
    }
    @WebMethod(exclude = true)
    public WebServiceContext getContext() {
        return context;
    }
    
    public int doubleIt(int i) {
        return i * 2;
    }
}
