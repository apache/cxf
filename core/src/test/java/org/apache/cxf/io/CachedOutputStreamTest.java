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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.cxf.attachment.AttachmentUtil;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CachedOutputStreamTest extends CachedStreamTestBase {

    @Override
    protected void reloadDefaultProperties() {
        CachedOutputStream.setDefaultThreshold(-1);
        CachedOutputStream.setDefaultMaxSize(-1);
        CachedOutputStream.setDefaultCipherTransformation(null);
    }

    @Override
    protected Object createCache() {
        return new CachedOutputStream();
    }

    @Override
    protected Object createCache(long threshold) {
        return createCache(threshold, null);
    }

    @Override
    protected Object createCache(long threshold, String transformation) {
        CachedOutputStream cos = new CachedOutputStream();
        cos.setThreshold(threshold);
        cos.setCipherTransformation(transformation);
        return cos;
    }

    @Override
    protected String getResetOutValue(String result, Object cache) throws IOException {
        CachedOutputStream cos = (CachedOutputStream)cache;
        cos.write(result.getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        cos.resetOut(out, true);
        return out.toString();
    }

    @Override
    protected File getTmpFile(String result, Object cache) throws IOException {
        CachedOutputStream cos = (CachedOutputStream)cache;
        cos.write(result.getBytes(StandardCharsets.UTF_8));
        cos.flush();
        cos.getOut().close();
        return cos.getTempFile();
    }

    @Override
    protected Object getInputStreamObject(Object cache) throws IOException {
        return ((CachedOutputStream)cache).getInputStream();
    }

    @Override
    protected String readFromStreamObject(Object obj) throws IOException {
        return readFromStream((InputStream)obj);
    }

    @Override
    protected String readPartiallyFromStreamObject(Object cache, int len) throws IOException {
        return readPartiallyFromStream((InputStream)cache, len);
    }

    @Test
    public void testUseSysPropsWithAttachmentDeserializer() throws Exception {
        String old = System.getProperty(CachedConstants.THRESHOLD_SYS_PROP);
        try {
            System.clearProperty(CachedConstants.THRESHOLD_SYS_PROP);
            reloadDefaultProperties();
            CachedOutputStream cache = new CachedOutputStream();

            Message message = new MessageImpl();
            AttachmentUtil.setStreamedAttachmentProperties(message, cache);

            File tmpfile = getTmpFile("Hello World!", cache);
            assertNull("expects no tmp file", tmpfile);
            cache.close();

            System.setProperty(CachedConstants.THRESHOLD_SYS_PROP, "4");
            reloadDefaultProperties();
            cache = new CachedOutputStream();
            AttachmentUtil.setStreamedAttachmentProperties(message, cache);

            tmpfile = getTmpFile("Hello World!", cache);
            assertNotNull("expects a tmp file", tmpfile);
            assertTrue("expects a tmp file", tmpfile.exists());
            cache.close();
            assertFalse("expects no tmp file", tmpfile.exists());
        } finally {
            System.clearProperty(CachedConstants.THRESHOLD_SYS_PROP);
            if (old != null) {
                System.setProperty(CachedConstants.THRESHOLD_SYS_PROP, old);
            }
            // Always restore the default properties
            reloadDefaultProperties();
        }
    }
}

