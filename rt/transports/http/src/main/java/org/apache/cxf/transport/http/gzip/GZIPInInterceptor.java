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
package org.apache.cxf.transport.http.gzip;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.HttpHeaderHelper;
import org.apache.cxf.interceptor.AttachmentInInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

/**
 * CXF interceptor that uncompresses those incoming messages that have "gzip"
 * content-encoding. An instance of this class should be added as an in and
 * inFault interceptor on clients that need to talk to a service that returns
 * gzipped responses or on services that want to accept gzipped requests. For
 * clients, you probably also want to use
 * {@link org.apache.cxf.transports.http.configuration.HTTPClientPolicy#setAcceptEncoding}
 * to let the server know you can handle compressed responses. To compress
 * outgoing messages, see {@link GZIPOutInterceptor}. This class was originally
 * based on one of the CXF samples (configuration_interceptor).
 * 
 * @author Ian Roberts (i.roberts@dcs.shef.ac.uk)
 */
public class GZIPInInterceptor extends AbstractPhaseInterceptor<Message> {

    /**
     * Key under which we store the original input stream on the message, for
     * use by the ending interceptor.
     */
    public static final String ORIGINAL_INPUT_STREAM_KEY = GZIPInInterceptor.class.getName()
                                                           + ".originalInputStream";

    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(GZIPInInterceptor.class);
    private static final Logger LOG = LogUtils.getL7dLogger(GZIPInInterceptor.class);

    /**
     * Ending interceptor that restores the original input stream on the message
     * when we have finished unzipping it.
     */
    private GZIPInEndingInterceptor ending = new GZIPInEndingInterceptor();

    public GZIPInInterceptor() {
        super(Phase.RECEIVE);
        addBefore(AttachmentInInterceptor.class.getName());
    }

    public void handleMessage(Message message) throws Fault {
        // check for Content-Encoding header - we are only interested in
        // messages that say they are gzipped.
        Map<String, List<String>> protocolHeaders = CastUtils.cast((Map<?, ?>)message
            .get(Message.PROTOCOL_HEADERS));
        if (protocolHeaders != null) {
            List<String> contentEncoding = CastUtils.cast(HttpHeaderHelper
                .getHeader(protocolHeaders, HttpHeaderHelper.CONTENT_ENCODING));
            if (contentEncoding != null
                && (contentEncoding.contains("gzip") || contentEncoding.contains("x-gzip"))) {
                try {
                    LOG.fine("Uncompressing response");
                    // remember the original input stream, the ending
                    // interceptor
                    // will use it later
                    InputStream is = message.getContent(InputStream.class);
                    message.put(ORIGINAL_INPUT_STREAM_KEY, is);

                    // wrap an unzipping stream around the original one
                    GZIPInputStream zipInput = new GZIPInputStream(is);
                    message.setContent(InputStream.class, zipInput);

                    // remove content encoding header as we've now dealt with it
                    for (String key : protocolHeaders.keySet()) {
                        if (key.equalsIgnoreCase("Content-Encoding")) {
                            protocolHeaders.remove(key);
                            break;
                        }
                    }

                    // add the ending interceptor
                    message.getInterceptorChain().add(ending);
                } catch (IOException ex) {
                    throw new Fault(new org.apache.cxf.common.i18n.Message("COULD_NOT_UNZIP", BUNDLE), ex);
                }
            }
        }
    }

    /**
     * Ending interceptor to restore the original input stream after processing,
     * so as not to interfere with streaming HTTP.
     */
    public class GZIPInEndingInterceptor extends AbstractPhaseInterceptor<Message> {
        public GZIPInEndingInterceptor() {
            super(Phase.POST_INVOKE);
        }

        /**
         * Restores the original input stream for the message.
         */
        public void handleMessage(Message message) throws Fault {
            InputStream originalIn = (InputStream)message.get(ORIGINAL_INPUT_STREAM_KEY);
            message.setContent(InputStream.class, originalIn);
        }
    }
}
