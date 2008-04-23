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

package org.apache.cxf.ws.rm.persistence;

import java.io.InputStream;
import java.math.BigInteger;

import org.apache.cxf.ws.rm.SequenceAcknowledgement;
import org.apache.cxf.ws.rm.SequenceAcknowledgement.AcknowledgementRange;
import org.junit.Assert;
import org.junit.Test;

/**
 * 
 */
public class PersistenceUtilsTest extends Assert {

    @Test
    public void testSerialiseDeserialiseAcknowledgement() {
        SequenceAcknowledgement ack = new SequenceAcknowledgement();
        AcknowledgementRange range = new AcknowledgementRange();
        range.setLower(BigInteger.ONE);
        range.setUpper(BigInteger.TEN);
        ack.getAcknowledgementRange().add(range);
        PersistenceUtils utils = PersistenceUtils.getInstance();
        InputStream is = utils.serialiseAcknowledgment(ack);
        SequenceAcknowledgement refAck = utils.deserialiseAcknowledgment(is);
        assertEquals(refAck.getAcknowledgementRange().size(), refAck.getAcknowledgementRange().size());
        AcknowledgementRange refRange = refAck.getAcknowledgementRange().get(0);
        assertEquals(range.getLower(), refRange.getLower());
        assertEquals(range.getUpper(), refRange.getUpper());
    }
}
