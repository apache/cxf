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

package org.apache.cxf.systest.jaxrs.security;

import org.junit.BeforeClass;
import org.junit.Test;

public class JAXRSSimpleSecurityTest extends AbstractSpringSecurityTest {
    public static final int PORT = BookServerSimpleSecurity.PORT;
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", 
                   launchServer(BookServerSimpleSecurity.class, true));
    }
    
    @Test
    public void testGetBookUserAdminWithFaultInterceptor() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/security1/bookstorestorage/thosebooks/123"; 
        getBook(endpointAddress, "foo", "bar", 403);
        getBook(endpointAddress, "bob", "bobspassword", 200);
    }
    
    @Test
    public void testGetBookUserAdminWithFilter() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/security2/bookstorestorage/thosebooks/123"; 
        getBook(endpointAddress, "foo", "bar", 403);
        getBook(endpointAddress, "bob", "bobspassword", 200);
    }
    
    @Test
    public void testGetBookUserAdminWithAnnotationsInterceptor() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/security3/bookstorestorage/thebook/123"; 
        getBook(endpointAddress, "foo", "bar", 403);
        getBook(endpointAddress, "bob", "bobspassword", 200);
    }
    
    @Test
    public void testGetBookUserAdminWithAnnotationsInterface() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/security5/bookstorestorage/thosebooks"; 
        getBook(endpointAddress, "foo", "bar", 403);
        getBook(endpointAddress, "bob", "bobspassword", 200);
    }
    
    @Test
    public void testGetBookUserAdminWithAnnotationsFilter() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/security4/bookstorestorage/thebook/123"; 
        getBook(endpointAddress, "foo", "bar", 403);
        getBook(endpointAddress, "bob", "bobspassword", 200);
    }
}
