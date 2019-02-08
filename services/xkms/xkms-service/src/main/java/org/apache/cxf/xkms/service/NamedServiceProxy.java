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
package org.apache.cxf.xkms.service;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.osgi.util.tracker.ServiceTracker;

public class NamedServiceProxy implements InvocationHandler {
    private ServiceTracker<?, ?> tracker;
    private String filter;

    public NamedServiceProxy(ServiceTracker<?, ?> tracker, String filter) {
        this.tracker = tracker;
        this.filter = filter;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object service = tracker.getService();
        if (service == null) {
            throw new IllegalStateException("No service found for filter: " + filter);
        }
        return method.invoke(service, args);
    }

    public void close() {
        tracker.close();
    }
}
