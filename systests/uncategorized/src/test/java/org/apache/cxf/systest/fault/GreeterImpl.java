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
package org.apache.cxf.systest.fault;

import org.apache.intfault.BadRecordLitFault;
import org.apache.intfault.types.BareDocumentResponse;
@javax.jws.WebService(portName = "SoapPort", serviceName = "SOAPService", 
                      targetNamespace = "http://apache.org/intfault", 
                      endpointInterface = "org.apache.intfault.Greeter",
                      wsdlLocation = "testutils/hello_world_fault.wsdl")
public class GreeterImpl {
    public BareDocumentResponse testDocLitFault(String in) throws BadRecordLitFault {
        System.out.println("Executing testDocLitFault sayHi\n");
        throw new BadRecordLitFault("int fault", 5);

    }

}
