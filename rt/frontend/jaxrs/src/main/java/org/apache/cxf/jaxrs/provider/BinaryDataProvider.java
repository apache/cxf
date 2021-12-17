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

package org.apache.cxf.jaxrs.provider;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.util.UUID;
import java.util.logging.Logger;

import jakarta.servlet.WriteListener;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import org.apache.cxf.annotations.UseNio;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.MessageDigestInputStream;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.continuations.Continuation;
import org.apache.cxf.continuations.ContinuationProvider;
import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.impl.HttpHeadersImpl;
import org.apache.cxf.jaxrs.nio.NioOutputStream;
import org.apache.cxf.jaxrs.nio.NioWriteEntity;
import org.apache.cxf.jaxrs.nio.NioWriteHandler;
import org.apache.cxf.jaxrs.nio.NioWriteListenerImpl;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;

public class BinaryDataProvider<T> extends AbstractConfigurableProvider
    implements MessageBodyReader<T>, MessageBodyWriter<T> {

    private static final String HTTP_RANGE_PROPERTY = "http.range.support";
    private static final Logger LOG = LogUtils.getL7dLogger(BinaryDataProvider.class);

    private int bufferSize = IOUtils.DEFAULT_BUFFER_SIZE;
    private boolean reportByteArraySize;
    private boolean closeResponseInputStream = true;
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        return byte[].class.isAssignableFrom(type)
               || InputStream.class.isAssignableFrom(type)
               || Reader.class.isAssignableFrom(type)
               || File.class.isAssignableFrom(type)
               || StreamingOutput.class.isAssignableFrom(type);
    }

    private static final class ReadingStreamingOutput implements StreamingOutput {
        private final InputStream inputStream;
        private ReadingStreamingOutput(InputStream inputStream) {
            this.inputStream = inputStream;
        }
        public void write(OutputStream outputStream) throws IOException {
            IOUtils.copy(inputStream, outputStream);
        }
    }

    public T readFrom(Class<T> clazz, Type genericType, Annotation[] annotations, MediaType type,
                           MultivaluedMap<String, String> headers, InputStream is)
        throws IOException {
        try {
            if (InputStream.class.isAssignableFrom(clazz)) {
                if (DigestInputStream.class.isAssignableFrom(clazz)) {
                    is = new MessageDigestInputStream(is);
                }
                return clazz.cast(is);
            }
            if (Reader.class.isAssignableFrom(clazz)) {
                return clazz.cast(new InputStreamReader(is, getEncoding(type)));
            }
            if (byte[].class.isAssignableFrom(clazz)) {
                return clazz.cast(IOUtils.readBytesFromStream(is));
            }
            if (File.class.isAssignableFrom(clazz)) {
                LOG.warning("Reading data into File objects with the help of pre-packaged"
                    + " providers is not recommended - use InputStream or custom File reader");
                // create a temp file, delete on exit
                File f = FileUtils.createTempFile("File" + UUID.randomUUID().toString(),
                                                  "jaxrs",
                                                  null,
                                                  true);
                OutputStream os = Files.newOutputStream(f.toPath());
                IOUtils.copy(is, os, bufferSize);
                os.close();
                return clazz.cast(f);
            }
            if (StreamingOutput.class.isAssignableFrom(clazz)) {
                return clazz.cast(new ReadingStreamingOutput(is));
            }
        } catch (ClassCastException e) {
            String msg = "Unsupported class: " + clazz.getName();
            LOG.warning(msg);
            throw ExceptionUtils.toInternalServerErrorException(null, null);
        }
        throw new IOException("Unrecognized class");
    }



    public long getSize(T t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        // TODO: if it's a range request, then we should probably always return -1 and set
        // Content-Length and Content-Range in handleRangeRequest
        if (reportByteArraySize && byte[].class.isAssignableFrom(t.getClass())) {
            return ((byte[])t).length;
        }
        return -1;
    }

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        return byte[].class.isAssignableFrom(type)
            || InputStream.class.isAssignableFrom(type)
            || File.class.isAssignableFrom(type)
            || Reader.class.isAssignableFrom(type)
            || StreamingOutput.class.isAssignableFrom(type);
    }

    public void writeTo(T o, Class<?> clazz, Type genericType, Annotation[] annotations,
                        MediaType type, MultivaluedMap<String, Object> headers, OutputStream os)
        throws IOException {

        if (InputStream.class.isAssignableFrom(o.getClass())) {
            copyInputToOutput((InputStream)o, os, annotations, headers);
        } else if (File.class.isAssignableFrom(o.getClass())) {
            copyInputToOutput(new BufferedInputStream(
                    Files.newInputStream(((File)o).toPath())), os, annotations, headers);
        } else if (byte[].class.isAssignableFrom(o.getClass())) {
            copyInputToOutput(new ByteArrayInputStream((byte[])o), os, annotations, headers);
        } else if (Reader.class.isAssignableFrom(o.getClass())) {
            try {
                Writer writer = new OutputStreamWriter(os, getEncoding(type));
                IOUtils.copy((Reader)o,
                              writer,
                              bufferSize);
                writer.flush();
            } finally {
                ((Reader)o).close();
            }

        } else if (StreamingOutput.class.isAssignableFrom(o.getClass())) {
            ((StreamingOutput)o).write(os);
        } else {
            throw new IOException("Unrecognized class");
        }

    }

    private String getEncoding(MediaType mt) {
        String enc = mt.getParameters().get("charset");
        return enc == null ? StandardCharsets.UTF_8.name() : enc;
    }
    

    protected void copyInputToOutput(InputStream is, OutputStream os,
            Annotation[] anns, MultivaluedMap<String, Object> outHeaders) throws IOException {
        if (isRangeSupported()) {
            Message inMessage = PhaseInterceptorChain.getCurrentMessage().getExchange().getInMessage();
            handleRangeRequest(is, os, new HttpHeadersImpl(inMessage), outHeaders);
        } else {
            boolean nioWrite = AnnotationUtils.getAnnotation(anns, UseNio.class) != null;
            if (nioWrite) {
                ContinuationProvider provider = getContinuationProvider();
                if (provider != null) {
                    copyUsingNio(is, os, provider.getContinuation());
                }
                return;
            }
            if (closeResponseInputStream) {
                IOUtils.copyAndCloseInput(is, os, bufferSize);
            } else {
                IOUtils.copy(is, os, bufferSize);
            }

        }
    }

    protected void copyUsingNio(InputStream is, OutputStream os, Continuation cont) {
        NioWriteListenerImpl listener =
            new NioWriteListenerImpl(cont,
                                     new NioWriteEntity(getNioHandler(is), null),
                                     new NioOutputStream(os));
        Message m = JAXRSUtils.getCurrentMessage();
        m.put(WriteListener.class, listener);
        cont.suspend(0);
    }

    private ContinuationProvider getContinuationProvider() {
        return (ContinuationProvider)JAXRSUtils.getCurrentMessage().getExchange()
            .getInMessage().get(ContinuationProvider.class.getName());
    }



    protected void handleRangeRequest(InputStream is,
                                      OutputStream os,
                                      HttpHeaders inHeaders,
                                      MultivaluedMap<String, Object> outHeaders) throws IOException {
        String range = inHeaders.getRequestHeaders().getFirst("Range");
        if (range == null) {
            IOUtils.copyAndCloseInput(is, os, bufferSize);
        } else {
            // implement
        }

    }

    protected boolean isRangeSupported() {
        Message message = PhaseInterceptorChain.getCurrentMessage();
        if (message != null) {
            return PropertyUtils.isTrue(message.get(HTTP_RANGE_PROPERTY));
        }
        return false;
    }

    public void setReportByteArraySize(boolean report) {
        this.reportByteArraySize = report;
    }

    public void setCloseResponseInputStream(boolean closeResponseInputStream) {
        this.closeResponseInputStream = closeResponseInputStream;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    protected NioWriteHandler getNioHandler(final InputStream in) {

        return new NioWriteHandler() {
            final byte[] buffer = new byte[bufferSize];
            @Override
            public boolean write(NioOutputStream out) {
                try {
                    final int n = in.read(buffer);

                    if (n >= 0) {
                        out.write(buffer, 0, n);
                        return true;
                    }
                    if (closeResponseInputStream) {
                        try {
                            in.close();
                        } catch (IOException ex) {
                            /* do nothing */
                        }
                    }

                    return false;
                } catch (IOException ex) {
                    throw new WebApplicationException(ex);
                }

            }
        };


    }
}
