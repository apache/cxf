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

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Variant.VariantListBuilder;
import javax.ws.rs.ext.RuntimeDelegate;

import org.junit.Assert;
import org.junit.Test;

public class RuntimeDelegateImplTest extends Assert {

    @Test
    public void testRuntimeDelegate() throws Exception {
        RuntimeDelegate rd = RuntimeDelegate.getInstance();
        assertSame(rd.getClass(), RuntimeDelegateImpl.class);
    }
    
    @Test
    public void testCreateInstance() throws Exception {
        assertSame(ResponseBuilderImpl.class,
                   new RuntimeDelegateImpl().
                       createInstance(ResponseBuilder.class).getClass());
        assertSame(UriBuilderImpl.class,
                   new RuntimeDelegateImpl().
                       createInstance(UriBuilder.class).getClass());
        assertSame(VariantListBuilderImpl.class,
                   new RuntimeDelegateImpl().
                       createInstance(VariantListBuilder.class).getClass());
    }
    
    @Test
    public void testCreateHeaderProvider() throws Exception {
        assertSame(MediaTypeHeaderProvider.class,
                   new RuntimeDelegateImpl().
                       createHeaderDelegate(MediaType.class).getClass());
        assertSame(EntityTagHeaderProvider.class,
                   new RuntimeDelegateImpl().
                       createHeaderDelegate(EntityTag.class).getClass());
        assertSame(CacheControlHeaderProvider.class,
                   new RuntimeDelegateImpl().
                       createHeaderDelegate(CacheControl.class).getClass());
        assertSame(CookieHeaderProvider.class,
                   new RuntimeDelegateImpl().
                       createHeaderDelegate(Cookie.class).getClass());
        assertSame(NewCookieHeaderProvider.class,
                   new RuntimeDelegateImpl().
                       createHeaderDelegate(NewCookie.class).getClass());
    }
    
    
}
