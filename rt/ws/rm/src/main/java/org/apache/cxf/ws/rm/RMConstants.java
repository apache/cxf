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

package org.apache.cxf.ws.rm;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;


/**
 * Holder for WS-RM names (of headers, namespaces etc.).
 */
public final class RMConstants {
   
    // namespaces
    private static final String WSRM_NAMESPACE_NAME = 
        "http://schemas.xmlsoap.org/ws/2005/02/rm";
    
    private static final String WSRMP_NAMESPACE_NAME = 
        "http://schemas.xmlsoap.org/ws/2005/02/rm/policy";
    
    private static final String WSA_NAMESPACE_NAME = 
        "http://schemas.xmlsoap.org/ws/2004/08/addressing";
    
    private static final String WSRM_NAMESPACE_PREFIX = "wsrm";
    
    private static final String WSRM_WSDL_NAMESPACE_NAME = 
        WSRM_NAMESPACE_NAME + "/wsdl";
    
  
    // element and header names
    
    private static final String WSRM_SEQUENCE_NAME =
        "Sequence";    
    
    private static final QName WSRM_SEQUENCE_QNAME =
        new QName(WSRM_NAMESPACE_NAME, WSRM_SEQUENCE_NAME);
    
    private static final String WSRM_SEQUENCE_FAULT_NAME =
        "SequenceFault";
    
    private static final QName WSRM_SEQUENCE_FAULT_QNAME =
        new QName(WSRM_NAMESPACE_NAME, WSRM_SEQUENCE_FAULT_NAME);
    
    private static final String WSRM_SEQUENCE_ACK_NAME =
        "SequenceAcknowledgement";
    
    private static final QName WSRM_SEQUENCE_ACK_QNAME =
        new QName(WSRM_NAMESPACE_NAME, WSRM_SEQUENCE_ACK_NAME);
    
    private static final String WSRM_ACK_REQUESTED_NAME =
        "AckRequested";
    
    private static final QName WSRM_ACK_REQUESTED_QNAME =
        new QName(WSRM_NAMESPACE_NAME, WSRM_ACK_REQUESTED_NAME);
    
    private static final String WSA_ANONYMOUS_ADDRESS = 
        WSA_NAMESPACE_NAME + "/role/anonymous";
    
    private static final String WSA_NONE_ADDRESS =
        WSA_NAMESPACE_NAME + "/role/none";
    
    private static final String RMASSERTION_NAME = "RMAssertion";
    
    private static final QName RMASSERTION_QNAME = 
        new QName(WSRMP_NAMESPACE_NAME, RMASSERTION_NAME);
      
    private static final Set<QName> HEADERS;
    
    // protocol operation names
    
    private static final QName WSRM_PORT_NAME =
        new QName(WSRM_WSDL_NAMESPACE_NAME, "SequenceAbstractSoapPort"); 
    
    private static final QName WSRM_CREATE_SEQUENCE_QNAME =
        new QName(WSRM_NAMESPACE_NAME, "CreateSequence");
    
    private static final QName WSRM_CREATE_SEQUENCE_RESPONSE_QNAME =
        new QName(WSRM_NAMESPACE_NAME, "CreateSequenceResponse");
    
    private static final QName WSRM_CREATE_SEQUENCE_ONEWAY_QNAME =
        new QName(WSRM_NAMESPACE_NAME, "CreateSequenceOneway");
    
    private static final QName WSRM_CREATE_SEQUENCE_RESPONSE_ONEWAY_QNAME =
        new QName(WSRM_NAMESPACE_NAME, "CreateSequenceResponseOneway");
    
    private static final QName WSRM_TERMINATE_SEQUENCE_QNAME =
        new QName(WSRM_NAMESPACE_NAME, "TerminateSequence");
    
    private static final QName WSRM_SEQUENCE_ACKNOWLEDGEMENT_QNAME =
        new QName(WSRM_NAMESPACE_NAME, "SequenceAcknowledgement");
    
    private static final QName WSRM_LAST_MESSAGE_QNAME =
        new QName(WSRM_NAMESPACE_NAME, "LastMessage");
    
