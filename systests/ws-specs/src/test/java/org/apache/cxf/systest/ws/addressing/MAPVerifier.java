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

package org.apache.cxf.systest.ws.addressing;


import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.addressing.Names;

import static org.apache.cxf.ws.addressing.JAXWSAConstants.ADDRESSING_PROPERTIES_INBOUND;
import static org.apache.cxf.ws.addressing.JAXWSAConstants.ADDRESSING_PROPERTIES_OUTBOUND;


/**
 * Verifies presence of MAPs in the context.
 */
public class MAPVerifier extends AbstractPhaseInterceptor<Message> {
    VerificationCache verificationCache;
    private final Deque<String> expectedExposedAs = new ConcurrentLinkedDeque<>();

    public MAPVerifier() {
        super(Phase.POST_LOGICAL);
    }

    public void handleMessage(Message message) {
        verify(message);
    }

    public void handleFault(Message message) {
        verify(message);
    }

    private void verify(Message message) {
        boolean isOutbound = ContextUtils.isOutbound(message);
        String mapProperty = isOutbound ? ADDRESSING_PROPERTIES_OUTBOUND : ADDRESSING_PROPERTIES_INBOUND;
        AddressingProperties maps =
            (AddressingProperties)message.get(mapProperty);
        if (maps == null) {
            return;
        }
        if (ContextUtils.isRequestor(message)) {
            if (isOutbound) {
                String exposeAs = getExpectedExposeAs(false);
                if (exposeAs != null) {
                    maps.exposeAs(exposeAs);
                }
            } else {
                String exposeAs = getExpectedExposeAs(true);
                String expected = exposeAs != null
                                  ? exposeAs
                                  : Names.WSA_NAMESPACE_NAME;
                if (!maps.getNamespaceURI().equals(expected)) {
                    verificationCache.put("Incoming version mismatch"
                                          + " expected: " + expected
                                          + " got: " + maps.getNamespaceURI());
                }
                exposeAs = null;
            }
        }
        verificationCache.put(MAPTestBase.verifyMAPs(maps, this));
    }

    private String getExpectedExposeAs(boolean remove) {
        return remove ? expectedExposedAs.pollLast() : expectedExposedAs.peekLast();
    }

    public void setVerificationCache(VerificationCache cache) {
        verificationCache = cache;
    }

    public void addToExpectedExposedAs(String str) {
        expectedExposedAs.add(str);
    }
}
