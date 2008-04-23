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

import java.util.HashMap;
import java.util.Map;

import bank.common.Account;
import bank.common.AccountAlreadyExistsException;
import bank.common.AccountAlreadyExistsExceptionType;
import bank.common.AccountNotFoundException;
import bank.common.AccountNotFoundExceptionType;
import bank.common.Bank;

@javax.jws.WebService(portName = "BankCORBAPort", serviceName = "BankCORBAService", 
                      targetNamespace = "http://cxf.apache.org/schemas/cxf/idl/bank", 
                      wsdlLocation = "file:../resources/bank.wsdl",
                      endpointInterface = "bank.common.Bank")

public class BankImpl implements Bank {

    Map<String, Account> accounts = new HashMap<String, Account>();

    public boolean createAccount(String name, javax.xml.ws.Holder<Account> account)
        throws AccountAlreadyExistsException { 

        System.out.println("Creating account: " + name);
        boolean result = false;
        if (accounts.get(name) == null) {
            account.value = new Account();
            account.value.setName(name);
            account.value.setBalance(100);
            accounts.put(name, account.value);
            result = true;
        } else {
            AccountAlreadyExistsExceptionType ex = new AccountAlreadyExistsExceptionType();
            ex.setName(name);
            throw new AccountAlreadyExistsException("Account Already Exists", ex);
        }
        return result;
    }

    public Account getAccount(String name) throws AccountNotFoundException {
        System.out.println("Getting account: " + name);
        Account result = accounts.get(name);
        if (result == null) {
            AccountNotFoundExceptionType ex = new AccountNotFoundExceptionType();
            ex.setName(name);
            throw new AccountNotFoundException("Account Not Found", ex);
        }
        return result;
    }
}
