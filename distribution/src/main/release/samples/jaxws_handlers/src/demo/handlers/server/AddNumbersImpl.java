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

package demo.handlers.server;

import javax.jws.HandlerChain;
import javax.jws.WebService;
import org.apache.handlers.AddNumbers;
import org.apache.handlers.AddNumbersFault;
import org.apache.handlers.types.FaultDetail;


@WebService(name = "AddNumbers",
            targetNamespace = "http://apache.org/handlers",
            portName = "AddNumbersPort",
            endpointInterface = "org.apache.handlers.AddNumbers",
            serviceName = "AddNumbersService")
@HandlerChain(file = "../common/demo_handlers.xml")
public class AddNumbersImpl implements AddNumbers {


    /**
     * @param number1
     * @param number2
     * @return The sum
     * @throws AddNumbersException
     *             if any of the numbers to be added is negative.
     */
    public int addNumbers(int number1, int number2) throws AddNumbersFault {
        System.out.println("addNumbers called....." + number1 + ":" + number2);
        if (number1 < 0 || number2 < 0) {
            String message = "Negative number cant be added!";
            String detail = "Numbers: " + number1 + ", " + number2;
            FaultDetail fault = new FaultDetail();
            fault.setMessage(message);
            fault.setFaultInfo(detail);
            throw new AddNumbersFault(message, fault);
        }
        return number1 + number2;
    }

}
