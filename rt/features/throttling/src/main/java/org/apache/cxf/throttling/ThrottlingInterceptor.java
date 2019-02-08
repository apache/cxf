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

package org.apache.cxf.throttling;

import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.continuations.Continuation;
import org.apache.cxf.continuations.ContinuationProvider;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.OutgoingChainInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;

/**
 *
 */
public class ThrottlingInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final Logger LOG = LogUtils.getL7dLogger(ThrottlingInterceptor.class);

    final ThrottlingManager manager;
    public ThrottlingInterceptor(String phase, ThrottlingManager manager) {
        super(ThrottlingInterceptor.class.getName() + "-" + phase, phase);
        this.manager = manager;
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        ThrottleResponse rsp = manager.getThrottleResponse(getPhase(), message);
        if (rsp == null) {
            return;
        }
        message.getExchange().put(ThrottleResponse.class, rsp);
        if (rsp.getResponseCode() >= 300) {
            createOutMessage(message);
            message.getInterceptorChain().doInterceptStartingAt(message,
                                                                OutgoingChainInterceptor.class.getName());
            return;
        }

        long l = rsp.getDelay();
        if (l > 0) {
            ContinuationProvider cp = message.get(ContinuationProvider.class);
            if (cp == null) {
                LOG.warning("No ContinuationProvider available, sleeping on current thread");
                try {
                    Thread.sleep(l);
                } catch (InterruptedException e) {
                    //ignore
                }
                return;
            }
            Continuation c = cp.getContinuation();
            c.suspend(l);
        }
    }
    private Message createOutMessage(Message inMessage) {
        Endpoint e = inMessage.getExchange().getEndpoint();
        Message mout = e.getBinding().createMessage();
        mout.setExchange(inMessage.getExchange());
        mout.setInterceptorChain(
             OutgoingChainInterceptor.getOutInterceptorChain(inMessage.getExchange()));
        inMessage.getExchange().setOutMessage(mout);
        inMessage.getExchange().put("cxf.io.cacheinput", Boolean.FALSE);
        return mout;
    }
}
