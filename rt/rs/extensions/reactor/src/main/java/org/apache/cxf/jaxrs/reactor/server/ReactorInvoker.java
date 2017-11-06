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

import java.util.function.Consumer;
import org.apache.cxf.jaxrs.JAXRSInvoker;
import org.apache.cxf.jaxrs.impl.AsyncResponseImpl;
import org.apache.cxf.message.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ReactorInvoker extends JAXRSInvoker {
    @Override
    protected AsyncResponseImpl checkFutureResponse(Message inMessage, Object result) {
        if (result instanceof Flux) {
            final Flux<?> flux = (Flux<?>) result;
            final AsyncResponseImpl asyncResponse = new AsyncResponseImpl(inMessage);
            flux.doOnNext(asyncResponse::resume)
                    .doOnError(asyncResponse::resume)
                    .doOnComplete(asyncResponse::onComplete)
                    .subscribe();
            return asyncResponse;
        } else if (result instanceof Mono) {
            // mono is only 0 or 1 element, so when something comes in need to complete the async
            final Mono<?> flux = (Mono<?>) result;
            final AsyncResponseImpl asyncResponse = new AsyncResponseImpl(inMessage);
            flux.doOnNext((Consumer<Object>) o -> {
                asyncResponse.resume(o);
                asyncResponse.onComplete();
            })
            .doOnError((Consumer<Throwable>) throwable -> {
                asyncResponse.resume(throwable);
                asyncResponse.onComplete();
            })
                .subscribe();
            return asyncResponse;
        }
        return null;
    }
}
