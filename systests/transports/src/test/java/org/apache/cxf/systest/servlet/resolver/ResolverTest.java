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

package org.apache.cxf.systest.servlet.resolver;


import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.util.resource.ResourceFactory;


import org.junit.Test;


public class ResolverTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(ResolverTest.class);

    @Test
    public void startServer() throws Throwable {
        Server server = new org.eclipse.jetty.server.Server(Integer.parseInt(PORT));

        WebAppContext webappcontext = new WebAppContext();
        webappcontext.setContextPath("/resolver");
        webappcontext.setBaseResource(ResourceFactory.of(webappcontext).newClassPathResource("/resolver"));
        server.setHandler(new Handler.Sequence(webappcontext, new DefaultHandler()));
        server.start();
        Throwable e = webappcontext.getUnavailableException();
        if (e != null) {
            throw e;
        }
        server.stop();
    }


}

