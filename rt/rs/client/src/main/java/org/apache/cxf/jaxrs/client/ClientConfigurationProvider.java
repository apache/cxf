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

package org.apache.cxf.jaxrs.client;

import jakarta.ws.rs.core.Configuration;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Message;

/**
 * Dedicated interface for client side {@link Configuration} provider.
 */
public interface ClientConfigurationProvider {
    String CLIENT_CONFIGURATION_PPROVIDER_NAME = "org.apache.cxf.jaxrs.client.ClientConfigurationProvider";

    /**
     * Gets the {@link ClientConfigurationProvider} instance from the message.
     * @param message message
     * @return {@link ClientConfigurationProvider} instance or null if not available
     */
    static ClientConfigurationProvider getInstance(Message message) {
        return getInstance(message.getExchange().getEndpoint());
    }

    /**
     * Gets the {@link ClientConfigurationProvider} instance from the endpoint.
     * @param endpoint endpoint
     * @return {@link ClientConfigurationProvider} instance or null if not available
     */
    static ClientConfigurationProvider getInstance(Endpoint endpoint) {
        return (ClientConfigurationProvider) endpoint.get(CLIENT_CONFIGURATION_PPROVIDER_NAME);
    }

    /**
     * Gets the {@link Configuration} instance from the message.
     * @param message message
     * @return {@link Configuration} instance or null if not available
     */
    Configuration getConfiguration(Message message);
}
