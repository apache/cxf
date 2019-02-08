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
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement;

/**
 * Utility class to construct SequenceFaults.
 */
class SequenceFaultFactory {

    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(SequenceFaultFactory.class);

    private final RMConstants constants;

    SequenceFaultFactory(RMConstants consts) {
        constants = consts;
    }

    SequenceFault createUnknownSequenceFault(Identifier sid) {
        Message msg = new Message("UNKNOWN_SEQ_EXC", BUNDLE);
        SequenceFault fault = new SequenceFault(msg.toString());
        fault.setDetail(sid);
        fault.setSender(true);
        fault.setFaultCode(constants.getUnknownSequenceFaultCode());
        return fault;
    }

    SequenceFault createSequenceTerminatedFault(Identifier sid, boolean sender) {
        Message msg = new Message("SEQ_TERMINATED_EXC", BUNDLE);
        SequenceFault fault = new SequenceFault(msg.toString());
        fault.setDetail(sid);
        fault.setSender(sender);
        fault.setFaultCode(constants.getSequenceTerminatedFaultCode());
        return fault;
    }

    SequenceFault createInvalidAcknowledgementFault(SequenceAcknowledgement ack) {
        Message msg = new Message("INVALID_ACK_EXC", BUNDLE);
        SequenceFault fault = new SequenceFault(msg.toString());
        fault.setDetail(ack);
        fault.setSender(true);
        fault.setFaultCode(constants.getInvalidAcknowledgmentFaultCode());
        return fault;
    }

    SequenceFault createMessageNumberRolloverFault(Identifier sid) {
        Message msg = new Message("MESSAGE_NR_ROLLOVER_EXC", BUNDLE);
        SequenceFault fault = new SequenceFault(msg.toString());
        fault.setDetail(sid);
        fault.setSender(true);
        fault.setFaultCode(constants.getMessageNumberRolloverFaultCode());
        return fault;
    }

    SequenceFault createCreateSequenceRefusedFault() {
        Message msg = new Message("CREATE_SEQ_REFUSED", BUNDLE);
        SequenceFault fault = new SequenceFault(msg.toString());
        fault.setSender(true);
        fault.setFaultCode(constants.getCreateSequenceRefusedFaultCode());
        return fault;
    }
}
