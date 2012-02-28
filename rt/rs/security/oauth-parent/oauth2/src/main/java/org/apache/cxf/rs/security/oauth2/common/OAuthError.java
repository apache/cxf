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


public class OAuthError {
    
    private String error;
    private String errorDescription;
    private String errorUri;
    private String state;
    
    public OAuthError() {
        
    }
    
    public OAuthError(String error) {
        this.error = error;
    }
    
    public OAuthError(String error, String descr) {
        this.error = error;
        this.errorDescription = descr;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    public String getError() {
        return error;
    }
    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }
    public String getErrorDescription() {
        return errorDescription;
    }
    public void setErrorUri(String errorUri) {
        this.errorUri = errorUri;
    }
    public String getErrorUri() {
        return errorUri;
    }
    public void setState(String state) {
        this.state = state;
    }
    public String getState() {
        return state;
    }
}
