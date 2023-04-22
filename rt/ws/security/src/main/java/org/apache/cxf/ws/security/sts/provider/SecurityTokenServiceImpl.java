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

package org.apache.cxf.ws.security.sts.provider;

import java.security.Principal;
import java.util.Map;

import jakarta.annotation.Resource;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPFault;
import jakarta.xml.ws.WebServiceContext;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.binding.soap.saaj.SAAJFactoryResolver;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenCollectionType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseCollectionType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.operation.CancelOperation;
import org.apache.cxf.ws.security.sts.provider.operation.IssueOperation;
import org.apache.cxf.ws.security.sts.provider.operation.IssueSingleOperation;
import org.apache.cxf.ws.security.sts.provider.operation.KeyExchangeTokenOperation;
import org.apache.cxf.ws.security.sts.provider.operation.RenewOperation;
import org.apache.cxf.ws.security.sts.provider.operation.RequestCollectionOperation;
import org.apache.cxf.ws.security.sts.provider.operation.ValidateOperation;


public class SecurityTokenServiceImpl implements SecurityTokenService {

    private CancelOperation cancelOperation;
    private IssueOperation issueOperation;
    private IssueSingleOperation issueSingleOperation;
    private KeyExchangeTokenOperation keyExchangeTokenOperation;
    private RenewOperation renewOperation;
    private RequestCollectionOperation requestCollectionOperation;
    private ValidateOperation validateOperation;

    @Resource
    private WebServiceContext context;

    public void setCancelOperation(CancelOperation cancelOperation) {
        this.cancelOperation = cancelOperation;
    }

    public void setIssueOperation(IssueOperation issueOperation) {
        this.issueOperation = issueOperation;
    }

    public void setIssueSingleOperation(IssueSingleOperation issueSingleOperation) {
        this.issueSingleOperation = issueSingleOperation;
    }

    public void setKeyExchangeTokenOperation(
            KeyExchangeTokenOperation keyExchangeTokenOperation) {
        this.keyExchangeTokenOperation = keyExchangeTokenOperation;
    }

    public void setRenewOperation(RenewOperation renewOperation) {
        this.renewOperation = renewOperation;
    }

    public void setRequestCollectionOperation(
            RequestCollectionOperation requestCollectionOperation) {
        this.requestCollectionOperation = requestCollectionOperation;
    }

    public void setValidateOperation(ValidateOperation validateOperation) {
        this.validateOperation = validateOperation;
    }

    public RequestSecurityTokenResponseType validate(
            RequestSecurityTokenType request) {
        if (validateOperation == null) {
            throwUnsupportedOperation("Validate");
        }
        return validateOperation.validate(request, getPrincipal(), getMessageContext());
    }


    public RequestSecurityTokenResponseCollectionType requestCollection(
            RequestSecurityTokenCollectionType requestCollection) {
        if (requestCollectionOperation == null) {
            throwUnsupportedOperation("RequestCollection");
        }
        return requestCollectionOperation.requestCollection(requestCollection, getPrincipal(), getMessageContext());
    }

    public RequestSecurityTokenResponseType keyExchangeToken(
            RequestSecurityTokenType request) {
        if (keyExchangeTokenOperation == null) {
            throwUnsupportedOperation("KeyExchangeToken");
        }
        return keyExchangeTokenOperation.keyExchangeToken(request, getPrincipal(), getMessageContext());
    }

    public RequestSecurityTokenResponseCollectionType issue(
            RequestSecurityTokenType request) {
        if (issueOperation == null) {
            throwUnsupportedOperation("Issue");
        }
        return issueOperation.issue(request, getPrincipal(), getMessageContext());
    }

    public RequestSecurityTokenResponseType issueSingle(
            RequestSecurityTokenType request) {
        if (issueSingleOperation == null) {
            throwUnsupportedOperation("IssueSingle");
        }
        return issueSingleOperation.issueSingle(request, getPrincipal(), getMessageContext());
    }

    public RequestSecurityTokenResponseType cancel(
            RequestSecurityTokenType request) {
        if (cancelOperation == null) {
            throwUnsupportedOperation("Cancel");
        }
        return cancelOperation.cancel(request, getPrincipal(), getMessageContext());
    }

    public RequestSecurityTokenResponseType renew(
            RequestSecurityTokenType request) {
        if (renewOperation == null) {
            throwUnsupportedOperation("Renew");
        }
        return renewOperation.renew(request, getPrincipal(), getMessageContext());
    }

    protected Principal getPrincipal() {
        return context.getUserPrincipal();
    }

    protected Map<String, Object> getMessageContext() {
        return context.getMessageContext();
    }

    private void throwUnsupportedOperation(String string) {
        try {
            SOAPFault fault = SAAJFactoryResolver.createSOAPFactory(null).createFault();
            fault.setFaultString("Unsupported operation " + string);
            throw new SOAPFaultException(fault);
        } catch (SOAPException e) {
            throw new Fault(e);
        }
    }

}