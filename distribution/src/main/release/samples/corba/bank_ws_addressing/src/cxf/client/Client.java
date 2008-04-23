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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.ws.wsaddressing.W3CEndpointReference;

import cxf.common.Account;
import cxf.common.AccountCORBAService;
import cxf.common.Bank;
import cxf.common.BankCORBAService;

public final class Client {

    private static final Logger LOG =
        Logger.getLogger(Client.class.getPackage().getName());

    
    private Client() {
    }

    public static void main(String args[]) throws Exception {
        
        LOG.log(Level.INFO, "Resolving the bank object");
        BankCORBAService service = new BankCORBAService();
        Bank port = service.getBankCORBAPort();

        // Test the method Bank.createAccount()
        System.out.println("Creating account called \"Account1\"");
        W3CEndpointReference epr1 = port.createAccount("Account1");
        Account account1 = getAccountFromEPR(epr1);
        System.out.println("Depositing 100.00 into account \'Account1\"");
        account1.deposit(100.00f);
        System.out.println("Current balance of account \"Account1\" is " + account1.getBalance());
        System.out.println();

        /* Re-enable when we have a utility to manipulate the meta data stored 
           within the EPR. 
        // Test the method Bank.createEprAccount()
        System.out.println("Creating account called \"Account2\"");
        W3CEndpointReference epr2 = port.createEprAccount("Account2");
        Account account2 = getAccountFromEPR(epr2);
        System.out.println("Depositing 5.00 into account \'Account2\"");
        account2.deposit(5.00f);
        System.out.println("Current balance of account \"Account2\" is " + account2.getBalance());
        System.out.println();
        */

        // create two more accounts to use with the getAccount calls
        Account acc3 = getAccountFromEPR(port.createAccount("Account3"));
        acc3.deposit(200.00f);
        Account acc4 = getAccountFromEPR(port.createAccount("Account4"));
        acc4.deposit(400.00f);
        
        // Test the method Bank.getAccount()
        System.out.println("Retrieving account called \"Account3\"");
        W3CEndpointReference epr3 = port.getAccount("Account3");
        Account account3 = getAccountFromEPR(epr3);
        System.out.println("Current balance for \"Account3\" is " + account3.getBalance());
        System.out.println("Depositing 10.00 into account \"Account3\"");
        account3.deposit(10.00f);
        System.out.println("New balance for account \"Account3\" is " + account3.getBalance());
        System.out.println();

        /* Re-enable when we have a utility to manipulate the meta data stored 
           within the EPR. 
        // Test the method Bank.getEprAccount()
        System.out.println("Retrieving account called \"Account4\"");
        EndpointReferenceType epr4 = port.getEprAccount("Account4");
        Account account4 = getAccountFromEPR(epr4);
        System.out.println("Current balance for account \"Account4\" is " + account4.getBalance());
        System.out.println("Withdrawing 150.00 into account \"Account4\"");
        account4.deposit(-150.00f);
        System.out.println("New balance for account \"Account4\" is " + account4.getBalance());
        System.out.println();
        */

        port.removeAccount("Account1");
        port.removeAccount("Account3");
        port.removeAccount("Account4");

        System.exit(0);
    }

    private static Account getAccountFromEPR(W3CEndpointReference epr) {
        AccountCORBAService service = new AccountCORBAService();
        return service.getPort(epr, Account.class);
    }
}
