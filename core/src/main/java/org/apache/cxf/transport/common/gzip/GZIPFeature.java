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
package org.apache.cxf.transport.common.gzip;

import java.util.List;

import org.apache.cxf.Bus;
import org.apache.cxf.annotations.Provider;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.feature.AbstractPortableFeature;
import org.apache.cxf.feature.DelegatingFeature;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.message.Message;

/**
 * This class is used to control GZIP compression of messages.
 * Attaching this feature to an endpoint will allow the endpoint to handle
 * compressed requests, and will cause outgoing responses to be compressed if
 * the client indicates (via the Accept-Encoding header) that it can handle
 * them.
 * <pre>
 * <![CDATA[
 * <jaxws:endpoint ...>
 *   <jaxws:features>
 *     <bean class="org.apache.cxf.transport.common.gzip.GZIPFeature"/>
 *   </jaxws:features>
 * </jaxws:endpoint>
 * ]]>
 * </pre>
 * Attaching this feature to a client will cause outgoing request messages
 * to be compressed and incoming compressed responses to be uncompressed.
 * Accept-Encoding header is sent to let the service know
 * that your client can accept compressed responses.
 */
@NoJSR250Annotations
@Provider(value = Provider.Type.Feature)
public class GZIPFeature extends DelegatingFeature<GZIPFeature.Portable> {

    public GZIPFeature() {
        super(new Portable());
    }

    public void remove(List<Interceptor<? extends Message>> outInterceptors) {
        delegate.remove(outInterceptors);
    }

    public void setThreshold(int threshold) {
        delegate.setThreshold(threshold);
    }

    public int getThreshold() {
        return delegate.getThreshold();
    }

    public void setForce(boolean b) {
        delegate.setForce(b);
    }

    public boolean getForce() {
        return delegate.getForce();
    }

    public static class Portable implements AbstractPortableFeature {
        private static final GZIPInInterceptor IN = new GZIPInInterceptor();
        private static final GZIPOutInterceptor OUT = new GZIPOutInterceptor();

        /**
         * The compression threshold to pass to the outgoing interceptor.
         */
        int threshold = -1;

        /**
         * Force GZIP instead of negotiate
         */
        boolean force;


        @Override
        public void doInitializeProvider(InterceptorProvider provider, Bus bus) {
            provider.getInInterceptors().add(IN);
            if (threshold == -1 && !force) {
                provider.getOutInterceptors().add(OUT);
                provider.getOutFaultInterceptors().add(OUT);
            } else {
                GZIPOutInterceptor out = new GZIPOutInterceptor();
                out.setThreshold(threshold);
                out.setForce(force);
                remove(provider.getOutInterceptors());
                remove(provider.getOutFaultInterceptors());
                provider.getOutInterceptors().add(out);
                provider.getOutFaultInterceptors().add(out);
            }
        }

        private void remove(List<Interceptor<? extends Message>> outInterceptors) {
            int x = outInterceptors.size();
            while (x > 0) {
                --x;
                if (outInterceptors.get(x) instanceof GZIPOutInterceptor) {
                    outInterceptors.remove(x);
                }
            }
        }

        public void setThreshold(int threshold) {
            this.threshold = threshold;
        }

        public int getThreshold() {
            return threshold;
        }


        /**
         * Set if GZIP is always used without negotiation
         * @param b
         */
        public void setForce(boolean b) {
            force = b;
        }

        /**
         * Retrieve the value set with {@link #setForce(boolean)}.
         */
        public boolean getForce() {
            return force;
        }
    }
}
