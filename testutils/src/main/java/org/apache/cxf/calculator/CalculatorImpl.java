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

package org.apache.cxf.calculator;

import javax.jws.WebService;

import org.apache.cxf.calculator.types.CalculatorFault;

@WebService(serviceName = "CalculatorService", 
            portName = "CalculatorPort", 
            targetNamespace = "http://apache.org/cxf/calculator", 
            endpointInterface = "org.apache.cxf.calculator.CalculatorPortType",
            wsdlLocation = "testutils/calculator.wsdl")
public class CalculatorImpl implements CalculatorPortType {
    public int add(int number1, int number2) throws AddNumbersFault {
        if (number1 < 0 || number2 < 0) {
            CalculatorFault fault = new CalculatorFault();
            fault.setMessage("Negative number cant be added!");
            fault.setFaultInfo("Numbers: " + number1 + ", " + number2);
            throw new AddNumbersFault("Negative number cant be added!", fault);
        }
        return number1 + number2;
    }

}
