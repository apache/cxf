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

package org.apache.cxf.systest.http;

import java.net.URL;

import javax.xml.namespace.QName;

import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

public class Server extends AbstractBusTestServerBase {
    public static final String PORT = allocatePort(Server.class);

    private String name;
    private String address;
    private URL configFileURL;
    
    public Server(String[] args) throws Exception {
        this(args[0], args[1], args[2]);
    }
    
    public Server(String n, String addr, String conf) throws Exception {
        name    = n;
        address = addr;
        configFileURL = new URL(conf);
        //System.out.println("Starting " + name 
        //                     + " Server at " + address
        //                     + " with config " + configFileURL);

    }

    protected void run()  {
        // We use a null binding id in the call to EndpointImpl
        // constructor. Why?
        final String nullBindingID = null;

        // We need to specify to use defaults on constructing the
        // bus, because our configuration file doesn't have
        // everything needed.
        final boolean useDefaults = true;

        // We configure a new bus for this server.
        setBus(new SpringBusFactory().createBus(configFileURL, useDefaults));

        // This impl class must have the appropriate annotations
        // to match the WSDL file that we are using.
        Object implementor = new GreeterImpl(name);
        
        // I don't know why this works.
        EndpointImpl ep = 
            new EndpointImpl(
                    getBus(), 
                    implementor,
                    nullBindingID,
                    this.getClass().getResource("resources/greeting.wsdl").toString());
        // How the hell do I know what the name of the 
        // http-destination is from using this call?
        ep.setEndpointName(new QName("http://apache.org/hello_world", name));
        ep.publish(address);
    }


    public static void main(String[] args) {
        try {
            Server s = new Server(args);
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } /*finally {
            System.out.println("done!");
        } */
    }
}

