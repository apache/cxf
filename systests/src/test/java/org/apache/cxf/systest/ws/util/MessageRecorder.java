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

package org.apache.cxf.systest.ws.util;

import junit.framework.Assert;

public class MessageRecorder extends Assert {
   
    private OutMessageRecorder outRecorder;
    private InMessageRecorder inRecorder;

    public MessageRecorder(OutMessageRecorder or, InMessageRecorder ir) {
        inRecorder = ir;
        outRecorder = or;
    }
 
    public void awaitMessages(int nExpectedOut, int nExpectedIn, int timeout) {
        int waited = 0;
        int nOut = 0;
        int nIn = 0;
        while (waited <= timeout) {                
            synchronized (outRecorder) {
                nOut = outRecorder.getOutboundMessages().size();
            }
            synchronized (inRecorder) {
                nIn = inRecorder.getInboundMessages().size();
            }
            if (nIn >= nExpectedIn && nOut >= nExpectedOut) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                // ignore
            }
            waited += 100;
        }
        assertEquals("Did not receive expected number of inbound messages", nExpectedIn, nIn);
        assertEquals("Did not send expected number of outbound messages", nExpectedOut, nOut);        
    }    
}
