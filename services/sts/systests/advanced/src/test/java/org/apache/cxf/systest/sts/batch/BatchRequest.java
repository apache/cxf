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
package org.apache.cxf.systest.sts.batch;

import java.util.List;

public class BatchRequest {

    List<String> tokenTypes;
    List<String> keyTypes;
    String requestType;
    String action;
    List<String> appliesTo;
    
    public List<String> getTokenTypes() {
        return tokenTypes;
    }
    public void setTokenTypes(List<String> tokenTypes) {
        this.tokenTypes = tokenTypes;
    }
    public List<String> getKeyTypes() {
        return keyTypes;
    }
    public void setKeyTypes(List<String> keyTypes) {
        this.keyTypes = keyTypes;
    }
    public String getRequestType() {
        return requestType;
    }
    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }
    public String getAction() {
        return action;
    }
    public void setAction(String action) {
        this.action = action;
    }
    public List<String> getAppliesTo() {
        return appliesTo;
    }
    public void setAppliesTo(List<String> appliesTo) {
        this.appliesTo = appliesTo;
    }
    
    
}
