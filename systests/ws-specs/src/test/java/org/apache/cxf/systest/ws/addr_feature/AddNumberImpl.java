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

package org.apache.cxf.systest.ws.addr_feature;

import javax.jws.WebService;
import javax.xml.ws.soap.Addressing;

// Jax-WS 2.1 WS-Addressing FromJava

@Addressing
@WebService
public class AddNumberImpl implements AddNumbersPortType {
    public int addNumbers(int number1, int number2) throws AddNumbersFault_Exception {
        return execute(number1, number2);
    }

    public int addNumbers2(int number1, int number2) {
        return number1 + number2;
    }

    public int addNumbers3(int number1, int number2) throws AddNumbersFault_Exception {
        return execute(number1, number2);
    }


    int execute(int number1, int number2) throws AddNumbersFault_Exception {
        if (number1 < 0 || number2 < 0) {
            AddNumbersFault fb = new AddNumbersFault();
            fb.setDetail("Negative numbers cant be added!");
            fb.setMessage("Numbers: " + number1 + ", " + number2);

            throw new AddNumbersFault_Exception(fb.getMessage(), fb);
        }

        return number1 + number2;
    }
}
