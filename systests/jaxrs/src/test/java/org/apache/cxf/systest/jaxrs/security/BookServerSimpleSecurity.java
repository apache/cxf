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

import java.net.URL;

import org.apache.cxf.systest.jaxrs.AbstractSpringServer;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.LoginService;



public class BookServerSimpleSecurity extends AbstractSpringServer {
    public static final int PORT = allocatePortAsInt(BookServerSimpleSecurity.class);

    public BookServerSimpleSecurity() {
        super("/jaxrs_simple_security", PORT);
    }

    @Override
    protected void configureServer(org.eclipse.jetty.server.Server server) throws Exception {
        URL resource = getClass()
            .getResource("/org/apache/cxf/systest/jaxrs/security/jetty-realm.properties");
        LoginService realm =
            new HashLoginService("BookStoreRealm", resource.toURI().getPath());
        server.addBean(realm);
    }

    public static void main(String[] args) {
        try {
            BookServerSimpleSecurity s = new BookServerSimpleSecurity();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }

}
