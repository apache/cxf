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

package org.apache.cxf.tools.fortest.addr;

import javax.jws.WebService;
import javax.xml.ws.Action;
import javax.xml.ws.BindingType;
import javax.xml.ws.Holder;
import javax.xml.ws.soap.Addressing;
import javax.xml.ws.soap.SOAPBinding;

@WebService(
    name = "AddressingFeatureTest2",
    portName = "AddressingFeatureTest2Port",
    targetNamespace = "http://addressingfeatureservice.org/wsdl",
    serviceName = "AddressingFeatureTest2Service"
)

@BindingType(value = SOAPBinding.SOAP11HTTP_BINDING)
@Addressing(enabled = true, required = true)

public class WSAImpl2 {
    @Action(input = "inputAction", output = "outputAction")
    public int addNumbers(Holder<String> testname, int number1, int number2) {
        if (number1 < 0 || number2 < 0) {
            new AddressingFeatureException("One of the numbers received was negative:" 
                                           + number1 + ", " + number2);
        }
        return number1 + number2;
    }
}
