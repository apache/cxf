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
package org.apache.cxf.feature;

import java.util.List;

import jakarta.xml.ws.WebServiceFeature;
import org.apache.cxf.Bus;
import org.apache.cxf.interceptor.InterceptorProvider;

/**
 * A Feature is something that is able to customize a Server, Client, or Bus, typically
 * adding capabilities. For instance, there may be a LoggingFeature which configures
 * one of the above to log each of their messages.
 * <p>
 * By default the initialize methods all delegate to initializeProvider(InterceptorProvider).
 * If you're simply adding interceptors to a Server, Client, or Bus, this allows you to add
 * them easily.
 */
public abstract class AbstractFeature extends WebServiceFeature implements AbstractPortableFeature {
    @Override
    public String getID() {
        return getClass().getName();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void doInitializeProvider(InterceptorProvider provider, Bus bus) {
        initializeProvider(provider, bus);
    }

    protected void initializeProvider(InterceptorProvider provider, Bus bus) {
        // no-op
    }

    public static <T> T getActive(List<? extends Feature> features, Class<T> type) {
        return AbstractPortableFeature.getActive(features, type);
    }
}
