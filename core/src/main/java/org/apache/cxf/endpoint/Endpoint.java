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

package org.apache.cxf.endpoint;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import org.apache.cxf.binding.Binding;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.MessageObserver;

/**
 * Represents an endpoint that receives messages.
 *
 */
public interface Endpoint extends Map<String, Object>, InterceptorProvider {

    EndpointInfo getEndpointInfo();

    Binding getBinding();

    Service getService();

    void setExecutor(Executor executor);

    Executor getExecutor();

    MessageObserver getInFaultObserver();

    MessageObserver getOutFaultObserver();

    void setInFaultObserver(MessageObserver observer);

    void setOutFaultObserver(MessageObserver observer);

    List<Feature> getActiveFeatures();

    /**
     * Add a hook that will be called when this end point being terminated.
     * This will be called prior to the Server/ClientLifecycleListener.*Destroyed()
     * method is called.  This provides an opportunity to cleanup any resources
     * that are specific to this Endpoint.
     * @param c
     */
    void addCleanupHook(Closeable c);
    List<Closeable> getCleanupHooks();
}
