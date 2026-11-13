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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.SystemPropertyAction;
import org.apache.cxf.endpoint.Endpoint;
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
 */
public class GZIPInInterceptor extends AbstractPhaseInterceptor<Message> {

    /**
     * The maximum size in bytes that a gzip-encoded message may decompress to. A negative value means
     * no limit. The default is 100 MB.
     */
    public static final String GZIP_MAX_DECOMPRESSED_SIZE = "gzip-max-decompressed-size";
    public static final long DEFAULT_GZIP_MAX_DECOMPRESSED_SIZE =
        SystemPropertyAction.getInteger("org.apache.cxf.gzip-max-decompressed-size", 100 * 1024 * 1024);

    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(GZIPInInterceptor.class);
    private static final Logger LOG = LogUtils.getL7dLogger(GZIPInInterceptor.class);

    public GZIPInInterceptor() {
        super(Phase.RECEIVE);
        addBefore(AttachmentInInterceptor.class.getName());
    }

    public void handleMessage(Message message) {
        if (isGET(message)) {
            return;
        }
        // check for Content-Encoding header - we are only interested in
        // messages that say they are gzipped.
        Map<String, List<String>> protocolHeaders = CastUtils.cast((Map<?, ?>)message
            .get(Message.PROTOCOL_HEADERS));
        if (protocolHeaders != null) {
            List<String> contentEncoding = HttpHeaderHelper.getHeader(protocolHeaders,
                                                                      HttpHeaderHelper.CONTENT_ENCODING);
            if (contentEncoding == null) {
                contentEncoding = protocolHeaders.get(GZIPOutInterceptor.SOAP_JMS_CONTENTENCODING);
            }
            if (contentEncoding != null
                && (contentEncoding.contains("gzip") || contentEncoding.contains("x-gzip"))) {
                try {
                    LOG.fine("Uncompressing response");
                    InputStream is = message.getContent(InputStream.class);
                    if (is == null) {
                        return;
                    }

                    final Integer responseCode = (Integer)message.get(Message.RESPONSE_CODE);
                    if (responseCode != null && responseCode == 204) {
                        return;
                    }

                    // wrap an unzipping stream around the original one
                    InputStream zipInput = new GZIPInputStream(is);
                    long maxSize = getMaxDecompressedSize(message);
                    if (maxSize >= 0) {
                        zipInput = new SizeLimitedInputStream(zipInput, maxSize);
                    }
                    message.setContent(InputStream.class, zipInput);

                    // remove content encoding header as we've now dealt with it
                    for (String key : protocolHeaders.keySet()) {
                        if ("Content-Encoding".equalsIgnoreCase(key)) {
                            protocolHeaders.remove(key);
                            break;
                        }
                    }

                    if (isRequestor(message)) {
                        //record the fact that is worked so future requests will
                        //automatically be FI enabled
                        Endpoint ep = message.getExchange().getEndpoint();
                        ep.put(GZIPOutInterceptor.USE_GZIP_KEY, GZIPOutInterceptor.UseGzip.YES);
                    }
                } catch (IOException ex) {
                    throw new Fault(new org.apache.cxf.common.i18n.Message("COULD_NOT_UNZIP", BUNDLE), ex);
                }
            }
        }
    }

    private static long getMaxDecompressedSize(Message message) {
        Object maxSize = message.getContextualProperty(GZIP_MAX_DECOMPRESSED_SIZE);
        if (maxSize instanceof Number) {
            return ((Number)maxSize).longValue();
        } else if (maxSize instanceof String) {
            try {
                return Long.parseLong(((String)maxSize).trim());
            } catch (NumberFormatException ex) {
                LOG.warning("Invalid value for " + GZIP_MAX_DECOMPRESSED_SIZE + ": " + maxSize);
            }
        }
        return DEFAULT_GZIP_MAX_DECOMPRESSED_SIZE;
    }

    /**
     * Throws an IOException once more than the given number of bytes has been read, to guard against
     * a small compressed message that decompresses to a very large one. The underlying stream is closed
     * at that point, and any further read throws the same exception.
     */
    private static final class SizeLimitedInputStream extends FilterInputStream {
        private final long maxSize;
        private long count;
        private boolean exceeded;

        SizeLimitedInputStream(InputStream in, long maxSize) {
            super(in);
            this.maxSize = maxSize;
        }

        @Override
        public int read() throws IOException {
            checkNotExceeded();
            int b = super.read();
            if (b != -1) {
                checkSize(1);
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            checkNotExceeded();
            int n = super.read(b, off, len);
            if (n > 0) {
                checkSize(n);
            }
            return n;
        }

        @Override
        public long skip(long n) throws IOException {
            checkNotExceeded();
            long skipped = super.skip(n);
            if (skipped > 0) {
                checkSize(skipped);
            }
            return skipped;
        }

        @Override
        public boolean markSupported() {
            return false;
        }

        private void checkSize(long n) throws IOException {
            count += n;
            if (count > maxSize) {
                exceeded = true;
                // the stream can't be used any further, so close it to release the Inflater straight away
                try {
                    in.close();
                } catch (IOException ex) {
                    // ignore, the size limit error is the one to report
                }
                checkNotExceeded();
            }
        }

        private void checkNotExceeded() throws IOException {
            if (exceeded) {
                throw new IOException(new org.apache.cxf.common.i18n.Message("GZIP_MAX_SIZE_EXCEEDED",
                                                                             BUNDLE, maxSize).toString());
            }
        }
    }
}
