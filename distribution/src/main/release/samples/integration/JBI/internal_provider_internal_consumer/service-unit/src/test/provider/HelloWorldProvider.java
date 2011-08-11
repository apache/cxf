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


package test.provider;

import org.apache.hello_world.Greeter;
import org.apache.hello_world.PingMeFault;
import org.apache.hello_world.types.FaultDetail;



@javax.jws.WebService(portName = "SoapPort", serviceName = "HelloWorldService",
                      targetNamespace = "http://apache.org/hello_world",
                      endpointInterface = "org.apache.hello_world.Greeter",
                      wsdlLocation = "./META-INF/hello_world.wsdl")

                  
public class HelloWorldProvider implements Greeter {


    public String greetMe(String me) {
        System.out.println("Executing operation greetMe");
        System.out.println("Message received: " + me);
        return "Hello " + me;
    }

    public void greetMeOneWay(String me) {
        System.out.println("Executing operation greetMe");
        System.out.println("Message received: " + me);
    }
    
    public String sayHi() {
        System.out.println("Executing operation sayHi");
        return "Bonjour";
    }

    public void pingMe() throws PingMeFault {
        FaultDetail faultDetail = new FaultDetail();
        faultDetail.setMajor((short)2);
        faultDetail.setMinor((short)1);
        System.out.println("Executing operation pingMe, throwing PingMeFault exception\n");
        throw new PingMeFault("PingMeFault raised by server", faultDetail);
    }

}