    private static final QName WSRM_ACK_REQ_QNAME =
        new QName(WSRM_NAMESPACE_NAME, "AckRequested");
 

    // actions
    
    private static final String WSRM_CREATE_SEQUENCE_ACTION =
        WSRM_NAMESPACE_NAME + "/CreateSequence";
    
    private static final String WSRM_CREATE_SEQUENCE_RESPONSE_ACTION =
        WSRM_NAMESPACE_NAME + "/CreateSequenceResponse";
    
    private static final String WSRM_TERMINATE_SEQUENCE_ACTION =
        WSRM_NAMESPACE_NAME + "/TerminateSequence";
    
    private static final String WSRM_SEQUENCE_ACK_ACTION =
        WSRM_NAMESPACE_NAME + "/SequenceAcknowledgement";
    
    private static final String WSRM_LAST_MESSAGE_ACTION =
        WSRM_NAMESPACE_NAME + "/LastMessage";
    
    private static final String WSRM_ACK_REQUESTED_ACTION =
        WSRM_NAMESPACE_NAME + "/AckRequested";
    
    private static final String WSRM_SEQUENCE_ACKNOWLEDGMENT_ACTION =
        WSRM_NAMESPACE_NAME + "/SequenceAcknowledgement";
    
    private static final String WSRM_SEQUENCE_INFO_ACTION =
        WSRM_NAMESPACE_NAME + "/SequenceInfo";
    
    // fault codes
    
    private static final String WSRM_UNKNOWN_SEQUENCE_FAULT_CODE =
        "UnknownSequence";
    
    private static final String WSRM_SEQUENCE_TERMINATED_FAULT_CODE =
        "SequenceTerminated";
    
    private static final String WSRM_INVALID_ACKNOWLEDGMENT_FAULT_CODE =
        "InvalidAcknowledgement";
    
    private static final String WSRM_MESSAGE_NUMBER_ROLLOVER_FAULT_CODE =
        "MessageNumberRollover";
    
    private static final String WSRM_CREATE_SEQUENCE_REFUSED_FAULT_CODE =
        "CreateSequenceRefused";
    
    private static final String WSRM_LAST_MESSAGE_NUMBER_EXCEEDED_FAULT_CODE =
        "LastMessageNumberExceeded";
    
    static {
        Set<QName> headers = new HashSet<QName>();
        headers.add(WSRM_SEQUENCE_QNAME);
        headers.add(WSRM_SEQUENCE_ACK_QNAME);
        headers.add(WSRM_ACK_REQUESTED_QNAME);
        HEADERS = Collections.unmodifiableSet(headers);
    }
    
    private RMConstants() {
    }
    
    public static Set<QName> getHeaders() {
        return HEADERS;
    }
    
    // namespaces
    
    public static String getNamespace() {
        return WSRM_NAMESPACE_NAME; 
    }
        
    public static String getPolicyNamespace() {
        return WSRMP_NAMESPACE_NAME;
    }
    
    public static String getAddressingNamespace() {
        return WSA_NAMESPACE_NAME;        
    }
    
    public static String getNamespacePrefix() {
        return WSRM_NAMESPACE_PREFIX;
    }
    
    public static  String getWsdlNamespace() {
        return WSRM_WSDL_NAMESPACE_NAME;
    }  
    
    // schema type constants
  
    public static String getSequenceName() {
        return WSRM_SEQUENCE_NAME;
    }
    
    public static QName getSequenceQName() {
        return WSRM_SEQUENCE_QNAME;
    }
    
    public static String getSequenceFaultName() {
        return WSRM_SEQUENCE_FAULT_NAME;
    }
    
    public static QName getSequenceFaultQName() {
        return WSRM_SEQUENCE_FAULT_QNAME;
    }
    
    public static String getSequenceAckName() {
        return WSRM_SEQUENCE_ACK_NAME;
    }
    
    public static QName getSequenceAckQName() {
        return WSRM_SEQUENCE_ACK_QNAME;
    }
    
    public static String getAckRequestedName() {
        return WSRM_ACK_REQUESTED_NAME;
    }
    
