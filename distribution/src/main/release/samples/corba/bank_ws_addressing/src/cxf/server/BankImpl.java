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

import javax.xml.ws.Endpoint;
import javax.xml.ws.wsaddressing.W3CEndpointReference;

import cxf.common.Bank;

@javax.jws.WebService(portName = "BankCORBAPort", 
                      serviceName = "BankCORBAService",
                      targetNamespace = "http://cxf.apache.org/schemas/cxf/idl/Bank",
                      wsdlLocation = "file:./BankWS-corba.wsdl",
                      endpointInterface = "cxf.common.Bank")

public class BankImpl implements Bank {

    private Map<String, W3CEndpointReference> accountList = 
        new HashMap<String, W3CEndpointReference>();
    private Map<String, Endpoint> endpointList = new HashMap<String, Endpoint>();

    public BankImpl() {
    }
    
    public W3CEndpointReference createAccount(String accountName) {
        System.out.println("[Bank] Called createAccount( " + accountName + " )...");
        System.out.println();
        W3CEndpointReference ref = null;
        ref = createAccountReference(accountName);
        if (ref != null) {
            accountList.put(accountName, ref);
        }
        return ref;
    }

    public W3CEndpointReference createEprAccount(String accountName) {
        System.out.println("[Bank] Called createEprAccount( " + accountName + " )...");
        System.out.println();
        W3CEndpointReference ref = createAccountReference(accountName);
        if (ref != null) {
            accountList.put(accountName, ref);
        }
        return ref;
    }

    public W3CEndpointReference getAccount(String accountName) {
        System.out.println("[Bank] Called getAccount( " + accountName + " )...");
        System.out.println();
        return accountList.get(accountName);
    }

    public W3CEndpointReference getEprAccount(String accountName) {
        System.out.println("[Bank] Called getEprAccount( " + accountName + " )...");
        System.out.println();
        return accountList.get(accountName);
    }

    // TODO: What is the correct implementation for this operation?
    public W3CEndpointReference getAccountEprWithNoUseAttribute(String accountName) {
        return null;
    }
    
    // TODO: What is the correct implementation for this operation?
    public Object findAccount(Object accountDetails) {
        return null;
    }

    public void removeAccount(String accountName) {
        System.out.println("[Bank] Called removeAccount( " + accountName + " )...");
        System.out.println();
        accountList.remove(accountName);
        Endpoint ep = endpointList.remove(accountName);
        ep.stop();
    }

    private W3CEndpointReference createAccountReference(String accountName) {
        String corbaAddress = "corbaname::localhost:1050#" + accountName;

        Object account = new AccountImpl();
        Endpoint ep = Endpoint.publish(corbaAddress, account);
        endpointList.put(accountName, ep);

        return (W3CEndpointReference)ep.getEndpointReference((org.w3c.dom.Element[])null);
    }
}

