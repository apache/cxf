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

import java.util.HashSet;
import java.util.Set;

public class Consumer {

    private String key;
    private String secret;
    private Set<String> redirectURIs;
    private String name;
    private String description;

    public Consumer() {

    }

    public Consumer(String key, String secret) {
        this.setKey(key);
        this.setSecret(secret);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Consumer && key.equals(((Consumer)o).key);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getRedirectURIs() {
        return redirectURIs;
    }

    public void setRedirectURIs(Set<String> redirectUri) {
        this.redirectURIs = redirectUri;
    }

    public boolean addRedirectURI(String redirectURI) {
        if (this.redirectURIs == null) {
            this.redirectURIs = new HashSet<String>();
        }
        return this.redirectURIs.add(redirectURI);
    }
}
