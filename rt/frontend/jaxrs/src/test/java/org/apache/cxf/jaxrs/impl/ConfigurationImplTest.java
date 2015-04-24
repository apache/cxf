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

import java.io.IOException;
import java.util.Collections;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

import org.junit.Assert;
import org.junit.Test;

public class ConfigurationImplTest extends Assert {
    
    @Test
    public void testIsRegistered() {
        ConfigurationImpl c = new ConfigurationImpl(RuntimeType.SERVER);
        ContainerResponseFilter filter = new ContainerResponseFilterImpl();
        assertTrue(c.register(filter, 
                   Collections.<Class<?>, Integer>singletonMap(ContainerResponseFilter.class, 1000)));
        assertTrue(c.isRegistered(filter));
        assertTrue(c.isRegistered(ContainerResponseFilterImpl.class));
        assertFalse(c.isRegistered(ContainerResponseFilter.class));
        assertFalse(c.register(filter, 
                              Collections.<Class<?>, Integer>singletonMap(ContainerResponseFilter.class, 1000)));
    }
    private static class ContainerResponseFilterImpl implements ContainerResponseFilter {

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
            // TODO Auto-generated method stub
            
        }
        
    }
    
}
