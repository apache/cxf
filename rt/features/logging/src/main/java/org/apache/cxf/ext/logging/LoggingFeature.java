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
package org.apache.cxf.ext.logging;

import org.apache.cxf.Bus;
import org.apache.cxf.annotations.Provider;
import org.apache.cxf.annotations.Provider.Type;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.ext.logging.event.LogEventSender;
import org.apache.cxf.ext.logging.event.PrettyLoggingFilter;
import org.apache.cxf.ext.logging.slf4j.Slf4jEventSender;
import org.apache.cxf.ext.logging.slf4j.Slf4jVerboseEventSender;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.interceptor.InterceptorProvider;

/**
 * This class is used to control message-on-the-wire logging.
 * By attaching this feature to an endpoint, you
 * can specify logging. If this feature is present, an endpoint will log input
 * and output of ordinary and log messages.
 * <pre>
 * <![CDATA[
    <jaxws:endpoint ...>
      <jaxws:features>
       <bean class="org.apache.cxf.ext.logging.LoggingFeature"/>
      </jaxws:features>
    </jaxws:endpoint>
  ]]>
  </pre>
 */
@NoJSR250Annotations
@Provider(value = Type.Feature)
public class LoggingFeature extends AbstractFeature {
    private LoggingInInterceptor in;
    private LoggingOutInterceptor out;
    private PrettyLoggingFilter inPrettyFilter;
    private PrettyLoggingFilter outPrettyFilter;

    public LoggingFeature() {
        LogEventSender sender = new Slf4jVerboseEventSender();
        inPrettyFilter = new PrettyLoggingFilter(sender);
        outPrettyFilter = new PrettyLoggingFilter(sender);
        in = new LoggingInInterceptor(inPrettyFilter);
        out = new LoggingOutInterceptor(outPrettyFilter);
    }

    @Override
    protected void initializeProvider(InterceptorProvider provider, Bus bus) {

        provider.getInInterceptors().add(in);
        provider.getInFaultInterceptors().add(in);

        provider.getOutInterceptors().add(out);
        provider.getOutFaultInterceptors().add(out);
    }

    public void setLimit(int limit) {
        in.setLimit(limit);
        out.setLimit(limit);
    }

    public void setInMemThreshold(long inMemThreshold) {
        in.setInMemThreshold(inMemThreshold);
        out.setInMemThreshold(inMemThreshold);
    }

    public void setSender(LogEventSender sender) {
        this.inPrettyFilter.setNext(sender);
        this.outPrettyFilter.setNext(sender);
    }
    public void setInSender(LogEventSender s) {
        this.inPrettyFilter.setNext(s);
    }
    public void setOutSender(LogEventSender s) {
        this.outPrettyFilter.setNext(s);
    }

    public void setPrettyLogging(boolean prettyLogging) {
        this.inPrettyFilter.setPrettyLogging(prettyLogging);
        this.outPrettyFilter.setPrettyLogging(prettyLogging);
    }

    /**
     * Log binary content?
     * @param logBinary defaults to false
     */
    public void setLogBinary(boolean logBinary) {
        in.setLogBinary(logBinary);
        out.setLogBinary(logBinary);
    }

    /**
     * Log multipart content?
     * @param logMultipart defaults to true
     */
    public void setLogMultipart(boolean logMultipart) {
        in.setLogMultipart(logMultipart);
        out.setLogMultipart(logMultipart);
    }

    public void setVerbose(boolean verbose) {
        setSender(verbose ? new Slf4jVerboseEventSender() : new Slf4jEventSender());
    }

    /**
     * Add additional binary media types to the default values in the LoggingInInterceptor.
     * Content for these types will not be logged.
     * For example:
     * <pre>
     * &lt;bean id="loggingFeature" class="org.apache.cxf.ext.logging.LoggingFeature"&gt;
     *   &lt;property name="addInBinaryContentMediaTypes" value="audio/mpeg;application/zip"/&gt;
     * &lt;/bean&gt;
     * </pre>
     * @param mediaTypes list of mediaTypes. symbol ; - delimeter
     */
    public void addInBinaryContentMediaTypes(String mediaTypes) {
        in.addBinaryContentMediaTypes(mediaTypes);
    }

    /**
     * Add additional binary media types to the default values in the LoggingOutInterceptor.
     * Content for these types will not be logged.
     * For example:
     * <pre>
     * &lt;bean id="loggingFeature" class="org.apache.cxf.ext.logging.LoggingFeature"&gt;
     *   &lt;property name="addOutBinaryContentMediaTypes" value="audio/mpeg;application/zip"/&gt;
     * &lt;/bean&gt;
     * </pre>
     * @param mediaTypes list of mediaTypes. symbol ; - delimeter
     */
    public void addOutBinaryContentMediaTypes(String mediaTypes) {
        out.addBinaryContentMediaTypes(mediaTypes);
    }

    /**
     * Add additional binary media types to the default values for both logging interceptors
     * Content for these types will not be logged.
     * For example:
     * <pre>
     * &lt;bean id="loggingFeature" class="org.apache.cxf.ext.logging.LoggingFeature"&gt;
     *   &lt;property name="addBinaryContentMediaTypes" value="audio/mpeg;application/zip"/&gt;
     * &lt;/bean&gt;
     * </pre>
     * @param mediaTypes list of mediaTypes. symbol ; - delimeter
     */
    public void addBinaryContentMediaTypes(String mediaTypes) {
        addInBinaryContentMediaTypes(mediaTypes);
        addOutBinaryContentMediaTypes(mediaTypes);
    }
}
