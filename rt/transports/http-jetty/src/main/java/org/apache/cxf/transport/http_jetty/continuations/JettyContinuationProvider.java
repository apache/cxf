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

package org.apache.cxf.transport.http_jetty.continuations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.continuations.Continuation;
import org.apache.cxf.continuations.ContinuationProvider;
import org.apache.cxf.message.Message;

public class JettyContinuationProvider implements ContinuationProvider {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private Message inMessage;
    private JettyContinuationWrapper wrapper;

    public JettyContinuationProvider(HttpServletRequest req,
                                     HttpServletResponse resp,
                                     Message m) {
        request = req;
        response = resp;
        this.inMessage = m;
    }

    public void complete() {
        JettyContinuationWrapper r = getContinuation(false);
        if (r != null) {
            r.reset();
        }
        wrapper = null;
    }
    public Continuation getContinuation() {
        return getContinuation(true);
    }
    public JettyContinuationWrapper getContinuation(boolean create) {
        Message m = inMessage;
        // Get the real message which is used in the interceptor chain
        if (m != null && m.getExchange() != null && m.getExchange().getInMessage() != null) {
            m = m.getExchange().getInMessage();
        }
        if (m == null || m.getExchange() == null || m.getExchange().isOneWay()) {
            return null;
        }
        if (wrapper == null && create) {
            wrapper = new JettyContinuationWrapper(request, response, m);
        }
        return wrapper;
    }

}
