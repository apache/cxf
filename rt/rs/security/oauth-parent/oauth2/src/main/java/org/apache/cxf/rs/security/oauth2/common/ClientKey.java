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
package org.apache.cxf.rs.security.oauth2.common;

import java.io.Serializable;

public class ClientKey implements Serializable {
    private static final long serialVersionUID = 6151645789585333184L;
    public enum Type {
        PASSWORD,
        X509CERTIFICATE
    }
    
    private String key;
    private Type type;
    
    public ClientKey() {
        
    }
    
    public ClientKey(String password) {
        this(password, Type.PASSWORD);
    }
    
    public ClientKey(Type type) {
        this(null, type);
    }
    
    public ClientKey(String key, Type type) {
        this.key = key;
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
    
    public int hashCode() {
        return (key == null ? 37 : key.hashCode()) * type.hashCode(); 
    }
    public boolean equals(Object obj) {
        if (obj instanceof ClientKey) {
            ClientKey other = (ClientKey)obj;
            if (this.key == null && other.key != null
                || this.key != null && other.key == null) {
                return false;
            }
            return this.key.equals(other.key) && this.type.equals(other.type);
        } else {
            return false;
        }
        
    }
}
