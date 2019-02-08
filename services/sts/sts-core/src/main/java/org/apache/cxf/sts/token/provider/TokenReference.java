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

package org.apache.cxf.sts.token.provider;

/**
 * A class that encapsulates how a token should be referenced
 */
public class TokenReference {

    private String identifier;
    private String wsse11TokenType;
    private String wsseValueType;
    private boolean useDirectReference;
    private boolean useKeyIdentifier;

    /**
     * Get the identifier associated with this token
     * @return the identifier associated with this token
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Set the identifier associated with this token
     * @param identifier the identifier associated with this token
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Get the wsse11 TokenType attribute
     * @return the wsse11 TokenType attribute
     */
    public String getWsse11TokenType() {
        return wsse11TokenType;
    }

    /**
     * Set the wsse11 TokenType attribute
     * @param wsse11TokenType the wsse11 TokenType attribute
     */
    public void setWsse11TokenType(String wsse11TokenType) {
        this.wsse11TokenType = wsse11TokenType;
    }

    /**
     * Get the wsse ValueType attribute
     * @return the wsse ValueType attribute
     */
    public String getWsseValueType() {
        return wsseValueType;
    }

    /**
     * Set the wsse ValueType attribute
     * @param wsseValueType the wsse ValueType attribute
     */
    public void setWsseValueType(String wsseValueType) {
        this.wsseValueType = wsseValueType;
    }

    /**
     * Get whether to use direct reference to refer to this token
     * @return whether to use direct reference to refer to this token
     */
    public boolean isUseDirectReference() {
        return useDirectReference;
    }

    /**
     * Set whether to use direct reference to refer to this token
     * @param useDirectReference whether to use direct reference to refer to this token
     */
    public void setUseDirectReference(boolean useDirectReference) {
        this.useDirectReference = useDirectReference;
    }

    /**
     * Get whether to use a KeyIdentifier to refer to this token
     * @return whether to use a KeyIdentifier to refer to this token
     */
    public boolean isUseKeyIdentifier() {
        return useKeyIdentifier;
    }

    /**
     * Set whether to use a KeyIdentifier to refer to this token
     * @param useKeyIdentifier whether to use a KeyIdentifier to refer to this token
     */
    public void setUseKeyIdentifier(boolean useKeyIdentifier) {
        this.useKeyIdentifier = useKeyIdentifier;
    }


}
