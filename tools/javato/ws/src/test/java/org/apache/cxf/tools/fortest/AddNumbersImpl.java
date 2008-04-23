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

package org.apache.cxf.tools.fortest;

import javax.jws.WebService;
import javax.xml.ws.Action;
import javax.xml.ws.FaultAction;
import javax.xml.ws.soap.Addressing;

// Jax-WS 2.1 WS-Addressing FromJava

@Addressing
@WebService
public class AddNumbersImpl {
    @Action(
            input = "http://cxf.apache.org/input",
            output = "http://cxf.apache.org/output")
    public int addNumbers(int number1, int number2) throws AddNumbersException {
        return execute(number1, number2);
    }

    public int addNumbers2(int number1, int number2) {
        return number1 + number2;
    }

    @Action(input = "http://cxf.apache.org/input3", output = "http://cxf.apache.org/output3",
            fault = {@FaultAction(className = AddNumbersException.class, 
                                  value = "http://cxf.apache.org/fault3") })
    public int addNumbers3(int number1, int number2) throws AddNumbersException {
        return execute(number1, number2);
    }

    int execute(int number1, int number2) throws AddNumbersException {
        if (number1 < 0 || number2 < 0) {
            throw new AddNumbersException("Negative numbers can't be added!",
                                          "Numbers: " + number1 + ", " + number2);
        }
        return number1 + number2;
    }
}
