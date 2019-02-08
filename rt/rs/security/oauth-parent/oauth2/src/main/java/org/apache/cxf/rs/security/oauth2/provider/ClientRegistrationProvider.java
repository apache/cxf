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
package org.apache.cxf.rs.security.oauth2.provider;

import java.util.List;

import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;

public interface ClientRegistrationProvider {
    /**
     * Get a Client with the given id
     * @param clientId the client id
     * @return Client
     */
    Client getClient(String clientId);
    /**
     * Set a Client
     * @param client the client
     */
    void setClient(Client client);
    /**
     * Remove a Client with the given id
     * @param clientId the client id
     * @return Client
     */
    Client removeClient(String clientId);

    /**
     * Get a list of clients registered by a resource owner.
     *
     * @param resourceOwner the resource owner, can be null
     * @return the list of clients
     */
    List<Client> getClients(UserSubject resourceOwner);
}
