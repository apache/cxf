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

import java.io.OutputStream;

import org.apache.cxf.interceptor.AttachmentOutInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.io.WriteOnCloseOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

/**
 * 
 */
public class RetransmissionInterceptor extends AbstractPhaseInterceptor {

    RMManager manager;

    public RetransmissionInterceptor() {
        super(Phase.PRE_STREAM);
        addBefore(StaxOutInterceptor.class.getName());
        addBefore(AttachmentOutInterceptor.class.getName());
    }
    
    public RMManager getManager() {
        return manager;
    }

    public void setManager(RMManager manager) {
        this.manager = manager;
    }

    public void handleMessage(Message message) throws Fault {
        handle(message, false);
    }
    
    @Override
    public void handleFault(Message message) {
        handle(message, true);
    }

    void handle(Message message, boolean isFault) {
        if (null == getManager().getRetransmissionQueue()) {
            return;
        }
          
        OutputStream os = message.getContent(OutputStream.class);
        if (null == os) {
            return;
        }
        if (isFault) { 
            // remove the exception set by the PhaseInterceptorChain so that the 
            // error does not reach the client when retransmission is scheduled 
            message.setContent(Exception.class, null);
            message.getExchange().put(Exception.class, null); 
        } else { 
            WriteOnCloseOutputStream stream = RMUtils.createCachedStream(message, os);
            stream.registerCallback(new RetransmissionCallback(message, getManager()));
        }
    }
}
    
    

   
