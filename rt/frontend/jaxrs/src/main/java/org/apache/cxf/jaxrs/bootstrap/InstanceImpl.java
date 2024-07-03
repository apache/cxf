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

package org.apache.cxf.jaxrs.bootstrap;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.SeBootstrap.Configuration;
import jakarta.ws.rs.SeBootstrap.Instance;
import org.apache.cxf.endpoint.Server;

public class InstanceImpl implements Instance {
    private final Server server;
    private final Configuration configuration;

    public InstanceImpl(Server server, Configuration configuration) {
        this.server = server;
        this.configuration = configuration;
    }

    @Override
    public Configuration configuration() {
        return configuration;
    }

    @Override
    public CompletionStage<StopResult> stop() {
        return CompletableFuture
            .runAsync(() -> server.stop())
            .thenApply(f -> new StopResult() {
                @Override
                public <T> T unwrap(final Class<T> nativeClass) {
                    return nativeClass.cast(Void.class);
                }
            });
    }

    @Override
    public <T> T unwrap(Class<T> nativeClass) {
        return nativeClass.cast(Server.class);
    }

}
