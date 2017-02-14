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

package org.apache.cxf.systest.jaxrs.security.oauth;

import java.net.URISyntaxException;

import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.testutil.common.TestUtil;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.webapp.WebAppContext;


public class OAuthServer extends AbstractBusTestServerBase {

    public static final int PORT = Integer.valueOf(TestUtil.getPortNumber("oauth-test"));
    private static final String RESOURCE_PATH =
        "/org/apache/cxf/systest/jaxrs/oauth/server";
    private org.eclipse.jetty.server.Server server;


    protected void run() {

        server = new org.eclipse.jetty.server.Server(PORT);

        WebAppContext webappcontext = new WebAppContext();
        String contextPath = null;
        try {
            contextPath = getClass().getResource(RESOURCE_PATH).toURI().getPath();
        } catch (URISyntaxException e1) {
            e1.printStackTrace();
        }
        webappcontext.setContextPath("/");

        webappcontext.setWar(contextPath);

        HandlerCollection handlers = new HandlerCollection();
        handlers.setHandlers(new Handler[] {webappcontext, new DefaultHandler()});

        server.setHandler(handlers);
        try {
            server.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void tearDown() throws Exception {
        super.tearDown();
        if (server != null) {
            server.stop();
            server.destroy();
            server = null;
        }
    }

}
