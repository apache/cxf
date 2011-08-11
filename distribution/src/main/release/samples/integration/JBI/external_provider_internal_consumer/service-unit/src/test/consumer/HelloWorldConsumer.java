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



package test.consumer;

import java.io.File;
import java.net.URL;

import javax.jbi.component.ComponentContext;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;


import org.apache.cxf.jbi.ServiceConsumer;
import org.apache.hello_world.Greeter;
import org.apache.hello_world.JMSGreeterService;
import org.apache.hello_world.PingMeFault;


public class HelloWorldConsumer implements ServiceConsumer { 

    private static final QName SERVICE_NAME
        = new QName("http://apache.org/hello_world", "JMSGreeterService");

    private volatile boolean running; 
    private ComponentContext ctx; 

    
    public void setComponentContext(ComponentContext cc) { 
        ctx = cc;
    } 

    public void stop() { 
        running = false;
    } 

    public void run() { 
        try { 
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

            running = true;
            waitForEndpointActivation();
            do { 
                System.out.println("getting service");
                URL wsdlURL;
                File wsdlFile = new File(getClass().
                    getResource("/META-INF/hello_world_client.wsdl").toString());
                if (wsdlFile.exists()) {
                    wsdlURL = wsdlFile.toURL();
                } else {
                    wsdlURL = new URL(getClass().
                        getResource("/META-INF/hello_world_client.wsdl").toString());
                }
                System.out.println(wsdlURL);
                JMSGreeterService service = new JMSGreeterService(wsdlURL, SERVICE_NAME);
                System.out.println("got service");
                Greeter g = service.getGreeterPort();
                System.out.println("invoking method");
                String ret = g.greetMe("ffang");

                System.out.println("greetMe service says: " + ret);

                ret = g.sayHi();
                System.out.println("sayHi service says: " + ret);

                g.greetMeOneWay("ffang");
                try {
                    System.out.println("Invoking pingMe, expecting exception...");
                    g.pingMe();
                } catch (PingMeFault ex) {
                    System.out.println("Expected exception: PingMeFault has occurred: " + ex.getMessage());
                }

                Thread.sleep(10000);
            } while (running);
        } catch (Exception ex) { 
            ex.printStackTrace();
        } 
    } 


    protected final void waitForEndpointActivation() { 

        final QName serviceName = 
            new QName("http://apache.org/hello_world", "JMSGreeterService");
        boolean ready = false;
        do { 
            ServiceEndpoint[] eps = ctx.getEndpointsForService(serviceName); 
            if (eps.length == 0) { 
                System.out.println("waiting for endpoints to become active");
                try { 
                    Thread.sleep(5000); 
                } catch (Exception ex) { 
                    //ignore it
                }
            } else {
                System.out.println("endpoints ready, pump starting");
                ready = true;
            } 
        } while(!ready && running);
    } 

}
