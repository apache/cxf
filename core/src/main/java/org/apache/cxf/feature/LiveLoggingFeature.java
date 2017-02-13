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

import org.apache.cxf.Bus;
import org.apache.cxf.annotations.Provider;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.interceptor.AbstractLoggingInterceptor;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.interceptor.LiveLoggingInInterceptor;
import org.apache.cxf.interceptor.LiveLoggingOutInterceptor;
import org.apache.cxf.message.Message;

/**
 * This class is used to enable message-on-the-wire logging on a running bus.
 */
@NoJSR250Annotations
@Provider(value = Provider.Type.Feature)
public class LiveLoggingFeature extends LoggingFeature {
    private static final int DEFAULT_LIMIT = AbstractLoggingInterceptor.DEFAULT_LIMIT;
    private static final LiveLoggingInInterceptor IN = new LiveLoggingInInterceptor(DEFAULT_LIMIT);
    private static final LiveLoggingOutInterceptor OUT = new LiveLoggingOutInterceptor(DEFAULT_LIMIT);

    public LiveLoggingFeature() {
    }

    @Override
    protected void initializeProvider(InterceptorProvider provider, Bus bus) {
        if (limit == DEFAULT_LIMIT && inLocation == null
                && outLocation == null && !prettyLogging) {
            provider.getInInterceptors().add(IN);
            provider.getInFaultInterceptors().add(IN);
            provider.getOutInterceptors().add(OUT);
            provider.getOutFaultInterceptors().add(OUT);
        } else {
            LiveLoggingInInterceptor in = new LiveLoggingInInterceptor(limit);
            in.setOutputLocation(inLocation);
            in.setPrettyLogging(prettyLogging);
            in.setShowBinaryContent(showBinary);
            LiveLoggingOutInterceptor out = new LiveLoggingOutInterceptor(limit);
            out.setOutputLocation(outLocation);
            out.setPrettyLogging(prettyLogging);
            out.setShowBinaryContent(showBinary);

            provider.getInInterceptors().add(in);
            provider.getInFaultInterceptors().add(in);
            provider.getOutInterceptors().add(out);
            provider.getOutFaultInterceptors().add(out);
        }
    }

    public synchronized void removeLiveLogging(Bus bus) {
        if (bus == null) {
            return;
        }

        while (bus.getInInterceptors().iterator().hasNext()) {
            Interceptor<? extends Message> in = bus.getInFaultInterceptors().iterator().next();
            if (in instanceof LiveLoggingInInterceptor) {
                bus.getInInterceptors().remove(in);
                bus.getInFaultInterceptors().remove(in);
            }
        }

        while (bus.getOutInterceptors().iterator().hasNext()) {
            Interceptor<? extends Message> out = bus.getOutFaultInterceptors().iterator().next();
            if (out instanceof LiveLoggingOutInterceptor) {
                bus.getOutInterceptors().remove(out);
                bus.getOutFaultInterceptors().remove(out);
            }
        }
    }
}
