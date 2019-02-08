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
package org.apache.cxf.systest.jaxrs.security.oidc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.provider.ClientRegistrationProvider;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;

public class MemoryClientDataProvider implements ClientRegistrationProvider {

    private Map<String, Client> clients = new HashMap<>();
    @Override
    public Client getClient(String clientId) throws OAuthServiceException {
        return clients.get(clientId);
    }

    @Override
    public void setClient(Client client) {
        clients.put(client.getClientId(), client);
    }

    @Override
    public List<Client> getClients(UserSubject resourceOwner) {
        return new ArrayList<>(clients.values());
    }


    @Override
    public Client removeClient(String clientId) {
        return clients.remove(clientId);
    }

}
