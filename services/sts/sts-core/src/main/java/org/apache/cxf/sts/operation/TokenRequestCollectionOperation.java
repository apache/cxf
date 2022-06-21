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

package org.apache.cxf.sts.operation;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBElement;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenCollectionType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseCollectionType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.operation.CancelOperation;
import org.apache.cxf.ws.security.sts.provider.operation.IssueSingleOperation;
import org.apache.cxf.ws.security.sts.provider.operation.RenewOperation;
import org.apache.cxf.ws.security.sts.provider.operation.RequestCollectionOperation;
import org.apache.cxf.ws.security.sts.provider.operation.ValidateOperation;

/**
 * An implementation of the RequestCollectionOperation interface. It is composed of the different
 * Operation implementations
 */
public class TokenRequestCollectionOperation extends AbstractOperation
    implements RequestCollectionOperation {

    public static final String WSTRUST_REQUESTTYPE_BATCH_ISSUE = STSConstants.WST_NS_05_12
        + "/BatchIssue";
    public static final String WSTRUST_REQUESTTYPE_BATCH_CANCEL = STSConstants.WST_NS_05_12
        + "/BatchCancel";
    public static final String WSTRUST_REQUESTTYPE_BATCH_RENEW = STSConstants.WST_NS_05_12
        + "/BatchRenew";
    public static final String WSTRUST_REQUESTTYPE_BATCH_VALIDATE = STSConstants.WST_NS_05_12
        + "/BatchValidate";

    static final Logger LOG = LogUtils.getL7dLogger(TokenRequestCollectionOperation.class);

    private IssueSingleOperation issueSingleOperation;
    private ValidateOperation validateOperation;
    private RenewOperation renewOperation;
    private CancelOperation cancelOperation;

    public RequestSecurityTokenResponseCollectionType requestCollection(
        RequestSecurityTokenCollectionType requestCollection,
        Principal principal,
        Map<String, Object> messageContext) {
        RequestSecurityTokenResponseCollectionType responseCollection =
            QNameConstants.WS_TRUST_FACTORY.createRequestSecurityTokenResponseCollectionType();

        String requestType = null;
        for (RequestSecurityTokenType request : requestCollection.getRequestSecurityToken()) {
            List<?> objectList = request.getAny();
            for (Object o : objectList) {
                if (o instanceof JAXBElement) {
                    QName qname = ((JAXBElement<?>) o).getName();
                    if (qname.equals(new QName(STSConstants.WST_NS_05_12, "RequestType"))) {
                        String val = ((JAXBElement<?>) o).getValue().toString();
                        // All batch requests must have the same RequestType
                        if (val == null || (requestType != null && !requestType.equals(val))) {
                            LOG.log(
                                Level.WARNING,
                                "All RequestSecurityTokenCollection elements do not share the same "
                                + "RequestType"
                            );
                            throw new STSException(
                                "Error in requesting a token", STSException.REQUEST_FAILED
                            );
                        }
                        requestType = val;
                    }
                }
            }

            RequestSecurityTokenResponseType response =
                handleRequest(request, principal, messageContext, requestType);
            responseCollection.getRequestSecurityTokenResponse().add(response);
        }
        return responseCollection;
    }

    public RequestSecurityTokenResponseType handleRequest(
            RequestSecurityTokenType request,
            Principal principal,
            Map<String, Object> messageContext,
            String requestType
    ) {
        if (WSTRUST_REQUESTTYPE_BATCH_ISSUE.equals(requestType)) {
            if (issueSingleOperation == null) {
                LOG.log(Level.WARNING, "IssueSingleOperation is null");
                throw new STSException(
                    "Error in requesting a token", STSException.REQUEST_FAILED
                );
            }
            return issueSingleOperation.issueSingle(request, principal, messageContext);
        } else if (WSTRUST_REQUESTTYPE_BATCH_VALIDATE.equals(requestType)) {
            if (validateOperation == null) {
                LOG.log(Level.WARNING, "ValidateOperation is null");
                throw new STSException(
                    "Error in requesting a token", STSException.REQUEST_FAILED
                );
            }
            return validateOperation.validate(request, principal, messageContext);
        } else if (WSTRUST_REQUESTTYPE_BATCH_CANCEL.equals(requestType)) {
            if (cancelOperation == null) {
                LOG.log(Level.WARNING, "CancelOperation is null");
                throw new STSException(
                    "Error in requesting a token", STSException.REQUEST_FAILED
                );
            }
            return cancelOperation.cancel(request, principal, messageContext);
        } else if (WSTRUST_REQUESTTYPE_BATCH_RENEW.equals(requestType)) {
            if (renewOperation == null) {
                LOG.log(Level.WARNING, "RenewOperation is null");
                throw new STSException(
                    "Error in requesting a token", STSException.REQUEST_FAILED
                );
            }
            return renewOperation.renew(request, principal, messageContext);
        } else {
            LOG.log(Level.WARNING, "Unknown operation requested");
            throw new STSException(
                "Error in requesting a token", STSException.REQUEST_FAILED
            );
        }
    }

    public IssueSingleOperation getIssueSingleOperation() {
        return issueSingleOperation;
    }

    public void setIssueSingleOperation(IssueSingleOperation issueSingleOperation) {
        this.issueSingleOperation = issueSingleOperation;
    }

    public ValidateOperation getValidateOperation() {
        return validateOperation;
    }

    public void setValidateOperation(ValidateOperation validateOperation) {
        this.validateOperation = validateOperation;
    }

    public RenewOperation getRenewOperation() {
        return renewOperation;
    }

    public void setRenewOperation(RenewOperation renewOperation) {
        this.renewOperation = renewOperation;
    }

    public CancelOperation getCancelOperation() {
        return cancelOperation;
    }

    public void setCancelOperation(CancelOperation cancelOperation) {
        this.cancelOperation = cancelOperation;
    }

}
