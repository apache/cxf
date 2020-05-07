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

package org.apache.cxf.microprofile.client;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.message.Message;

public final class Utils {

    private Utils() {
    }

    public static ExecutorService getExecutorService(Message message) {
        ExecutorService es = message.get(ExecutorService.class);
        if (es == null) {
            es = AccessController.doPrivileged((PrivilegedAction<ExecutorService>)() -> {
                return ForkJoinPool.commonPool();
            });
        }
        return es;
    }

    public static ExecutorService getExecutorService(MessageContext mc) {
        ExecutorService es = (ExecutorService) mc.get(ExecutorService.class);
        if (es == null) {
            es = AccessController.doPrivileged((PrivilegedAction<ExecutorService>) () -> {
                return ForkJoinPool.commonPool();
            });
        }
        return es;
    }
}