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
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.helpers.IOUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public abstract class CachedStreamTestBase {
    // use two typical ciphers for testing
    private static final String[] CIPHER_LIST = {"RC4", "AES/CTR/NoPadding"};

    protected abstract void reloadDefaultProperties();
    protected abstract Object createCache();
    protected abstract Object createCache(long threshold);
    protected abstract Object createCache(long threshold, String transformation);
    protected abstract String getResetOutValue(String result, Object cache) throws IOException;
    protected abstract File getTmpFile(String result, Object cache) throws IOException;
    protected abstract Object getInputStreamObject(Object cache) throws IOException;
    protected abstract String readFromStreamObject(Object cache) throws IOException;
    protected abstract String readPartiallyFromStreamObject(Object cache, int len) throws IOException;

    @Test
    public void testResetOut() throws IOException {
        String result = initTestData(16);
        Object cache = createCache();
        String test = getResetOutValue(result, cache);
        assertEquals("The test stream content isn't same ", test, result);
        close(cache);
    }

    @Test
    public void testDeleteTmpFile() throws IOException {
        Object cache = createCache(64 * 1024);
        //ensure output data size larger then 64k which will generate tmp file
        String result = initTestData(65);
        File tempFile = getTmpFile(result, cache);
        assertNotNull(tempFile);
        //assert tmp file is generated
        assertTrue(tempFile.exists());
        close(cache);
        //assert tmp file is deleted after close the CachedOutputStream
        assertFalse(tempFile.exists());
    }

    @Test
    public void testDeleteTmpFile2() throws IOException {
        Object cache = createCache();
        //ensure output data size larger then 128k which will generate tmp file
        String result = initTestData(130);
        File tempFile = getTmpFile(result, cache);
        assertNotNull(tempFile);
        //assert tmp file is generated
        assertTrue(tempFile.exists());
        Object in = getInputStreamObject(cache);
        close(cache);
        //assert tmp file is not deleted when the input stream is open
        assertTrue(tempFile.exists());
        close(in);
        //assert tmp file is deleted after the input stream is closed
        assertFalse(tempFile.exists());
    }

    @Test
    public void testEncryptAndDecryptWithDeleteOnClose() throws IOException {
        // need a 8-bit cipher so that all bytes are flushed when the stream is flushed.
        for (String cipher: CIPHER_LIST) {
            verifyEncryptAndDecryptWithDeleteOnClose(cipher);
        }
    }

    private void verifyEncryptAndDecryptWithDeleteOnClose(String cipher) throws IOException {
        Object cache = createCache(4, cipher);
        final String text = "Hello Secret World!";
        File tmpfile = getTmpFile(text, cache);
        assertNotNull(tmpfile);

        final String enctext = readFromStream(new FileInputStream(tmpfile));
        assertNotEquals("text is not encoded", enctext, text);

        Object fin = getInputStreamObject(cache);

        assertTrue("file is deleted", tmpfile.exists());

        final String dectext = readFromStreamObject(fin);
        assertEquals("text is not decoded correctly", text, dectext);

        // the file is deleted when cos is closed while all the associated inputs are closed
        assertTrue("file is deleted", tmpfile.exists());
        close(cache);
        assertFalse("file is not deleted", tmpfile.exists());
    }

    @Test
    public void testEncryptAndDecryptWithDeleteOnInClose() throws IOException {
        for (String cipher: CIPHER_LIST) {
            verifyEncryptAndDecryptWithDeleteOnInClose(cipher);
        }
    }

    private void verifyEncryptAndDecryptWithDeleteOnInClose(String cipher) throws IOException {
        // need a 8-bit cipher so that all bytes are flushed when the stream is flushed.
        Object cache = createCache(4, cipher);
        final String text = "Hello Secret World!";
        File tmpfile = getTmpFile(text, cache);
        assertNotNull(tmpfile);

        final String enctext = readFromStream(new FileInputStream(tmpfile));
        assertNotEquals("text is not encoded", enctext, text);

        Object fin = getInputStreamObject(cache);

        close(cache);
        assertTrue("file is deleted", tmpfile.exists());

        // the file is deleted when cos is closed while all the associated inputs are closed
        final String dectext = readFromStreamObject(fin);
        assertEquals("text is not decoded correctly", text, dectext);
        assertFalse("file is not deleted", tmpfile.exists());
    }

    @Test
    public void testEncryptAndDecryptPartially() throws IOException {
        for (String cipher: CIPHER_LIST) {
            verifyEncryptAndDecryptPartially(cipher);
        }
    }

    private void verifyEncryptAndDecryptPartially(String cipher) throws IOException {
        // need a 8-bit cipher so that all bytes are flushed when the stream is flushed.
        Object cache = createCache(4, cipher);
        final String text = "Hello Secret World!";
        File tmpfile = getTmpFile(text, cache);
        assertNotNull(tmpfile);

        Object fin = getInputStreamObject(cache);
        // read partially and keep the stream open
        String pdectext = readPartiallyFromStreamObject(fin, 4);
        assertTrue("text is not decoded correctly", text.startsWith(pdectext));

        Object fin2 = getInputStreamObject(cache);

        final String dectext = readFromStreamObject(fin2);
        assertEquals("text is not decoded correctly", text, dectext);

        // close the partially read stream
        if (fin instanceof Closeable) {
            ((Closeable)fin).close();
        }

        // the file is deleted when cos is closed while all the associated inputs are closed
        assertTrue("file is deleted", tmpfile.exists());
        close(cache);
        assertFalse("file is not deleted", tmpfile.exists());
    }


    @Test
    public void testUseSysProps() throws Exception {
        String old = System.getProperty(CachedConstants.THRESHOLD_SYS_PROP);
        try {
            System.clearProperty(CachedConstants.THRESHOLD_SYS_PROP);
            reloadDefaultProperties();
            Object cache = createCache();
            File tmpfile = getTmpFile("Hello World!", cache);
            assertNull("expects no tmp file", tmpfile);
            close(cache);

            System.setProperty(CachedConstants.THRESHOLD_SYS_PROP, "4");
            reloadDefaultProperties();
            cache = createCache();
            tmpfile = getTmpFile("Hello World!", cache);
            assertNotNull("expects a tmp file", tmpfile);
            assertTrue("expects a tmp file", tmpfile.exists());
            close(cache);
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


    @Test
    public void testUseBusProps() throws Exception {
        Bus oldbus = BusFactory.getThreadDefaultBus(false);
        try {
            Object cache = createCache(64);
            File tmpfile = getTmpFile("Hello World!", cache);
            assertNull("expects no tmp file", tmpfile);
            close(cache);

            Bus b = mock(Bus.class);
            when(b.getProperty(CachedConstants.THRESHOLD_BUS_PROP)).thenReturn("4");
            when(b.getProperty(CachedConstants.MAX_SIZE_BUS_PROP)).thenReturn(null);
            when(b.getProperty(CachedConstants.CIPHER_TRANSFORMATION_BUS_PROP)).thenReturn(null);
            Path tmpDirPath = Files.createTempDirectory("temp-dir");
            when(b.getProperty(CachedConstants.OUTPUT_DIRECTORY_BUS_PROP)).thenReturn(tmpDirPath.toString());

            BusFactory.setThreadDefaultBus(b);

            cache = createCache();
            tmpfile = getTmpFile("Hello World!", cache);
            assertEquals(tmpfile.getParent(), tmpDirPath.toString());
            assertNotNull("expects a tmp file", tmpfile);
            assertTrue("expects a tmp file", tmpfile.exists());
            close(cache);
            assertFalse("expects no tmp file", tmpfile.exists());
            
            verify(b).getProperty(CachedConstants.THRESHOLD_BUS_PROP);
            verify(b).getProperty(CachedConstants.MAX_SIZE_BUS_PROP);
            verify(b).getProperty(CachedConstants.CIPHER_TRANSFORMATION_BUS_PROP);
            verify(b).getProperty(CachedConstants.OUTPUT_DIRECTORY_BUS_PROP);
        } finally {
            BusFactory.setThreadDefaultBus(oldbus);
        }
    }

    private static void close(Object obj) throws IOException {
        if (obj instanceof Closeable) {
            ((Closeable)obj).close();
        }
    }

    protected static String readFromStream(InputStream is) throws IOException {
        try (ByteArrayOutputStream buf = new ByteArrayOutputStream()) {
            IOUtils.copyAndCloseInput(is, buf);
            return new String(buf.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    protected static String readPartiallyFromStream(InputStream is, int len) throws IOException {
        try (ByteArrayOutputStream buf = new ByteArrayOutputStream()) {
            IOUtils.copyAtLeast(is, buf, len);
            return new String(buf.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    protected static String readFromReader(Reader is) throws IOException {
        try (StringWriter writer = new StringWriter()) {
            IOUtils.copyAndCloseInput(is, writer);
            return writer.toString();
        }
    }

    protected static String readPartiallyFromReader(Reader is, int len) throws IOException {
        try (StringWriter writer = new StringWriter()) {
            IOUtils.copyAtLeast(is, writer, len);
            return writer.toString();
        }
    }

    private static String initTestData(int packetSize) {
        String temp = "abcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+?><[]/0123456789";
        String result = new String();
        for (int i = 0; i <  1024 * packetSize / temp.length(); i++) {
            result = result + temp;
        }
        return result;
    }
}

