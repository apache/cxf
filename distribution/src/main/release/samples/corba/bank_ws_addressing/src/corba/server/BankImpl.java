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

package corba.server;

import java.util.HashMap;
import java.util.Map;

import corba.common.Account;
import corba.common.AccountHelper;
import corba.common.BankPOA;

import org.omg.PortableServer.POA;

public class BankImpl extends BankPOA {
    
    private POA poa_;

    private Map<String, Account> accountList;
    
    BankImpl(POA poa) {
        poa_ = poa;

        accountList = new HashMap<String, Account>();
    }

    public Account create_account(String accountName) {
        if (accountList.containsKey(accountName)) {
            return accountList.get(accountName);
        }
        
        System.out.println("[Bank] Called create_account( " + accountName + " )...");
        System.out.println();        
        AccountImpl accountImpl = new AccountImpl(poa_);
        String name = "Account-" + accountName;
        byte[] oid = name.getBytes();       
        try {
            poa_.activate_object_with_id(oid, accountImpl);
        } catch (Exception ex) {
            //Ignore
        }

        org.omg.CORBA.Object obj = poa_.create_reference_with_id(oid, AccountHelper.id());
        Account account = AccountHelper.narrow(obj);

        accountList.put(accountName, account);
        return account;
    }

    public org.omg.CORBA.Object create_epr_account(String accountName) {
        System.out.println("[Bank] Called create_epr_account( " + accountName + ")...");
        System.out.println();
        /*Account_impl accountImpl = new Account_impl(poa_);
        Account account = accountImpl._this(orb_);
        accountList.put(account_name, account);*/

        AccountImpl accountImpl = new AccountImpl(poa_);
        String name = "Account-" + accountName;
        byte[] oid = name.getBytes();       
        try {
            poa_.activate_object_with_id(oid, accountImpl);
        } catch (Exception ex) {
            //Ignore
        }

        org.omg.CORBA.Object obj = poa_.create_reference_with_id(oid, AccountHelper.id());
        Account account = AccountHelper.narrow(obj);

        accountList.put(accountName, account);
        return account;
    }

    public Account get_account(String accountName) {
        System.out.println("[Bank] Called get_account( " + accountName + ")...");
        System.out.println();
        return accountList.get(accountName);
    }

    public org.omg.CORBA.Object get_epr_account(String accountName) {
        System.out.println("[Bank] Called get_epr_account( " + accountName + ")...");
        System.out.println();
        return accountList.get(accountName);
    }

    // TODO: What is the correct implementation for this operation?
    public org.omg.CORBA.Object get_account_epr_with_no_use_attribute(String accountName) {
        System.out.println("[Bank] Called get_epr_with_no_use_attribute( " + accountName + " )...");
        System.out.println();
        return null;
    }

    // TODO: What is the correct implementation for this operation?
    public void find_account(org.omg.CORBA.AnyHolder accountDetails) {
        System.out.println("[Bank] Called find_account (account_details)...");
        System.out.println();
    }

    public void remove_account(String accountName) {
        System.out.println("[Bank] Called remove_account ( " + accountName + ")...");
        System.out.println();
        accountList.remove(accountName);
    }
    
    public POA _default_POA() {
        return poa_;
    }
}

