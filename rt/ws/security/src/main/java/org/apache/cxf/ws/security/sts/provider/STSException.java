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

import javax.xml.namespace.QName;

/**
 * A RuntimeException that can be thrown by an STS implementation. If the FaultCode is set, then this 
 * code/String will be returned to the user, otherwise the Exception message is returned.
 */
public class STSException extends RuntimeException {

    /**
     * WS-Trust 1.3 namespace
     */
    public static final String WST_NS_05_12 = "http://docs.oasis-open.org/ws-sx/ws-trust/200512";
    
    /**
     * Specification Fault Codes
     */
    public static final QName INVALID_REQUEST = new QName(WST_NS_05_12, "InvalidRequest");
    public static final QName FAILED_AUTH = new QName(WST_NS_05_12, "FailedAuthentication");
    public static final QName REQUEST_FAILED = new QName(WST_NS_05_12, "RequestFailed");
    public static final QName INVALID_TOKEN = new QName(WST_NS_05_12, "InvalidSecurityToken");
    public static final QName AUTH_BAD_ELEMENTS = new QName(WST_NS_05_12, "AuthenticationBadElements");
    public static final QName BAD_REQUEST = new QName(WST_NS_05_12, "BadRequest");
    public static final QName EXPIRED_DATA = new QName(WST_NS_05_12, "ExpiredData");
    public static final QName INVALID_TIME = new QName(WST_NS_05_12, "InvalidTimeRange");
    public static final QName INVALID_SCOPE = new QName(WST_NS_05_12, "InvalidScope");
    public static final QName RENEW_NEEDED = new QName(WST_NS_05_12, "RenewNeeded");
    public static final QName UNABLE_TO_RENEW = new QName(WST_NS_05_12, "UnableToRenew");
    
    /**
     * A map of Fault Code to Fault Strings
     */
    private static final java.util.Map<QName, String> FAULT_CODE_MAP = 
            new java.util.HashMap<QName, String>();

    static {
        FAULT_CODE_MAP.put(INVALID_REQUEST, "The request was invalid or malformed");
        FAULT_CODE_MAP.put(FAILED_AUTH, "Authentication failed");
        FAULT_CODE_MAP.put(REQUEST_FAILED, "The specified request failed");
        FAULT_CODE_MAP.put(INVALID_TOKEN, "Security token has been revoked");
        FAULT_CODE_MAP.put(AUTH_BAD_ELEMENTS, "Insufficient Digest Elements");
        FAULT_CODE_MAP.put(BAD_REQUEST, "The specified RequestSecurityToken is not understood");
        FAULT_CODE_MAP.put(EXPIRED_DATA, "The request data is out-of-date");
        FAULT_CODE_MAP.put(INVALID_TIME, "The requested time range is invalid or unsupported");
        FAULT_CODE_MAP.put(INVALID_SCOPE, "The request scope is invalid or unsupported");
        FAULT_CODE_MAP.put(RENEW_NEEDED, "A renewable security token has expired");
        FAULT_CODE_MAP.put(UNABLE_TO_RENEW, "The requested renewal failed");
    }
    
    /**
     * 
     */
    private static final long serialVersionUID = 2186924985128534490L;
    
    
    private QName faultCode;
    
    public STSException(String message) {
        super(message);
    }
    
    public STSException(String message, QName faultCode) {
        super(message);
        this.faultCode = faultCode;
    }
    
    public STSException(String message, Throwable e) {
        super(message, e);
    }

    public STSException(String message, Throwable e, QName faultCode) {
        super(message, e);
        this.faultCode = faultCode;
    }
    
    public void setFaultCode(QName faultCode) {
        this.faultCode = faultCode;
    }
    
    public QName getFaultCode() {
        return faultCode;
    }
    
    @Override
    public String getMessage() {
        if (faultCode != null && FAULT_CODE_MAP.get(faultCode) != null) {
            return FAULT_CODE_MAP.get(faultCode);
        }
        return super.getMessage();
    }

}
