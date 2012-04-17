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

import javax.xml.namespace.QName;

/**
 * Holder for WS-RM names (of headers, namespaces etc.).
 */
public abstract class RMConstants {
    
    public static final String NAMESPACE_PREFIX = "wsrm";
    
    // WSDL
    
    public static final String SERVICE_NAME = "SequenceAbstractService";
    
    public static final String INTERFACE_NAME = "SequenceAbstractPortType";
    
    public static final String BINDING_NAME = "SequenceAbstractSoapBinding";
  
    // element and header names
    
    public static final String SEQUENCE_NAME = "Sequence";    
    
    public static final String SEQUENCE_FAULT_NAME = "SequenceFault";
    
    public static final String SEQUENCE_ACK_NAME = "SequenceAcknowledgement";
    
    public static final String ACK_REQUESTED_NAME = "AckRequested";
    
    public static final String RMASSERTION_NAME = "RMAssertion";
    
    // fault codes
    
    public static final String UNKNOWN_SEQUENCE_FAULT_CODE = "UnknownSequence";
    
    public static final String SEQUENCE_TERMINATED_FAULT_CODE = "SequenceTerminated";
    
    public static final String INVALID_ACKNOWLEDGMENT_FAULT_CODE = "InvalidAcknowledgement";
    
    public static final String MESSAGE_NUMBER_ROLLOVER_FAULT_CODE = "MessageNumberRollover";
    
    public static final String CREATE_SEQUENCE_REFUSED_FAULT_CODE = "CreateSequenceRefused";
    
    // WS-RM 1.1 only
    public static final String SEQUENCE_CLOSED_FAULT_CODE = "SequenceClosed";
    
    public static final String WSRM_REQUIRED_FAULT_CODE = "WSRMRequired";
    
    public abstract String getWSRMNamespace();
    
    // actions access methods
    
    public abstract String getCreateSequenceAction();

    public abstract String getCreateSequenceResponseAction();
    
    public abstract String getCloseSequenceAction();
    
    public abstract String getTerminateSequenceAction();
    
    public abstract String getAckRequestedAction();
    
    public abstract String getSequenceAckAction();
    
    public abstract String getSequenceInfoAction();
    
    // service model constants access methods
    
    public abstract QName getPortName();
    
    public abstract QName getCreateSequenceOperationName();
    
    public abstract QName getCreateSequenceResponseOperationName();
    
    public abstract QName getCreateSequenceOnewayOperationName();
    
    public abstract QName getCreateSequenceResponseOnewayOperationName();
    
    public abstract QName getCloseSequenceOperationName();
    
    public abstract QName getTerminateSequenceOperationName();
    
    public abstract QName getTerminateSequenceAnonymousOperationName();
    
    public abstract QName getSequenceAckOperationName();
    
    public abstract QName getAckRequestedOperationName();
    
    // fault codes access methods
    
    public abstract QName getUnknownSequenceFaultCode();
        
    public abstract QName getSequenceTerminatedFaultCode();
        
    public abstract QName getInvalidAcknowledgmentFaultCode();
  
    public abstract QName getMessageNumberRolloverFaultCode();
    
    public abstract QName getCreateSequenceRefusedFaultCode();
    
    /**
     * Get SequenceClosed fault code.
     * 
     * @return code, or <code>null</code> if not supported
     */
    public abstract QName getSequenceClosedFaultCode();
    
    /**
     * Get WSRMRequired fault code.
     * 
     * @return code, or <code>null</code> if not supported
     */
    public abstract QName getWSRMRequiredFaultCode();
}