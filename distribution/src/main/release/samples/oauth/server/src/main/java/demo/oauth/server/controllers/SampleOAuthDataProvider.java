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
package demo.oauth.server.controllers;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cxf.rs.security.oauth.data.Client;
import org.apache.cxf.rs.security.oauth.data.Token;

public class SampleOAuthDataProvider extends MemoryOAuthDataProvider
    implements OAuthClientManager {

    public Client registerNewClient(String consumerKey, Client client) {
        Client authNInfo = clientAuthInfo.putIfAbsent(consumerKey, client);
        if (authNInfo == null) {
            userRegisteredClients.add(consumerKey, consumerKey);
        }
        return authNInfo;
    }

    public Set<Client> listRegisteredClients() {
        Set<Client> apps = new HashSet<Client>();
        Set<String> appList = userRegisteredClients.keySet();
        if (appList != null) {
            for (String s : appList) {
                apps.add(clientAuthInfo.get(s));
            }
        }
        return apps;
    }

    public Set<Client> listAuthorizedClients() {
        Set<Client> apps = new HashSet<Client>();
        Set<String> appList = userAuthorizedClients.keySet();
        if (appList != null) {
            for (String s : appList) {
                apps.add(clientAuthInfo.get(s));
            }
        }
        return apps;
    }
 
    public synchronized void removeRegisteredClient(String consumerKey) {
        List<String> registeredApps = this.userRegisteredClients.get(consumerKey);
        this.clientAuthInfo.remove(consumerKey);

        //remove registered app
        registeredApps.remove(consumerKey);
        this.userRegisteredClients.put(consumerKey, registeredApps);

        //remove all authorized apps from other clients
        for (Map.Entry<String, List<String>> userAuthorizedClientsSet : userAuthorizedClients.entrySet()) {
            String principalName = userAuthorizedClientsSet.getKey();
            List<String> clients = userAuthorizedClientsSet.getValue();
            clients.remove(consumerKey);
            userAuthorizedClients.put(principalName, clients);
        }
        //remove access tokens
        for (Token token : oauthTokens.values()) {
            Client authNInfo = token.getClient();
            if (consumerKey.equals(authNInfo.getConsumerKey())) {
                oauthTokens.remove(token.getTokenKey());
            }
        }
    }   
}
