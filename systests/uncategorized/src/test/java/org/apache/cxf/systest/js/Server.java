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

package org.apache.cxf.systest.js;

import java.io.File;
import java.net.URLDecoder;

import org.apache.cxf.js.rhino.ProviderFactory;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

public class Server extends AbstractBusTestServerBase {
    public static final String JS_PORT = allocatePort(Server.class);
    public static final String JSX_PORT = allocatePort(Server.class, 1);
    protected void run()  {
        
        try {            
            ProviderFactory pf = new ProviderFactory();            
            String f = getClass().getResource("resources/hello_world.js").toURI().getPath();
            f = URLDecoder.decode(f, "UTF-8");
            pf.createAndPublish(new File(f), "http://localhost:" + JS_PORT 
                                + "/SoapContext/SoapPort", false);
            f = getClass().getResource("resources/hello_world.jsx").toURI().getPath();
            f = URLDecoder.decode(f, "UTF-8");
            pf.createAndPublish(new File(f), "http://localhost:" + JSX_PORT, false);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            System.err.println("Server main");
            Server s = new Server();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }
}
