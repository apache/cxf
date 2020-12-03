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

package org.apache.cxf.systest.http_undertow;

import java.io.File;
import java.io.IOException;

import org.apache.cxf.transport.http_undertow.CXFUndertowHttpHandler;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.accesslog.AccessLogHandler;
import io.undertow.server.handlers.accesslog.AccessLogReceiver;
import io.undertow.server.handlers.accesslog.DefaultAccessLogReceiver;

public class LogHandler implements CXFUndertowHttpHandler {

    private HttpHandler next;
    private AccessLogHandler accessLogHandler;

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        if (accessLogHandler == null) {
            buildLogHandler();
        }
        this.accessLogHandler.handleRequest(exchange);
    }

    @Override
    public void setNext(HttpHandler nextHandler) {
        this.next = nextHandler;

    }

    private void buildLogHandler() {
        HttpHandler handler = this.next;
        XnioWorker xnioWorker = createWorker(this.getClass().getClassLoader());
        AccessLogReceiver logReceiver = DefaultAccessLogReceiver.builder().setLogWriteExecutor(xnioWorker)
            .setOutputDirectory(new File("target").toPath()).setLogBaseName("request.")
            .setLogNameSuffix("log").setRotate(true).build();
        this.accessLogHandler = new AccessLogHandler(handler, logReceiver, "combined",
                                                     AccessLogHandler.class.getClassLoader());

    }

    public static XnioWorker createWorker(ClassLoader loader) {
        try {
            if (loader == null) {
                loader = Undertow.class.getClassLoader();
            }
            Xnio xnio = Xnio.getInstance(loader);
            return xnio.createWorker(OptionMap.builder().set(Options.THREAD_DAEMON, true).getMap());
        } catch (IOException ignore) {

            return null;
        }
    }
}
