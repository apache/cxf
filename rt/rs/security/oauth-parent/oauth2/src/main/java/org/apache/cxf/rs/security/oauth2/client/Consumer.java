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
package org.apache.cxf.rs.security.oauth2.client;

public class Consumer {

    private String clientId;
    private String clientSecret;

    public Consumer() {

    }

    public Consumer(String id) {
        this.clientId = id;
    }
    
    public Consumer(String id, String secret) {
        this.clientId = id;
        this.clientSecret = secret;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String id) {
        this.clientId = id;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String secret) {
        this.clientSecret = secret;
    }

    @Override
    public int hashCode() {
        return clientId.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Consumer && clientId.equals(((Consumer)o).clientId);
    }

}
