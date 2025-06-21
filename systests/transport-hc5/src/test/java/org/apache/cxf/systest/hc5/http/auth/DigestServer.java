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
package org.apache.cxf.systest.hc5.http.auth;

import java.net.URL;

import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.eclipse.jetty.ee11.webapp.WebAppContext;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.util.resource.ResourceFactory;

public class DigestServer extends AbstractBusTestServerBase {
    public static final String PORT = allocatePort(DigestServer.class);

    private org.eclipse.jetty.server.Server server;

    private void configureServer() throws Exception {
        URL resource = getClass().getResource("jetty-realm.properties");

        LoginService realm =
            new HashLoginService("BookStoreRealm", 
                ResourceFactory.of(new ContextHandler()).newResource(resource.toString()));
        server.addBean(realm);
    }

    protected void run() {
        server = new org.eclipse.jetty.server.Server(Integer.parseInt(PORT));

        WebAppContext webappcontext = new WebAppContext();
        webappcontext.setContextPath("/digestauth");
        webappcontext.setBaseResourceAsString(this.getClass().getResource("/digestauth").toString());

        Handler.Collection handlers = new Handler.Sequence();
        handlers.setHandlers(new Handler[] {webappcontext, new DefaultHandler()});

        server.setHandler(handlers);

        try {
            configureServer();
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public void tearDown() throws Exception {
        if (server != null) {
            server.stop();
            server.destroy();
            server = null;
        }
    }
    public static void main(String[] args) {
        try {
            DigestServer s = new DigestServer();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        }
    }

}
