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

import java.util.ResourceBundle;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.i18n.Message;

/**
 * Utility class to construct SequenceFaults.
 */

class SequenceFaultFactory { 

    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(SequenceFaultFactory.class);
    
    SequenceFault createUnknownSequenceFault(Identifier sid) {
        SequenceFaultType sf = RMUtils.getWSRMFactory().createSequenceFaultType();
        sf.setFaultCode(RMConstants.getUnknownSequenceFaultCode());
        Message msg = new Message("UNKNOWN_SEQ_EXC", BUNDLE);
        SequenceFault fault = new SequenceFault(msg.toString());
        fault.setSequenceFault(sf);
        fault.setDetail(sid);
        fault.setSender(true);
        return fault;
    } 
    
    SequenceFault createSequenceTerminatedFault(Identifier sid, boolean sender) {
        SequenceFaultType sf = RMUtils.getWSRMFactory().createSequenceFaultType();
        sf.setFaultCode(RMConstants.getSequenceTerminatedFaultCode());
        Message msg = new Message("SEQ_TERMINATED_EXC", BUNDLE);
        SequenceFault fault = new SequenceFault(msg.toString());
        fault.setSequenceFault(sf);
        fault.setDetail(sid);
        fault.setSender(sender);
        return fault;
    }
    
    SequenceFault createInvalidAcknowledgementFault(SequenceAcknowledgement ack) {
        SequenceFaultType sf = RMUtils.getWSRMFactory().createSequenceFaultType();
        sf.setFaultCode(RMConstants.getInvalidAcknowledgmentFaultCode());
        Message msg = new Message("INVALID_ACK_EXC", BUNDLE);
        SequenceFault fault = new SequenceFault(msg.toString());
        fault.setSequenceFault(sf);
        fault.setDetail(ack);
        fault.setSender(true);
        return fault;
    }
    
    SequenceFault createMessageNumberRolloverFault(Identifier sid) {
        SequenceFaultType sf = RMUtils.getWSRMFactory().createSequenceFaultType();
        sf.setFaultCode(RMConstants.getMessageNumberRolloverFaultCode());
        Message msg = new Message("MESSAGE_NR_ROLLOVER_EXC", BUNDLE);
        SequenceFault fault = new SequenceFault(msg.toString());
        fault.setSequenceFault(sf);
        fault.setDetail(sid);
        fault.setSender(true);
        return fault;
    }
    
    SequenceFault createLastMessageNumberExceededFault(Identifier sid) {
        SequenceFaultType sf = RMUtils.getWSRMFactory().createSequenceFaultType();
        sf.setFaultCode(RMConstants.getLastMessageNumberExceededFaultCode());
        Message msg = new Message("LAST_MESSAGE_NUMBER_EXCEEDED_EXC", BUNDLE);
        SequenceFault fault = new SequenceFault(msg.toString());
        fault.setSequenceFault(sf);
        fault.setDetail(sid);
        fault.setSender(true);
        return fault;
    }
    
    SequenceFault createCreateSequenceRefusedFault() {
        SequenceFaultType sf = RMUtils.getWSRMFactory().createSequenceFaultType();
        sf.setFaultCode(RMConstants.getCreateSequenceRefusedFaultCode());
        Message msg = new Message("CREATE_SEQ_REFUSED", BUNDLE);
        SequenceFault fault = new SequenceFault(msg.toString());
        fault.setSequenceFault(sf);
        fault.setSender(true);
        return fault;
    }
    
    
}
