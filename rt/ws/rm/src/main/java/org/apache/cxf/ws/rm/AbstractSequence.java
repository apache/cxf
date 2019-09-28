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

import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement.AcknowledgementRange;

public abstract class AbstractSequence {

    protected final Identifier id;
    protected SequenceAcknowledgement acknowledgement;
    private final ProtocolVariation protocol;

    protected AbstractSequence(Identifier i, ProtocolVariation p) {
        id = i;
        protocol = p;
    }

    /**
     * @return the sequence identifier
     */
    public Identifier getIdentifier() {
        return id;
    }

    public ProtocolVariation getProtocol() {
        return protocol;
    }

    public String toString() {
        return id.getValue();
    }

    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (other instanceof AbstractSequence) {
            AbstractSequence otherSeq = (AbstractSequence)other;
            return otherSeq.getIdentifier().getValue().equals(getIdentifier().getValue());
        }
        return false;
    }

    public int hashCode() {
        return getIdentifier().getValue().hashCode();
    }

    public static boolean identifierEquals(Identifier id1, Identifier id2) {
        if (null == id1) {
            return null == id2;
        }
        return null != id2 && id1.getValue().equals(id2.getValue());
    }

    public synchronized boolean isAcknowledged(long m) {
        for (AcknowledgementRange r : acknowledgement.getAcknowledgementRange()) {
            if (m >= r.getLower().longValue() && r.getUpper().longValue() >= m) {
                return true;
            }
        }
        return false;
    }

}
