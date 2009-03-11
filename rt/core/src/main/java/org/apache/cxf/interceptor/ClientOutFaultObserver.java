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

package org.apache.cxf.interceptor;

import java.util.Map;
import java.util.SortedSet;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.ClientCallback;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseManager;

public class ClientOutFaultObserver extends AbstractFaultChainInitiatorObserver {

    public ClientOutFaultObserver(Bus bus) {
        super(bus);
    }

    @Override
    protected SortedSet<Phase> getPhases() {
        return getBus().getExtension(PhaseManager.class).getOutPhases();
    }

    /**
     * override the super class method
     */
    public void onMessage(Message m) {
        Exception ex = m.getContent(Exception.class);
        ClientCallback callback = m.getExchange().get(ClientCallback.class);

        if (callback != null) {
            Map<String, Object> resCtx = CastUtils.cast((Map<?, ?>) m.getExchange().getOutMessage().get(
                    Message.INVOCATION_CONTEXT));
            resCtx = CastUtils.cast((Map<?, ?>) resCtx.get(ClientImpl.RESPONSE_CONTEXT));
            callback.handleException(resCtx, ex);
        }
    }

    protected boolean isOutboundObserver() {
        return true;
    }
}
