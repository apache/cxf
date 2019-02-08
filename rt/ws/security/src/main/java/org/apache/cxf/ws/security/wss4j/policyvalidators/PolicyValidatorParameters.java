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

package org.apache.cxf.ws.security.wss4j.policyvalidators;

import java.util.Collection;
import java.util.List;

import org.w3c.dom.Element;

import org.apache.cxf.message.Message;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.wss4j.dom.WSDataRef;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.WSHandlerResult;

/**
 * Holds various parameters to the policy validators
 */
public class PolicyValidatorParameters {
    private AssertionInfoMap assertionInfoMap;
    private Message message;
    private Element soapBody;
    private Element soapHeader;
    private WSHandlerResult results;
    private List<WSSecurityEngineResult> signedResults;
    private List<WSSecurityEngineResult> encryptedResults;
    private List<WSSecurityEngineResult> usernameTokenResults;
    private List<WSSecurityEngineResult> samlResults;
    private Element timestampElement;
    private boolean utWithCallbacks;
    private Collection<WSDataRef> signed;
    private Collection<WSDataRef> encrypted;

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public Element getSoapBody() {
        return soapBody;
    }

    public void setSoapBody(Element soapBody) {
        this.soapBody = soapBody;
    }

    public WSHandlerResult getResults() {
        return results;
    }

    public void setResults(WSHandlerResult results) {
        this.results = results;
    }

    public List<WSSecurityEngineResult> getSignedResults() {
        return signedResults;
    }

    public void setSignedResults(List<WSSecurityEngineResult> signedResults) {
        this.signedResults = signedResults;
    }

    public List<WSSecurityEngineResult> getEncryptedResults() {
        return encryptedResults;
    }

    public void setEncryptedResults(List<WSSecurityEngineResult> encryptedResults) {
        this.encryptedResults = encryptedResults;
    }

    public AssertionInfoMap getAssertionInfoMap() {
        return assertionInfoMap;
    }

    public void setAssertionInfoMap(AssertionInfoMap assertionInfoMap) {
        this.assertionInfoMap = assertionInfoMap;
    }

    public List<WSSecurityEngineResult> getUsernameTokenResults() {
        return usernameTokenResults;
    }

    public void setUsernameTokenResults(List<WSSecurityEngineResult> usernameTokenResults) {
        this.usernameTokenResults = usernameTokenResults;
    }

    public List<WSSecurityEngineResult> getSamlResults() {
        return samlResults;
    }

    public void setSamlResults(List<WSSecurityEngineResult> samlResults) {
        this.samlResults = samlResults;
    }

    public Element getTimestampElement() {
        return timestampElement;
    }

    public void setTimestampElement(Element timestampElement) {
        this.timestampElement = timestampElement;
    }

    public boolean isUtWithCallbacks() {
        return utWithCallbacks;
    }

    public void setUtWithCallbacks(boolean utWithCallbacks) {
        this.utWithCallbacks = utWithCallbacks;
    }

    public Element getSoapHeader() {
        return soapHeader;
    }

    public void setSoapHeader(Element soapHeader) {
        this.soapHeader = soapHeader;
    }

    public Collection<WSDataRef> getSigned() {
        return signed;
    }

    public void setSigned(Collection<WSDataRef> signed) {
        this.signed = signed;
    }

    public Collection<WSDataRef> getEncrypted() {
        return encrypted;
    }

    public void setEncrypted(Collection<WSDataRef> encrypted) {
        this.encrypted = encrypted;
    }

}
