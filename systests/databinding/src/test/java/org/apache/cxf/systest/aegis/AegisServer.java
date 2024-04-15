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

package org.apache.cxf.systest.aegis;


import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.DefaultHandler;


public class AegisServer extends AbstractBusTestServerBase {
    static final String PORT = allocatePort(AegisServer.class);

    private org.eclipse.jetty.server.Server server;

    protected void run() {
        //System.out.println("Starting Server");

        
        try {
            server = new org.eclipse.jetty.server.Server(Integer.parseInt(PORT));

            WebAppContext webappcontext = new WebAppContext();
            webappcontext.setContextPath("/");
            webappcontext.setBaseResourceAsString(this.getClass().getResource("/webapp").toString());
            
            server.setHandler(new Handler.Sequence(webappcontext, new DefaultHandler()));
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
            AegisServer s = new AegisServer();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }

}
