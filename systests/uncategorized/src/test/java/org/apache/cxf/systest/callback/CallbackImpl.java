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

package org.apache.cxf.systest.callback;

import org.apache.callback.CallbackPortType;


@javax.jws.WebService(serviceName = "CallbackService", 
                      portName = "CallbackPort",
                      endpointInterface = "org.apache.callback.CallbackPortType",
                      targetNamespace = "http://apache.org/callback", 
                      wsdlLocation = "testutils/basic_callback_test.wsdl")
                  
public class CallbackImpl implements CallbackPortType  {

    //private static final Logger LOG = 
    //    Logger.getLogger(CallbackImpl.class.getPackage().getName());
    
    /**
     * serverSayHi
     * @param: return_message (String)
     * @return: String
     */
    public String serverSayHi(String message) {
        return new String("Hi " + message);
    }
    
}
