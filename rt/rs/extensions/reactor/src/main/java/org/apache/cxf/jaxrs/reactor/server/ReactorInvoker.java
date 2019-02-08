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
package org.apache.cxf.jaxrs.reactor.server;

import org.apache.cxf.jaxrs.impl.AsyncResponseImpl;
import org.apache.cxf.jaxrs.reactivestreams.server.AbstractReactiveInvoker;
import org.apache.cxf.message.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ReactorInvoker extends AbstractReactiveInvoker {
    
    @Override
    protected AsyncResponseImpl checkFutureResponse(Message inMessage, Object result) {
        if (result instanceof Flux) {
            final Flux<?> flux = (Flux<?>) result;
            final AsyncResponseImpl asyncResponse = new AsyncResponseImpl(inMessage);
            if (!isStreamingSubscriberUsed(flux, asyncResponse, inMessage)) {
                flux.doOnNext(asyncResponse::resume)
                    .doOnError(t -> handleThrowable(asyncResponse, t))
                    .subscribe();
            }
            return asyncResponse;
        } else if (result instanceof Mono) {
            final Mono<?> mono = (Mono<?>) result;
            final AsyncResponseImpl asyncResponse = new AsyncResponseImpl(inMessage);
            mono.doOnNext(asyncResponse::resume)
                .doOnError(t -> handleThrowable(asyncResponse, t))
                .subscribe();
            return asyncResponse;
        }
        return null;
    }
    
}
