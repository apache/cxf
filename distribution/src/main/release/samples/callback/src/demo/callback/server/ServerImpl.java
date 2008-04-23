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

package demo.callback.server;


import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.wsaddressing.W3CEndpointReference;

import org.apache.callback.CallbackPortType;
import org.apache.callback.ServerPortType;


@javax.jws.WebService(serviceName = "SOAPService", 
                      portName = "SOAPPort",
                      targetNamespace = "http://apache.org/callback",
                      endpointInterface = "org.apache.callback.ServerPortType") 
                  
public class ServerImpl implements ServerPortType  {
    
    public String registerCallback(W3CEndpointReference callback) {
        
        try {

            WebServiceFeature[] wfs = new WebServiceFeature[] {};
            CallbackPortType port = (CallbackPortType)callback.getPort(CallbackPortType.class, wfs);

            System.out.println("Invoking on callback object");
            String resp = port.serverSayHi(System.getProperty("user.name"));
            System.out.println("Response from callback object: " + resp);
  

            
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        
        return "registerCallback called";     
    }
    
    
}
