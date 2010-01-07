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

package org.apache.cxf.systest.ws.addr_fromwsdl;

import java.util.concurrent.Future;

import javax.jws.WebService;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Response;

import org.apache.cxf.systest.ws.addr_feature.AddNumbersFault;
import org.apache.cxf.systest.ws.addr_feature.AddNumbersFault_Exception;
import org.apache.cxf.systest.ws.addr_feature.AddNumbersPortType;
import org.apache.cxf.systest.ws.addr_feature.AddNumbersResponse;

// Jax-WS 2.1 WS-Addressing FromWsdl

@WebService(serviceName = "AddNumbersService",
            targetNamespace = "http://apache.org/cxf/systest/ws/addr_feature/")
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

    public Response<AddNumbersResponse> addNumbers2Async(int number1, int number2) {
        return null;
    }

    public Future<?> addNumbers2Async(int number1, int number2, 
                                      AsyncHandler<AddNumbersResponse> asyncHandler) {
        return null;
    }

    public Response<AddNumbersResponse> addNumbers3Async(int number1, int number2) {
        return null;
    }

    public Future<?> addNumbers3Async(int number1, int number2, 
                                      AsyncHandler<AddNumbersResponse> asyncHandler) {
        return null;
    }

    public Response<AddNumbersResponse> addNumbersAsync(int number1, int number2) {
        return null;
    }

    public Future<?> addNumbersAsync(int number1, int number2,
                                     AsyncHandler<AddNumbersResponse> asyncHandler) {
        return null;
    }
}
