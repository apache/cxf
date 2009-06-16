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

package corba.client;

import java.util.Properties;

import org.omg.CORBA.ORB;
import org.omg.CORBA.UserException;


import corba.common.Account;
import corba.common.AccountHelper;
import corba.common.Bank;
import corba.common.BankHelper;


public final class Client {
    private Client() {
        //not constructed
    }
    static int run(ORB orb, String[] args) throws UserException {
        
        // Get the Bank object
        org.omg.CORBA.Object obj = 
            orb.string_to_object("corbaname::localhost:1050#Bank");
        if (obj == null) {
            System.err.println("bank.Client: cannot read IOR from corbaname::localhost:1050#Bank");
            return 1;
        }
        Bank bank = BankHelper.narrow(obj);


        // Test the method Bank.create_account()
        System.out.println("Creating account called \"Account1\"");
        Account account1 = bank.create_account("Account1");
        System.out.println("Depositing 100.00 into account \"Account1\"");
        account1.deposit(100.00f);
        System.out.println("Current balance of \"Account1\" is " + account1.get_balance());
        System.out.println();

        // Test the method Bank.create_epr_account()
        System.out.println("Creating account called \"Account2\"");
        org.omg.CORBA.Object account2Obj = bank.create_epr_account("Account2");
        Account account2 = AccountHelper.narrow(account2Obj);
        System.out.println("Depositing 5.00 into account \"Account2\"");
        account2.deposit(5.00f);
        System.out.println("Current balance of \"Account2\" is " + account2.get_balance());
        System.out.println();
        
        // Create two more accounts to use with the getAccount calls
        Account acc3 = bank.create_account("Account3");
        acc3.deposit(200.00f);
        Account acc4 = bank.create_account("Account4");
        acc4.deposit(400.00f);

        // Test the method Bank.get_account()
        System.out.println("Retrieving account called \"Account3\"");
        Account account3 = bank.get_account("Account3");
        System.out.println("Current balance for \"Account3\" is " + account3.get_balance());
        System.out.println("Depositing 10.00 into account \"Account3\"");
        account3.deposit(10.00f);
        System.out.println("New balance for account \"Account3\" is " + account3.get_balance());
        System.out.println();

        // Test the method Bank.get_epr_account()
        System.out.println("Retrieving account called \"Account4\"");
        Account account4 = bank.get_account("Account4");
        System.out.println("Current balance for \"Account4\" is " + account4.get_balance());
        System.out.println("Withdrawing 150.00 into account \"Account4\"");
        account4.deposit(-150.00f);
        System.out.println("New balance for account \"Account4\" is " + account4.get_balance());
        System.out.println();

        bank.remove_account("Account1");
        bank.remove_account("Account2");
        bank.remove_account("Account3");
        bank.remove_account("Account4");

        return 0;
    }

    public static void main(String args[]) {
        int status = 0;
        ORB orb = null;

        Properties props = System.getProperties();

        try {
            orb = ORB.init(args, props);
            status = run(orb, args);
        } catch (Exception ex) {
            ex.printStackTrace();
            status = 1;
        }

        if (orb != null) {
            try {
                orb.destroy();
            } catch (Exception ex) {
                ex.printStackTrace();
                status = 1;
            }
        }

        System.exit(status);
    }
}