    public static QName getAckRequestedQName() {
        return WSRM_ACK_REQUESTED_QNAME;
    }
    
    public static String getAnonymousAddress() {
        return WSA_ANONYMOUS_ADDRESS; 
    }
    
    public static String getNoneAddress() {
        return WSA_NONE_ADDRESS;
    }
    
    // service model constants
    
    public static QName getPortName() {
        return WSRM_PORT_NAME;
    }
    
    public static QName getCreateSequenceOperationName() {
        return WSRM_CREATE_SEQUENCE_QNAME;
    }
    
    public static QName getCreateSequenceResponseOperationName() {
        return WSRM_CREATE_SEQUENCE_RESPONSE_QNAME;
    }
    
    public static QName getCreateSequenceOnewayOperationName() {
        return WSRM_CREATE_SEQUENCE_ONEWAY_QNAME;
    }
    
    public static QName getCreateSequenceResponseOnewayOperationName() {
        return WSRM_CREATE_SEQUENCE_RESPONSE_ONEWAY_QNAME;
    }
    
    public static QName getTerminateSequenceOperationName() {
        return WSRM_TERMINATE_SEQUENCE_QNAME;
    }
    
    public static QName getSequenceAckOperationName() {
        return WSRM_SEQUENCE_ACKNOWLEDGEMENT_QNAME;
    }
    
    public static QName getLastMessageOperationName() {
        return WSRM_LAST_MESSAGE_QNAME;
    }
    
    public static QName getAckRequestedOperationName() {
        return WSRM_ACK_REQ_QNAME;
    }
    
    // actions
    
    public static String getCreateSequenceAction() {
        return WSRM_CREATE_SEQUENCE_ACTION;
    }

    public static String getCreateSequenceResponseAction() {
        return WSRM_CREATE_SEQUENCE_RESPONSE_ACTION;
    }
    
    public static String getTerminateSequenceAction() {
        return WSRM_TERMINATE_SEQUENCE_ACTION;
    }
    
    public static String getSequenceAckAction() {
        return WSRM_SEQUENCE_ACK_ACTION;
    }
    
    public static String getLastMessageAction() {
        return WSRM_LAST_MESSAGE_ACTION;
    }
    
    public static String getAckRequestedAction() {
        return WSRM_ACK_REQUESTED_ACTION;
    }
    
    public static String getSequenceAcknowledgmentAction() {
        return WSRM_SEQUENCE_ACKNOWLEDGMENT_ACTION;
    }
    
    public static String getSequenceInfoAction() {
        return WSRM_SEQUENCE_INFO_ACTION;
    }
    
    // fault codes
    
    public static QName getUnknownSequenceFaultCode() {
        return new QName(WSRM_NAMESPACE_NAME, WSRM_UNKNOWN_SEQUENCE_FAULT_CODE);
    }
        
    public static QName getSequenceTerminatedFaultCode() {
        return new QName(WSRM_NAMESPACE_NAME, WSRM_SEQUENCE_TERMINATED_FAULT_CODE);
    }
        
    public static QName getInvalidAcknowledgmentFaultCode() {
        return new QName(RMConstants.WSRM_NAMESPACE_NAME, WSRM_INVALID_ACKNOWLEDGMENT_FAULT_CODE);
    }
  
    public static QName getMessageNumberRolloverFaultCode() {
        return new QName(WSRM_NAMESPACE_NAME, WSRM_MESSAGE_NUMBER_ROLLOVER_FAULT_CODE);
    }
    
    public static QName getCreateSequenceRefusedFaultCode() {
        return new QName(WSRM_NAMESPACE_NAME, WSRM_CREATE_SEQUENCE_REFUSED_FAULT_CODE);
    }
    
    public static QName getLastMessageNumberExceededFaultCode() {
        return new QName(WSRM_NAMESPACE_NAME, WSRM_LAST_MESSAGE_NUMBER_EXCEEDED_FAULT_CODE);
    }
    
    // policies
    
    public static String getRMAssertionName() {
        return RMASSERTION_NAME;
    }
    
    public static QName getRMAssertionQName() {
        return RMASSERTION_QNAME;
    }
}
