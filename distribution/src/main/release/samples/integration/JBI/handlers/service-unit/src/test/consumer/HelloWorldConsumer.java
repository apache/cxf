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
import org.apache.handlers.AddNumbers;
import org.apache.handlers.AddNumbersFault;
import org.apache.handlers.AddNumbersService;


public class HelloWorldConsumer implements ServiceConsumer { 

    static QName serviceName = new QName("http://apache.org/handlers",
                                           "AddNumbersService");

    static QName portName = new QName("http://apache.org/handlers",
                                        "AddNumbersPort");

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
                    
                AddNumbersService service = new AddNumbersService();
                AddNumbers port = (AddNumbers)service.getAddNumbersPort();
        
                try {
                    int number1 = 10;
                    int number2 = 20;

                    System.out.printf("Invoking addNumbers(%d, %d)\n", number1, number2);
                    int result = port.addNumbers(number1, number2);
                    System.out.printf("The result of adding %d and %d is %d.\n\n", number1, number2, result);

                    number1 = 3; 
                    number2 = 5; 

                    System.out.printf("Invoking addNumbers(%d, %d)\n", number1, number2);
                    result = port.addNumbers(number1, number2);
                    System.out.printf("The result of adding %d and %d is %d.\n\n", number1, number2, result);
            
                    number1 = -10;
                    System.out.printf("Invoking addNumbers(%d, %d)\n", number1, number2);
                    result = port.addNumbers(number1, number2);
                    System.out.printf("The result of adding %d and %d is %d.\n", number1, number2, result);

                } catch (AddNumbersFault ex) {
                    System.out.printf("Caught AddNumbersFault: %s\n", ex.getFaultInfo().getMessage());
                }


                Thread.sleep(10000);
            } while (running);
        } catch (Exception ex) { 
            ex.printStackTrace();
        } 
    } 


    protected final void waitForEndpointActivation() { 

        final QName service = 
            new QName("http://apache.org/handlers", "AddNumbersService");
        boolean ready = false;
        do { 
            ServiceEndpoint[] eps = ctx.getEndpointsForService(service); 
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
