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



package demo.hw.server;

import java.util.concurrent.Future;
import java.util.logging.Logger;
import javax.jws.WebService;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Response;

import org.apache.hello_world_async_soap_http.GreeterAsync;
import org.apache.hello_world_async_soap_http.types.GreetMeSometimeResponse;

@WebService(serviceName = "SOAPService", 
            portName = "SoapPort", 
            endpointInterface = "org.apache.hello_world_async_soap_http.GreeterAsync",
            targetNamespace = "http://apache.org/hello_world_async_soap_http")
                  
public class GreeterImpl implements GreeterAsync {
    private static final Logger LOG = 
        Logger.getLogger(GreeterImpl.class.getPackage().getName());
 
     /* (non-Javadoc)
     * @see org.apache.hello_world_soap_http.Greeter#greetMeSometime(java.lang.String)
     */
    public String greetMeSometime(String me) {
        LOG.info("Executing operation greetMeSometime");
        System.out.println("Executing operation greetMeSometime\n");
        return "How are you " + me;
    }
    
    public Future<?>  greetMeSometimeAsync(String requestType, 
                                           AsyncHandler<GreetMeSometimeResponse> asyncHandler) {
        return null; 
        /*not called */
    }
    
    public Response<GreetMeSometimeResponse> greetMeSometimeAsync(String requestType) { 
        return null; 
        /*not called */
    }

    
}
