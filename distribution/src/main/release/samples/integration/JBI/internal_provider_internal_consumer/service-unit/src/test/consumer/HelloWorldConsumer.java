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

import javax.jbi.component.ComponentContext;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import org.apache.cxf.jbi.ServiceConsumer;
import org.apache.hello_world.Greeter;
import org.apache.hello_world.HelloWorldService;
import org.apache.hello_world.PingMeFault;


public class HelloWorldConsumer implements ServiceConsumer { 

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
                    
                HelloWorldService service = new HelloWorldService();
                System.out.println("got service");
                Greeter g = service.getSoapPort();
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
            new QName("http://apache.org/hello_world", "HelloWorldService");
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
