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

package demo.hw_https.server;

import java.util.logging.Logger;
import org.apache.hello_world_soap_http.Greeter;

@javax.jws.WebService(name = "Greeter", serviceName = "SOAPService", 
                      targetNamespace = "http://apache.org/hello_world_soap_http", 
                      wsdlLocation = "file:./wsdl/hello_world.wsdl")
                  
public class GreeterImpl implements Greeter {

    private static final Logger LOG = 
        Logger.getLogger(GreeterImpl.class.getPackage().getName());
    
    /* (non-Javadoc)
     * @see org.objectweb.hello_world_soap_http.Greeter#greetMe(java.lang.String)
     */
    public String greetMe(String me) {
        LOG.info("Executing operation greetMe");
        System.out.println("Executing operation greetMe");
        System.out.println("Message received: " + me + "\n");
        return "Hello " + me;
    }
}
