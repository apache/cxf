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
package org.apache.cxf.io;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;

public class CachedWriterTest extends CachedStreamTestBase {
    @Override
    protected void reloadDefaultProperties() {
        CachedWriter.setDefaultThreshold(-1);
        CachedWriter.setDefaultMaxSize(-1);
        CachedWriter.setDefaultCipherTransformation(null);
    }

    @Override
    protected Object createCache() {
        return new CachedWriter();
    }

    @Override
    protected Object createCache(long threshold) {
        return createCache(threshold, null);
    }

    @Override
    protected Object createCache(long threshold, String transformation) {
        CachedWriter cos = new CachedWriter();
        cos.setThreshold(threshold);
        cos.setCipherTransformation(transformation);
        return cos;
    }

    @Override
    protected String getResetOutValue(String result, Object cache) throws IOException {
        CachedWriter cos = (CachedWriter)cache;
        cos.write(result);
        StringWriter out = new StringWriter();
        cos.resetOut(out, true);
        return out.toString();
    }

    @Override
    protected File getTmpFile(String result, Object cache) throws IOException {
        CachedWriter cos = (CachedWriter)cache;
        cos.write(result);
        cos.flush();
        cos.getOut().close();
        return cos.getTempFile();
    }

    @Override
    protected Object getInputStreamObject(Object cache) throws IOException {
        return ((CachedWriter)cache).getReader();
    }

    @Override
    protected String readFromStreamObject(Object obj) throws IOException {
        return readFromReader((Reader)obj);
    }

    @Override
    protected String readPartiallyFromStreamObject(Object cache, int len) throws IOException {
        return readPartiallyFromReader((Reader)cache, len);
    }
}


