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
 * Holder for names for WS-ReliableMessaging 1.0.
 */
public final class RM10Constants extends RMConstants {
    
    public static final RM10Constants INSTANCE = new RM10Constants();
    
    // namespaces
    public static final String NAMESPACE_URI = "http://schemas.xmlsoap.org/ws/2005/02/rm";
    
    public static final String WSRMP_NAMESPACE_URI = "http://schemas.xmlsoap.org/ws/2005/02/rm/policy";
    
    public static final String WSDL_NAMESPACE_URI = NAMESPACE_URI + "/wsdl";
    
    // element and header names
    
    public static final QName SEQUENCE_QNAME = new QName(NAMESPACE_URI, SEQUENCE_NAME);
    
    public static final QName SEQUENCE_FAULT_QNAME = new QName(NAMESPACE_URI, SEQUENCE_FAULT_NAME);
    
    public static final QName SEQUENCE_ACK_QNAME = new QName(NAMESPACE_URI, SEQUENCE_ACK_NAME);
    
    public static final QName ACK_REQUESTED_QNAME = new QName(NAMESPACE_URI, ACK_REQUESTED_NAME);
    
    public static final QName WSRMP_RMASSERTION_QNAME = new QName(WSRMP_NAMESPACE_URI, RMASSERTION_NAME);
    
    // protocol operation names
    
    public static final QName PORT_NAME =
        new QName(WSDL_NAMESPACE_URI, "SequenceAbstractSoapPort"); 
    
    public static final QName CREATE_SEQUENCE_QNAME = new QName(NAMESPACE_URI, "CreateSequence");
    
    public static final QName CREATE_SEQUENCE_RESPONSE_QNAME =
        new QName(NAMESPACE_URI, "CreateSequenceResponse");
    
    public static final QName TERMINATE_SEQUENCE_QNAME =
        new QName(NAMESPACE_URI, "TerminateSequence");
    
    public static final QName TERMINATE_SEQUENCE_ANONYMOUS_QNAME =
        new QName(NAMESPACE_URI, "TerminateSequenceAnonymous");
    
    public static final QName SEQUENCE_ACKNOWLEDGEMENT_QNAME =
        new QName(NAMESPACE_URI, "SequenceAcknowledgement");
    
    public static final QName CLOSE_SEQUENCE_QNAME = new QName(NAMESPACE_URI, "CloseSequence");
    
    public static final QName ACK_REQ_QNAME = new QName(NAMESPACE_URI, "AckRequested");
    
    public static final QName CREATE_SEQUENCE_ONEWAY_QNAME =
        new QName(NAMESPACE_URI, "CreateSequenceOneway");
    
    public static final QName CREATE_SEQUENCE_RESPONSE_ONEWAY_QNAME =
        new QName(NAMESPACE_URI, "CreateSequenceResponseOneway");
    
    public static final QName RMASSERTION_QNAME =  new QName(WSRMP_NAMESPACE_URI, RMASSERTION_NAME);

    // actions
    
    public static final String CREATE_SEQUENCE_ACTION =
        NAMESPACE_URI + "/CreateSequence";
    
    public static final String CREATE_SEQUENCE_RESPONSE_ACTION =
        NAMESPACE_URI + "/CreateSequenceResponse";
    
    public static final String TERMINATE_SEQUENCE_ACTION =
        NAMESPACE_URI + "/TerminateSequence";
    
    public static final String CLOSE_SEQUENCE_ACTION =
        NAMESPACE_URI + "/LastMessage";    
    
    public static final String ACK_REQUESTED_ACTION =
        NAMESPACE_URI + "/AckRequested";
    
    public static final String SEQUENCE_ACKNOWLEDGMENT_ACTION =
        NAMESPACE_URI + "/SequenceAcknowledgement";
    
    public static final String SEQUENCE_INFO_ACTION =
        NAMESPACE_URI + "/SequenceInfo";
    
    public static final Set<String> ACTIONS;
    static {
        Set<String> actions = new HashSet<String>();
        actions.add(CREATE_SEQUENCE_ACTION);
        actions.add(CREATE_SEQUENCE_RESPONSE_ACTION);
        actions.add(TERMINATE_SEQUENCE_ACTION);
        actions.add(CLOSE_SEQUENCE_ACTION);
        actions.add(ACK_REQUESTED_ACTION);
        actions.add(SEQUENCE_ACKNOWLEDGMENT_ACTION);
        actions.add(SEQUENCE_INFO_ACTION);
        ACTIONS = actions;
    }

    // fault codes
    
