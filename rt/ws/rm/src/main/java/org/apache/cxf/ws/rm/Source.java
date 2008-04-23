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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.ws.rm.persistence.RMStore;

public class Source extends AbstractEndpoint {

    private static final String REQUESTOR_SEQUENCE_ID = "";
    
    private Map<String, SourceSequence> map;
    private Map<String, SourceSequence> current;     
    private Lock sequenceCreationLock;
    private Condition sequenceCreationCondition;
    private boolean sequenceCreationNotified;

    Source(RMEndpoint reliableEndpoint) {
        super(reliableEndpoint);
        map = new HashMap<String, SourceSequence>();
        current = new HashMap<String, SourceSequence>();
             
        sequenceCreationLock = new ReentrantLock();
        sequenceCreationCondition = sequenceCreationLock.newCondition();
    }  
    
    public SourceSequence getSequence(Identifier id) {        
        return map.get(id.getValue());
    }
    
    public Collection<SourceSequence> getAllSequences() {                 
        return CastUtils.cast(map.values());
    } 
    
    public void addSequence(SourceSequence seq) { 
        addSequence(seq, true);        
    }
    
    public void addSequence(SourceSequence seq, boolean persist) {
        seq.setSource(this);
        map.put(seq.getIdentifier().getValue(), seq);
        if (persist) {
            RMStore store = getReliableEndpoint().getManager().getStore();
            if (null != store) {
                store.createSourceSequence(seq);
            }
        }
    }
    
    public void removeSequence(SourceSequence seq) {        
        map.remove(seq.getIdentifier().getValue());
        RMStore store = getReliableEndpoint().getManager().getStore();
        if (null != store) {
            store.removeSourceSequence(seq.getIdentifier());
        }
    }
    
    /**
     * Returns a collection of all sequences for which have not yet been
     * completely acknowledged.
     * 
     * @return the collection of unacknowledged sequences.
     */
    public Collection<SourceSequence> getAllUnacknowledgedSequences() {
        Collection<SourceSequence> seqs = new ArrayList<SourceSequence>();
        for (SourceSequence seq : map.values()) {
            if (!seq.allAcknowledged()) {
                seqs.add(seq);
            }
        }
        return seqs;        
    }

    /**
     * Returns the current sequence used by a client side source.
     * 
     * @return the current sequence.
     */
    SourceSequence getCurrent() {
        return getCurrent(null);
    }
    
    /**
     * Sets the current sequence used by a client side source.
     * @param s the current sequence.
     */
    public void setCurrent(SourceSequence s) {
        setCurrent(null, s);
    }
    
    /**
     * Returns the current sequence used by a server side source for responses to a message
     * sent as part of the inbound sequence with the specified identifier.
     * 
     * @return the current sequence.
     */
    SourceSequence getCurrent(Identifier i) {        
        sequenceCreationLock.lock();
        try {
            return getAssociatedSequence(i);
        } finally {
            sequenceCreationLock.unlock();
        }
    }

    /**
     * Returns the sequence associated with the given identifier.
     * 
     * @param i the corresponding sequence identifier
     * @return the associated sequence
     * @pre the sequenceCreationLock is already held
     */
    SourceSequence getAssociatedSequence(Identifier i) {        
        return current.get(i == null ? REQUESTOR_SEQUENCE_ID : i.getValue());
    }
    
    /**
     * Await the availability of a sequence corresponding to the given identifier.
     * 
     * @param i the sequence identifier
     * @return
     */
    SourceSequence awaitCurrent(Identifier i) {
        sequenceCreationLock.lock();
        try {
            SourceSequence seq = getAssociatedSequence(i);
            while (seq == null) {
                while (!sequenceCreationNotified) {
                    try {
                        sequenceCreationCondition.await();
                    } catch (InterruptedException ie) {
                        // ignore
                    }
                }
                seq = getAssociatedSequence(i);
            }
            return seq;
        } finally {
            sequenceCreationLock.unlock();
        }
    }
    
    /**
     * Sets the current sequence used by a server side source for responses to a message
     * sent as part of the inbound sequence with the specified identifier.
     * @param s the current sequence.
     */
    void setCurrent(Identifier i, SourceSequence s) {        
        sequenceCreationLock.lock();
        try {
            current.put(i == null ? REQUESTOR_SEQUENCE_ID : i.getValue(), s);
            sequenceCreationNotified = true;
            sequenceCreationCondition.signal();
        } finally {
            sequenceCreationLock.unlock();
        }
    }
}
