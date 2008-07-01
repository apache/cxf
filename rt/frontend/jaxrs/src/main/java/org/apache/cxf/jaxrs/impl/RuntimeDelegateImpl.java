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

package org.apache.cxf.jaxrs.impl;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.ApplicationConfig;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Variant.VariantListBuilder;
import javax.ws.rs.ext.RuntimeDelegate;



public class RuntimeDelegateImpl extends RuntimeDelegate {
    protected Map<Class, HeaderDelegate> headerProviders 
        = new HashMap<Class, HeaderDelegate>();    

    public RuntimeDelegateImpl() {
        headerProviders.put(MediaType.class, new MediaTypeHeaderProvider());
        headerProviders.put(CacheControl.class, new CacheControlHeaderProvider());
        headerProviders.put(EntityTag.class, new EntityTagHeaderProvider());
        headerProviders.put(Cookie.class, new CookieHeaderProvider());
        headerProviders.put(NewCookie.class, new NewCookieHeaderProvider());
    }
    
    
    
    public <T> T createInstance(Class<T> type) {
        if (type.isAssignableFrom(ResponseBuilder.class)) {
            return type.cast(new ResponseBuilderImpl());
        } 
        if (type.isAssignableFrom(UriBuilder.class)) {
            return type.cast(new UriBuilderImpl());
        }
        if (type.isAssignableFrom(VariantListBuilder.class)) {
            return type.cast(new VariantListBuilderImpl());
        }
        return null;
    }
   
    
    @SuppressWarnings("unchecked")
    @Override
    public <T> HeaderDelegate<T> createHeaderDelegate(Class<T> type) {
        return headerProviders.get(type);
    }



    @Override
    public ResponseBuilder createResponseBuilder() {
        return new ResponseBuilderImpl();
    }



    @Override
    public UriBuilder createUriBuilder() {
        return new UriBuilderImpl();
    }



    @Override
    public VariantListBuilder createVariantListBuilder() {
        return new VariantListBuilderImpl(); 
    }



    @Override
    public <T> T createEndpoint(ApplicationConfig applicationConfig, Class<T> endpointType) 
        throws IllegalArgumentException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
    

}
