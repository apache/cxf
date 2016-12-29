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
package org.apache.cxf.tracing.brave;

import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.http.HttpClientResponseAdapter;
import com.github.kristofa.brave.http.HttpServerRequestAdapter;
import com.github.kristofa.brave.http.SpanNameProvider;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;

/**
 * 
 */
@NoJSR250Annotations
public class BraveStartInterceptor extends AbstractBraveInterceptor {
    public BraveStartInterceptor(Brave brave, SpanNameProvider spanNameProvider) {
        super(Phase.PRE_INVOKE, brave, spanNameProvider);
    }

    public void handleMessage(Message message) throws Fault {       
        if (MessageUtils.isRequestor(message)) {
            brave.clientResponseInterceptor().handle(new HttpClientResponseAdapter(() -> 200));
        } else {
            HttpServerRequestAdapter adapter = 
                new HttpServerRequestAdapter(getServerRequest(message), spanNameProvider);
            brave.serverRequestInterceptor().handle(adapter);
        }
    }
}
