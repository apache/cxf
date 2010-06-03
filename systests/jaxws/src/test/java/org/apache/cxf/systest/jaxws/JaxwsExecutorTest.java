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

package org.apache.cxf.systest.jaxws;

import java.util.concurrent.Executor;

import javax.jws.WebService;
import javax.xml.ws.Response;

import org.apache.cxf.greeter_control.AbstractGreeterImpl;
import org.apache.cxf.greeter_control.BasicGreeterService;
import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.greeter_control.types.GreetMeResponse;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 */
public class JaxwsExecutorTest  extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(ServerNoBodyParts.class);

    public static class Server extends AbstractBusTestServerBase {
        
        protected void run()  {            
            GreeterImpl implementor = new GreeterImpl();
            String address = "http://localhost:" + PORT + "/SoapContext/GreeterPort";
            javax.xml.ws.Endpoint.publish(address, implementor);
        }
        

        public static void main(String[] args) {
            try { 
                Server s = new Server(); 
                s.start();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(-1);
            } finally { 
                System.out.println("done!");
            }
        }
        
        @WebService(serviceName = "BasicGreeterService",
                    portName = "GreeterPort",
                    endpointInterface = "org.apache.cxf.greeter_control.Greeter",
                    targetNamespace = "http://cxf.apache.org/greeter_control",
                    wsdlLocation = "testutils/greeter_control.wsdl")
        public class GreeterImpl extends AbstractGreeterImpl {
        }
    }  
 

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class));
    }
         
    @Test
    public void testUseCustomExecutorOnClient() throws Exception {
        BasicGreeterService service = new BasicGreeterService();
        
        class CustomExecutor implements Executor {
            
            private int count;
            
            public void execute(Runnable command) {
                count++;
                command.run();
            }
            
            public int getCount() {
                return count;
            }
        }
        
        CustomExecutor executor = new CustomExecutor();        
        service.setExecutor(executor);
        assertSame(executor, service.getExecutor());
        
        Greeter proxy = service.getGreeterPort();
        updateAddressPort(proxy, PORT);
        
        assertEquals(0, executor.getCount());
        
        Response<GreetMeResponse>  response = proxy.greetMeAsync("cxf");
        int waitCount = 0;
        while (!response.isDone() && waitCount < 15) {
            Thread.sleep(1000);
            waitCount++;
        }
        assertTrue("Response still not received.", response.isDone());
        
        assertEquals(1, executor.getCount());
    }
   
}
