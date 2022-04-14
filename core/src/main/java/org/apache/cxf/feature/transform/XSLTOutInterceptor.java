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

package org.apache.cxf.feature.transform;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Templates;
import javax.xml.transform.stream.StreamSource;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.AbstractOutDatabindingInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.io.CachedOutputStreamCallback;
import org.apache.cxf.io.CachedWriter;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.DelegatingXMLStreamWriter;
import org.apache.cxf.staxutils.StaxUtils;

/** Class provides XSLT transformation of outgoing message.
 * Actually it breaks streaming (can be fixed in further versions when XSLT engine supports XML stream)
 */
public class XSLTOutInterceptor extends AbstractXSLTInterceptor {
    private static final Logger LOG = LogUtils.getL7dLogger(XSLTOutInterceptor.class);

    public XSLTOutInterceptor(String xsltPath) {
        super(Phase.PRE_STREAM, StaxOutInterceptor.class, null, xsltPath);
    }

    public XSLTOutInterceptor(String phase, Class<?> before, Class<?> after, String xsltPath) {
        super(phase, before, after, xsltPath);
    }

    @Override
    public void handleMessage(Message message) {
        if (checkContextProperty(message)) {
            return;
        }

        // 1. Try to get and transform XMLStreamWriter message content
        XMLStreamWriter xWriter = message.getContent(XMLStreamWriter.class);
        if (xWriter != null) {
            transformXWriter(message, xWriter);
        } else {
            // 2. Try to get and transform OutputStream message content
            OutputStream out = message.getContent(OutputStream.class);
            if (out != null) {
                transformOS(message, out);
            } else {
                // 3. Try to get and transform Writer message content (actually used for JMS TextMessage)
                Writer writer = message.getContent(Writer.class);
                if (writer != null) {
                    transformWriter(message, writer);
                }
            }
        }
    }

    protected void transformXWriter(Message message, XMLStreamWriter xWriter) {
        CachedWriter writer = new CachedWriter();
        XMLStreamWriter delegate = StaxUtils.createXMLStreamWriter(writer);
        XSLTStreamWriter wrapper = new XSLTStreamWriter(getXSLTTemplate(), writer, delegate, xWriter);
        message.setContent(XMLStreamWriter.class, wrapper);
        message.put(AbstractOutDatabindingInterceptor.DISABLE_OUTPUTSTREAM_OPTIMIZATION,
                    Boolean.TRUE);
    }

    protected void transformOS(Message message, OutputStream out) {
        CachedOutputStream wrapper = new CachedOutputStream();
        CachedOutputStreamCallback callback = new XSLTCachedOutputStreamCallback(getXSLTTemplate(), out);
        wrapper.registerCallback(callback);
        message.setContent(OutputStream.class, wrapper);
    }

    protected void transformWriter(Message message, Writer writer) {
        XSLTCachedWriter wrapper = new XSLTCachedWriter(getXSLTTemplate(), writer);
        message.setContent(Writer.class, wrapper);
    }


    public static class XSLTStreamWriter extends DelegatingXMLStreamWriter {
        private final Templates xsltTemplate;
        private final CachedWriter cachedWriter;
        private final XMLStreamWriter origXWriter;

        public XSLTStreamWriter(Templates xsltTemplate, CachedWriter cachedWriter,
                                XMLStreamWriter delegateXWriter, XMLStreamWriter origXWriter) {
            super(delegateXWriter);
            this.xsltTemplate = xsltTemplate;
            this.cachedWriter = cachedWriter;
            this.origXWriter = origXWriter;
        }

        @Override
        public void close() {
            try {
                super.flush();
                try (Reader transformedReader = XSLTUtils.transform(xsltTemplate, cachedWriter.getReader())) {
                    StaxUtils.copy(new StreamSource(transformedReader), origXWriter);                    
                }
            } catch (XMLStreamException e) {
                throw new Fault("STAX_COPY", LOG, e, e.getMessage());
            } catch (IOException e) {
                throw new Fault("GET_CACHED_INPUT_STREAM", LOG, e, e.getMessage());
            } finally {
                try {
                    cachedWriter.close();
                    StaxUtils.close(origXWriter);
                    super.close();
                } catch (Exception e) {
                    LOG.warning("Cannot close stream after transformation: " + e.getMessage());
                }
            }
        }
    }

    public static class XSLTCachedOutputStreamCallback implements CachedOutputStreamCallback {
        private final Templates xsltTemplate;
        private final OutputStream origStream;

        public XSLTCachedOutputStreamCallback(Templates xsltTemplate, OutputStream origStream) {
            this.xsltTemplate = xsltTemplate;
            this.origStream = origStream;
        }

        @Override
        public void onFlush(CachedOutputStream wrapper) {
        }

        @Override
        public void onClose(CachedOutputStream wrapper) {
            InputStream transformedStream;
            Exception exceptionOnClose = null;
            try {
                transformedStream = XSLTUtils.transform(xsltTemplate, wrapper.getInputStream());
                IOUtils.copyAndCloseInput(transformedStream, origStream);
            } catch (IOException e) {
                throw new Fault("STREAM_COPY", LOG, e, e.getMessage());
            } finally {
                try {
                    origStream.close();
                } catch (Exception e) {
                    exceptionOnClose = e;
                }
            }
            
            if (exceptionOnClose == null) {
                return;
            }
            throw new Fault(exceptionOnClose);
        }
    }

    public static class XSLTCachedWriter extends CachedWriter {
        private final Templates xsltTemplate;
        private final Writer origWriter;

        public XSLTCachedWriter(Templates xsltTemplate, Writer origWriter) {
            this.xsltTemplate = xsltTemplate;
            this.origWriter = origWriter;
        }

        @Override
        protected void doClose() {
            try {
                final Reader transformedReader = XSLTUtils.transform(xsltTemplate, getReader());
                IOUtils.copyAndCloseInput(transformedReader, origWriter, IOUtils.DEFAULT_BUFFER_SIZE);
            } catch (IOException e) {
                throw new Fault("READER_COPY", LOG, e, e.getMessage());
            } finally {
                try {
                    origWriter.close();
                } catch (IOException e) {
                    LOG.warning("Cannot close stream after transformation: " + e.getMessage());
                }
            }
        }
    }

}