    public static final QName UNKNOWN_SEQUENCE_FAULT_QNAME =
        new QName(NAMESPACE_URI, UNKNOWN_SEQUENCE_FAULT_CODE);
    
    public static final QName SEQUENCE_TERMINATED_FAULT_QNAME =
        new QName(NAMESPACE_URI, SEQUENCE_TERMINATED_FAULT_CODE);
    
    public static final QName INVALID_ACKNOWLEDGMENT_FAULT_QNAME =
        new QName(NAMESPACE_URI, INVALID_ACKNOWLEDGMENT_FAULT_CODE);
    
    public static final QName MESSAGE_NUMBER_ROLLOVER_FAULT_QNAME =
        new QName(NAMESPACE_URI, MESSAGE_NUMBER_ROLLOVER_FAULT_CODE);
    
    public static final QName CREATE_SEQUENCE_REFUSED_FAULT_QNAME =
        new QName(NAMESPACE_URI, CREATE_SEQUENCE_REFUSED_FAULT_CODE);
    
    // headers
    
    public static final Set<QName> HEADERS;
    static {
        Set<QName> headers = new HashSet<QName>();
        headers.add(new QName(NAMESPACE_URI, SEQUENCE_NAME));
        headers.add(new QName(NAMESPACE_URI, SEQUENCE_ACK_NAME));
        headers.add(new QName(NAMESPACE_URI, ACK_REQUESTED_NAME));
        HEADERS = Collections.unmodifiableSet(headers);
    }
    
    private RM10Constants() {
    }
    
    public String getWSRMNamespace() {
        return NAMESPACE_URI;
    }
    
    // actions access methods

    public String getCreateSequenceAction() {
        return CREATE_SEQUENCE_ACTION;
    }

    public String getCreateSequenceResponseAction() {
        return CREATE_SEQUENCE_RESPONSE_ACTION;
    }
    
    public String getTerminateSequenceAction() {
        return TERMINATE_SEQUENCE_ACTION;
    }
    
    public String getCloseSequenceAction() {
        return CLOSE_SEQUENCE_ACTION;
    }
    
    public String getAckRequestedAction() {
        return ACK_REQUESTED_ACTION;
    }
    
    public String getSequenceAckAction() {
        return SEQUENCE_ACKNOWLEDGMENT_ACTION;
    }
    
    public String getSequenceInfoAction() {
        return SEQUENCE_INFO_ACTION;
    }
    
    // service model constants access methods
    
    public QName getPortName() {
        return PORT_NAME;
    }
    
    public QName getCreateSequenceOperationName() {
        return CREATE_SEQUENCE_QNAME;
    }
    
    public QName getCreateSequenceResponseOperationName() {
        return CREATE_SEQUENCE_RESPONSE_QNAME;
    }
    
    public QName getCreateSequenceOnewayOperationName() {
        return CREATE_SEQUENCE_ONEWAY_QNAME;
    }
    
    public QName getCreateSequenceResponseOnewayOperationName() {
        return CREATE_SEQUENCE_RESPONSE_ONEWAY_QNAME;
    }
    
    public QName getTerminateSequenceOperationName() {
        return TERMINATE_SEQUENCE_QNAME;
    }
    
    public QName getTerminateSequenceAnonymousOperationName() {
        return TERMINATE_SEQUENCE_ANONYMOUS_QNAME;
    }
    
    public QName getSequenceAckOperationName() {
        return SEQUENCE_ACKNOWLEDGEMENT_QNAME;
    }
    
    public QName getCloseSequenceOperationName() {
        return CLOSE_SEQUENCE_QNAME;
    }
    
    public QName getAckRequestedOperationName() {
        return ACK_REQ_QNAME;
    }
    
    // fault codes access methods
    
    public QName getUnknownSequenceFaultCode() {
        return UNKNOWN_SEQUENCE_FAULT_QNAME;
    }
        
    public QName getSequenceTerminatedFaultCode() {
        return SEQUENCE_TERMINATED_FAULT_QNAME;
    }
        
    public QName getInvalidAcknowledgmentFaultCode() {
        return INVALID_ACKNOWLEDGMENT_FAULT_QNAME;
    }
  
    public QName getMessageNumberRolloverFaultCode() {
        return MESSAGE_NUMBER_ROLLOVER_FAULT_QNAME;
    }
    
    public QName getCreateSequenceRefusedFaultCode() {
        return CREATE_SEQUENCE_REFUSED_FAULT_QNAME;
    }
    
    public QName getSequenceClosedFaultCode() {
        return null;
    }
    
    public QName getWSRMRequiredFaultCode() {
        return null;
    }
}