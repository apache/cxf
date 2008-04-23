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
package cxf.client;

import java.net.URL;
import javax.xml.namespace.QName;

import bank.common.Account;
import bank.common.AccountAlreadyExistsException;
import bank.common.AccountNotFoundException;
import bank.common.Bank;
import bank.common.BankCORBAService;

public final class Client {

    private static final QName SERVICE_NAME 
        = new QName("http://cxf.apache.org/schemas/cxf/idl/bank", "BankCORBAService");

    private Client() {
    }

    public static void main(String args[]) throws Exception {
        URL wsdlUrl = new URL("file:./../resources/bank.wsdl");
    
        BankCORBAService ss = new BankCORBAService(wsdlUrl, SERVICE_NAME);
        Bank port = ss.getBankCORBAPort();  
        

        System.out.print("Invoking createAccount for Mr. John... ");
        javax.xml.ws.Holder<Account> account = new javax.xml.ws.Holder<Account>(new Account());
        try {
            if (port.createAccount("John", account)) {
                System.out.println("success");
            } else {
                System.out.println("failure (Unknown)");
            }
        } catch (AccountAlreadyExistsException ex) {
            System.out.println("failure (" + ex.getMessage() + " : " + ex.getFaultInfo().getName() + ")");
        }

        Account bankAccount = account.value;
        if (bankAccount != null) {
            System.out.println("Created Account : " 
                               + bankAccount.getName() + ": " + bankAccount.getBalance());
        }

        System.out.println("Getting Mr. John's account...");
        try {
            bankAccount = port.getAccount("John");
            if (bankAccount != null) {
                System.out.println("success");
                System.out.println(bankAccount.getName() + ": " + bankAccount.getBalance());
            } else {
                System.out.println("failure");
            }
        } catch (AccountNotFoundException ex) {
            System.out.println("failure (" + ex.getMessage() + " : " + ex.getFaultInfo().getName() + ")");
        }       

        System.out.println("Getting an non-existent account (Ms. Helen)...");
        try {
            bankAccount = port.getAccount("Helen");
        } catch (AccountNotFoundException ex) {
            System.out.println("Caught the expected AccountNotFoundException(" 
                               + ex.getFaultInfo().getName() + ")");
        }
                
        System.exit(0);
    }

}
