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
import org.apache.cxf.annotations.Logging;
import org.apache.cxf.annotations.Provider;
import org.apache.cxf.annotations.Provider.Type;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.interceptor.AbstractLoggingInterceptor;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;

/**
 * This class is used to control message-on-the-wire logging.
 * By attaching this feature to an endpoint, you
 * can specify logging. If this feature is present, an endpoint will log input
 * and output of ordinary and log messages.
 *
 * <pre>
 * <![CDATA[
    <jaxws:endpoint ...>
      <jaxws:features>
       <bean class="org.apache.cxf.feature.LoggingFeature"/>
      </jaxws:features>
    </jaxws:endpoint>
  ]]>
  </pre>
 *
 * @deprecated use the logging module rt/features/logging instead
 */
@NoJSR250Annotations
@Deprecated
@Provider(value = Type.Feature)
public class LoggingFeature extends DelegatingFeature<LoggingFeature.Portable> {
    public LoggingFeature() {
        super(new Portable());
    }
    public LoggingFeature(int lim) {
        super(new Portable(lim));
    }
    public LoggingFeature(String in, String out) {
        super(new Portable(in, out));
    }
    public LoggingFeature(String in, String out, int lim) {
        super(new Portable(in, out, lim));
    }

    public LoggingFeature(String in, String out, int lim, boolean p) {
        super(new Portable(in, out, lim, p));
    }

    public LoggingFeature(String in, String out, int lim, boolean p, boolean showBinary) {
        super(new Portable(in, out, lim, p, showBinary));
    }

    public LoggingFeature(Logging annotation) {
        super(new Portable(annotation));
    }

    public void setLimit(int lim) {
        delegate.setLimit(lim);
    }

    public int getLimit() {
        return delegate.getLimit();
    }

    public boolean isPrettyLogging() {
        return delegate.isPrettyLogging();
    }

    public void setPrettyLogging(boolean prettyLogging) {
        delegate.setPrettyLogging(prettyLogging);
    }

    public static class Portable implements AbstractPortableFeature {
        private static final int DEFAULT_LIMIT = AbstractLoggingInterceptor.DEFAULT_LIMIT;
        private static final LoggingInInterceptor IN = new LoggingInInterceptor(DEFAULT_LIMIT);
        private static final LoggingOutInterceptor OUT = new LoggingOutInterceptor(DEFAULT_LIMIT);


        String inLocation;
        String outLocation;
        boolean prettyLogging;
        boolean showBinary;

        int limit = DEFAULT_LIMIT;

        public Portable() {

        }
        public Portable(int lim) {
            limit = lim;
        }
        public Portable(String in, String out) {
            inLocation = in;
            outLocation = out;
        }
        public Portable(String in, String out, int lim) {
            inLocation = in;
            outLocation = out;
            limit = lim;
        }

        public Portable(String in, String out, int lim, boolean p) {
            inLocation = in;
            outLocation = out;
            limit = lim;
            prettyLogging = p;
        }

        public Portable(String in, String out, int lim, boolean p, boolean showBinary) {
            this(in, out, lim, p);
            this.showBinary = showBinary;
        }

        public Portable(Logging annotation) {
            inLocation = annotation.inLocation();
            outLocation = annotation.outLocation();
            limit = annotation.limit();
            prettyLogging = annotation.pretty();
            showBinary = annotation.showBinary();
        }

        @Override
        public void doInitializeProvider(InterceptorProvider provider, Bus bus) {
            if (limit == DEFAULT_LIMIT && inLocation == null
                    && outLocation == null && !prettyLogging) {
                provider.getInInterceptors().add(IN);
                provider.getInFaultInterceptors().add(IN);
                provider.getOutInterceptors().add(OUT);
                provider.getOutFaultInterceptors().add(OUT);
            } else {
                LoggingInInterceptor in = new LoggingInInterceptor(limit);
                in.setOutputLocation(inLocation);
                in.setPrettyLogging(prettyLogging);
                in.setShowBinaryContent(showBinary);
                LoggingOutInterceptor out = new LoggingOutInterceptor(limit);
                out.setOutputLocation(outLocation);
                out.setPrettyLogging(prettyLogging);
                out.setShowBinaryContent(showBinary);

                provider.getInInterceptors().add(in);
                provider.getInFaultInterceptors().add(in);
                provider.getOutInterceptors().add(out);
                provider.getOutFaultInterceptors().add(out);
            }
        }

        /**
         * Set a limit on how much content can be logged
         * @param lim
         */
        public void setLimit(int lim) {
            limit = lim;
        }

        /**
         * Retrieve the value set with {@link #setLimit(int)}.
         */
        public int getLimit() {
            return limit;
        }

        /**
         */
        public boolean isPrettyLogging() {
            return prettyLogging;
        }
        /**
         * Turn pretty logging of XML content on/off
         * @param prettyLogging
         */
        public void setPrettyLogging(boolean prettyLogging) {
            this.prettyLogging = prettyLogging;
        }
    }
}
