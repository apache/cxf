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

import org.w3c.dom.Element;

public class BatchRequest {

    private String tokenType;
    private String keyType;
    private String appliesTo;
    private Element validateTarget;

    public String getTokenType() {
        return tokenType;
    }
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
    public String getKeyType() {
        return keyType;
    }
    public void setKeyType(String keyType) {
        this.keyType = keyType;
    }
    public String getAppliesTo() {
        return appliesTo;
    }
    public void setAppliesTo(String appliesTo) {
        this.appliesTo = appliesTo;
    }
    public Element getValidateTarget() {
        return validateTarget;
    }
    public void setValidateTarget(Element validateTarget) {
        this.validateTarget = validateTarget;
    }


}
