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
package demo.hwRPCLit.server;

import java.util.logging.Logger;
import org.apache.hello_world_rpclit.GreeterRPCLit;
import org.apache.hello_world_rpclit.types.MyComplexStruct;

@javax.jws.WebService(portName = "SoapPortRPCLit", serviceName = "SOAPServiceRPCLit", 
                      targetNamespace = "http://apache.org/hello_world_rpclit", 
                      endpointInterface = "org.apache.hello_world_rpclit.GreeterRPCLit")
public class GreeterRPCLitImpl implements GreeterRPCLit {

    private static final Logger LOG = Logger.getLogger(GreeterRPCLitImpl.class.getPackage().getName());

    public String greetMe(String me) {
        LOG.info("Executing operation greetMe");
        System.out.println("Executing operation greetMe");
        System.out.println("Message received: " + me + "\n");
        return "Hello " + me;
    }

    public String sayHi() {
        LOG.info("Executing operation sayHi");
        System.out.println("Executing operation sayHi" + "\n");
        return "Bonjour";
    }

    public MyComplexStruct sendReceiveData(MyComplexStruct in) {
        LOG.info("Executing operation sendReceiveData");
        System.out.println("Executing operation sendReceiveData");
        System.out.println("Received struct with values :\nElement-1 : " + in.getElem1() + "\nElement-2 : "
                           + in.getElem2() + "\nElement-3 : " + in.getElem3() + "\n");
        return in;
    }
}
