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

import java.util.logging.Logger;

import javax.jws.WebService;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.hello_world.Greeter;


@WebService(serviceName = "SOAPService", 
            endpointInterface = "org.apache.hello_world.Greeter", 
            targetNamespace = "http://apache.org/hello_world")
public class GreeterImpl implements Greeter {

    private static final Logger LOG = 
        LogUtils.getLogger(GreeterImpl.class,
                           null,
                           GreeterImpl.class.getPackage().getName());
    private String myName;
    
    public GreeterImpl() {
        this("defaultGreeter");
    }
    
    public GreeterImpl(String name) {
        myName = name;
    }

    public String greetMe(String me) {
        LOG.info("Executing operation greetMe");
        LOG.info("Message received: " + me);
        return "Hello " + me;
    }
    

    public String sayHi() {
        LOG.info("Executing operation sayHi");        
        return "Bonjour from " + myName;
    }
    
    public void pingMe() {
    }

    
}
