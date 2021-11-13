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

package org.apache.cxf.transport.http.asyncclient.hc5;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.hc.core5.function.Supplier;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;

abstract class MutableHttpEntity implements HttpEntity {
    static final int OUTPUT_BUFFER_SIZE = 4096;

    private InputStream content;
    private String contentType;
    private String contentEncoding;
    private boolean chunked;
    private long length;
    
    MutableHttpEntity(final String contentType, final String contentEncoding, final boolean chunked) {
        this.contentType = contentType;
        this.contentEncoding = contentEncoding;
        this.chunked = chunked;
    }

    @Override
    public String getContentEncoding() {
        return contentEncoding;
    }

    @Override
    public InputStream getContent() throws IOException, UnsupportedOperationException {
        return content;
    }

    @Override
    public boolean isStreaming() {
        return false;
    }

    @Override
    public long getContentLength() {
        return length;
    }
    
    public void setContentLength(long l) {
        this.length = l;
    }
    
    public void setChunked(boolean chunked) {
        this.chunked = chunked;
    }
    
    @Override
    public boolean isChunked() {
        return chunked;
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public Supplier<List<? extends Header>> getTrailers() {
        return null;
    }

    @Override
    public Set<String> getTrailerNames() {
        return Collections.emptySet();
    }
    
    @Override
    public String getContentType() {
        return contentType;
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    public static void writeTo(final HttpEntity entity, final OutputStream outStream) throws IOException {
        try (InputStream inStream = entity.getContent()) {
            if (inStream != null) {
                int count;
                final byte[] tmp = new byte[OUTPUT_BUFFER_SIZE];
                while ((count = inStream.read(tmp)) != -1) {
                    outStream.write(tmp, 0, count);
                }
            }
        }
    }

    @Override
    public void writeTo(final OutputStream outStream) throws IOException {
        writeTo(this, outStream);
    }
}
