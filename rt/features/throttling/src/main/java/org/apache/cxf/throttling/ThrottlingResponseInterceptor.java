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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

/**
 *
 */
public class ThrottlingResponseInterceptor extends AbstractPhaseInterceptor<Message> {
    public ThrottlingResponseInterceptor() {
        super(Phase.SETUP);
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        ThrottleResponse rsp = message.getExchange().get(ThrottleResponse.class);
        if (rsp != null) {
            if (rsp.getResponseCode() > 0) {
                message.put(Message.RESPONSE_CODE, rsp.getResponseCode());
                if (rsp.getErrorMessage() != null) {
                    message.put(Message.ERROR_MESSAGE, rsp.getErrorMessage());
                }
            }
            Map<String, List<String>> headers = CastUtils.cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));

            if (headers == null) {
                headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                message.put(Message.PROTOCOL_HEADERS, headers);
            }
            for (Map.Entry<String, String> e : rsp.getResponseHeaders().entrySet()) {
                List<String> r = headers.get(e.getKey());
                if (r == null) {
                    r = new ArrayList<>();
                    headers.put(e.getKey(), r);
                }
                r.add(e.getValue());
            }
            if (rsp.getResponseCode() == 503 && rsp.getDelay() > 0
                && !rsp.getResponseHeaders().containsKey("Retry-After")) {
                String retryAfter = Long.toString(rsp.getDelay() / 1000);
                headers.put("Retry-After", Collections.singletonList(retryAfter));
            }
        }
        ThrottlingCounter tCounter = message.getExchange().get(ThrottlingCounter.class);
        if (tCounter != null) {
            tCounter.decrementAndGet();
        }
    }
}
