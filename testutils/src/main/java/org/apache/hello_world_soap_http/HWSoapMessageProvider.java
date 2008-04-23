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

package org.apache.hello_world_soap_http;


//import java.util.logging.Logger;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceProvider;

@WebServiceProvider(portName = "SoapPort", serviceName = "SOAPService",
                      targetNamespace = "http://apache.org/hello_world_soap_http",
                      wsdlLocation = "wsdl/hello_world.wsdl")
@ServiceMode(value = Service.Mode.MESSAGE)                      
public class HWSoapMessageProvider implements Provider<SOAPMessage> {

    //private static final Logger LOG =
    //    Logger.getLogger(AnnotatedGreeterImpl.class.getName());

    private int invokeCount;
    
    public HWSoapMessageProvider() {
        //Complete
    }

    public SOAPMessage invoke(SOAPMessage source) {
        invokeCount++;
        return source;
    }
    
    public int getInvokeCount() {
        return invokeCount;
    }
}
