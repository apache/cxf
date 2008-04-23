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

import java.util.ArrayList;
import java.util.Collection;

public class RMProperties {
    private SequenceType sequence;
    private Collection<SequenceAcknowledgement> acks;
    private Collection<AckRequestedType> acksRequested;
    
    public Collection<SequenceAcknowledgement> getAcks() {
        return acks;
    }
    
    public Collection<AckRequestedType> getAcksRequested() {
        return acksRequested;
    }
    
    public SequenceType getSequence() {
        return sequence;
    }
    
    public void setAcks(Collection<SequenceAcknowledgement> a) {
        acks = a;
    }
    
    public void setAcksRequested(Collection<AckRequestedType> ar) {
        acksRequested = ar;       
    }
    
    public void setSequence(SequenceType s) {
        sequence = s;
    }
    
    public void setSequence(SourceSequence seq) {
        SequenceType s = RMUtils.getWSRMFactory().createSequenceType();
        s.setIdentifier(seq.getIdentifier());
        s.setMessageNumber(seq.getCurrentMessageNr());   
        if (seq.isLastMessage()) {
            s.setLastMessage(new SequenceType.LastMessage());
        }
        setSequence(s);
    }
    
    public void addAck(DestinationSequence seq) {
        if (null == acks) {
            acks = new ArrayList<SequenceAcknowledgement>();
        }
        SequenceAcknowledgement ack = seq.getAcknowledgment();
        acks.add(ack);
        seq.acknowledgmentSent();
    }
  
}
