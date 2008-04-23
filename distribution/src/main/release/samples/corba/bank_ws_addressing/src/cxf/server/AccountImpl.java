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

package cxf.server;

import cxf.common.Account;

@javax.jws.WebService(portName = "AccountCORBAPort",
                      serviceName = "AccountCORBAService",
                      targetNamespace = "http://cxf.apache.org/schemas/cxf/idl/Bank",
                      wsdlLocation = "file:./BankWS-corba.wsdl",
                      endpointInterface = "cxf.common.Account")

public class AccountImpl implements Account {
    
    private float balance;
    
    public float getBalance() {
        System.out.println("[Account] Called AccountImpl.getBalance()...");
        System.out.println();

        return balance;
    }

    public void deposit(float addition) {
        System.out.println("[Account] Called AccountImpl.deposit( " + addition + " )...");
        System.out.println();

        balance += addition;
    }
}
