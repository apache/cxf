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
package sample.ws.service;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;

@WebService(name = "SecondServiceAT", targetNamespace = "http://service.ws.sample/")
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface SecondServiceAT {

    /**
     * Create a new booking
     */
    @WebMethod
    public void incrementCounter(int numSeats);

    /**
     * obtain the number of existing bookings
     *
     * @return the number of current bookings
     */
    @WebMethod
    public int getCounter();

    /**
     * Reset the booking count to zero
     */
    @WebMethod
    public void resetCounter();

}


