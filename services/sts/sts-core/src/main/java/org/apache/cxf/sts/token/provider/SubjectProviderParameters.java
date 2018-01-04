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

import java.util.List;

import org.w3c.dom.Document;

import org.apache.wss4j.common.saml.bean.AttributeStatementBean;
import org.apache.wss4j.common.saml.bean.AuthDecisionStatementBean;
import org.apache.wss4j.common.saml.bean.AuthenticationStatementBean;

/**
 * The parameters that are passed through to a SubjectProvider implementation to create a Subject(Bean).
 */
public class SubjectProviderParameters {

    private TokenProviderParameters providerParameters;
    private Document doc;
    private byte[] secret;
    private List<AttributeStatementBean> attrBeanList;
    private List<AuthenticationStatementBean> authBeanList;
    private List<AuthDecisionStatementBean> authDecisionBeanList;

    public TokenProviderParameters getProviderParameters() {
        return providerParameters;
    }

    public void setProviderParameters(TokenProviderParameters providerParameters) {
        this.providerParameters = providerParameters;
    }

    public Document getDoc() {
        return doc;
    }

    public void setDoc(Document doc) {
        this.doc = doc;
    }

    public byte[] getSecret() {
        return secret;
    }

    public void setSecret(byte[] secret) {
        this.secret = secret;
    }

    public List<AttributeStatementBean> getAttrBeanList() {
        return attrBeanList;
    }

    public void setAttrBeanList(List<AttributeStatementBean> attrBeanList) {
        this.attrBeanList = attrBeanList;
    }

    public List<AuthenticationStatementBean> getAuthBeanList() {
        return authBeanList;
    }

    public void setAuthBeanList(List<AuthenticationStatementBean> authBeanList) {
        this.authBeanList = authBeanList;
    }

    public List<AuthDecisionStatementBean> getAuthDecisionBeanList() {
        return authDecisionBeanList;
    }

    public void setAuthDecisionBeanList(List<AuthDecisionStatementBean> authDecisionBeanList) {
        this.authDecisionBeanList = authDecisionBeanList;
    }

}
