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
package org.apache.cxf.attachment;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;

import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.Test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class AttachmentUtilTest {

    @Test
    public void testContendDispositionFileNameNoQuotes() {
        assertEquals("a.txt",
                     AttachmentUtil.getContentDispositionFileName("form-data; filename=a.txt"));
    }

    @Test
    public void testContendDispositionFileNameNoQuotesAndType() {
        assertEquals("a.txt",
                     AttachmentUtil.getContentDispositionFileName("filename=a.txt"));
    }

    @Test
    public void testContendDispositionFileNameNoQuotesAndType2() {
        assertEquals("a.txt",
                     AttachmentUtil.getContentDispositionFileName("name=files; filename=a.txt"));
    }

    @Test
    public void testContendDispositionFileNameSpacesNoQuotes() {
        assertEquals("a.txt",
                     AttachmentUtil.getContentDispositionFileName("form-data; filename = a.txt"));
    }

    @Test
    public void testContendDispositionFileNameWithQuotes() {
        assertEquals("a.txt",
                     AttachmentUtil.getContentDispositionFileName("form-data; filename=\"a.txt\""));
    }

    @Test
    public void testContendDispositionFileNameWithQuotesAndSemicolon() {
        assertEquals("a;txt",
                     AttachmentUtil.getContentDispositionFileName("form-data; filename=\"a;txt\""));
    }

    @Test
    public void testContendDispositionFileNameWithQuotesAndSemicolon2() {
        assertEquals("a;txt",
                     AttachmentUtil.getContentDispositionFileName("filename=\"a;txt\""));
    }

    @Test
    public void testContendDispositionFileNameWithQuotesAndSemicolon3() {
        assertEquals("a;txt",
                     AttachmentUtil.getContentDispositionFileName("name=\"a\";filename=\"a;txt\""));
    }

    @Test
    public void testContentDispositionAsterickMode() {
        assertEquals("a b.txt",
                   AttachmentUtil.getContentDispositionFileName("filename=\"bad.txt\"; filename*=UTF-8''a%20b.txt"));
    }

    @Test
    public void testContentDispositionAsterickModeLowercase() {
        assertEquals("a b.txt",
                   AttachmentUtil.getContentDispositionFileName("filename*=utf-8''a%20b.txt"));
    }

    @Test
    public void testContentDispositionAsterickModeFnUppercase() {
        assertEquals("a b.txt",
                   AttachmentUtil.getContentDispositionFileName("FILENAME*=utf-8''a%20b.txt"));
    }

    @Test
    public void testContentDispositionFnUppercase() {
        assertEquals("a b.txt",
                   AttachmentUtil.getContentDispositionFileName("FILENAME=\"a b.txt\""));
    }

    @Test
    public void testContendDispositionFileNameKanjiChars() {
        assertEquals("世界ーファイル.txt",
                AttachmentUtil.getContentDispositionFileName(
                        "filename*=UTF-8''%e4%b8%96%e7%95%8c%e3%83%bc%e3%83%95%e3%82%a1%e3%82%a4%e3%83%ab.txt"));
    }

    @Test
    public void testContendDispositionFileNameNoRfc5987() {
        assertEquals("демо-сервис.zip",
            AttachmentUtil.getContentDispositionFileName(
                "filename=\"&#1076;&#1077;&#1084;&#1086;-&#1089;&#1077;&#1088;&#1074;&#1080;&#1089;.zip\""));
    }

    @Test
    public void testContentDispositionFnEquals() {
        assertEquals("a=b.txt",
            AttachmentUtil.getContentDispositionFileName("filename=\"a=b.txt\""));
    }

    @Test
    public void testCreateContentID() throws Exception {
        assertNotEquals(AttachmentUtil.createContentID(null), AttachmentUtil.createContentID(null));
    }

    private CachedOutputStream testSetStreamedAttachmentProperties(final String property, final Object value)
            throws IOException {
        return testSetStreamedAttachmentProperties(property, value, new CachedOutputStream());
    }

    private CachedOutputStream testSetStreamedAttachmentProperties(final String property, final Object value,
            final CachedOutputStream cos) throws IOException {
        Message message = new MessageImpl();
        message.put(property, value);
        AttachmentUtil.setStreamedAttachmentProperties(message, cos);

        return cos;
    }

    @Test
    public void bigIntAsAttachmentMemoryThreshold() throws IOException {
        BigInteger bigInteger = new BigInteger(String.valueOf(Long.MAX_VALUE));
        try (CachedOutputStream cos = testSetStreamedAttachmentProperties(
                AttachmentDeserializer.ATTACHMENT_MEMORY_THRESHOLD, bigInteger)) {
            assertEquals(bigInteger.longValue(), cos.getThreshold());
        }
        // Overflow long value
        bigInteger = bigInteger.add(BigInteger.ONE);
        try (CachedOutputStream cos = testSetStreamedAttachmentProperties(
                AttachmentDeserializer.ATTACHMENT_MEMORY_THRESHOLD, bigInteger)) {
            assertEquals(AttachmentDeserializer.THRESHOLD, cos.getThreshold());
        }
    }

    @Test
    public void longAsAttachmentMemoryThreshold() throws IOException {
        try (CachedOutputStream cos = testSetStreamedAttachmentProperties(
                AttachmentDeserializer.ATTACHMENT_MEMORY_THRESHOLD, Long.MAX_VALUE)) {
            assertEquals(Long.MAX_VALUE, cos.getThreshold());
        }
    }

    @Test
    public void integerAsAttachmentMemoryThreshold() throws IOException {
        try (CachedOutputStream cos = testSetStreamedAttachmentProperties(
                AttachmentDeserializer.ATTACHMENT_MEMORY_THRESHOLD, Integer.MAX_VALUE)) {
            assertEquals(Integer.MAX_VALUE, cos.getThreshold());
        }
    }

    @Test
    public void shortAsAttachmentMemoryThreshold() throws IOException {
        try (CachedOutputStream cos = testSetStreamedAttachmentProperties(
                AttachmentDeserializer.ATTACHMENT_MEMORY_THRESHOLD, Short.MAX_VALUE)) {
            assertEquals(Short.MAX_VALUE, cos.getThreshold());
        }
    }

    @Test
    public void byteAsAttachmentMemoryThreshold() throws IOException {
        try (CachedOutputStream cos = testSetStreamedAttachmentProperties(
                AttachmentDeserializer.ATTACHMENT_MEMORY_THRESHOLD, Byte.MAX_VALUE)) {
            assertEquals(Byte.MAX_VALUE, cos.getThreshold());
        }
    }

    @Test
    public void numberStringAsAttachmentMemoryThreshold() throws IOException {
        try (CachedOutputStream cos = testSetStreamedAttachmentProperties(
                AttachmentDeserializer.ATTACHMENT_MEMORY_THRESHOLD, "12345")) {
            assertEquals(12345, cos.getThreshold());
        }
    }

    @Test(expected = IOException.class)
    public void nonNumberStringAsAttachmentMemoryThreshold() throws IOException {
        try (CachedOutputStream cos = testSetStreamedAttachmentProperties(
                AttachmentDeserializer.ATTACHMENT_MEMORY_THRESHOLD, "test")) {
            // Will throw exception
        }
    }

    @Test(expected = IOException.class)
    public void objectAsAttachmentMemoryThreshold() throws IOException {
        try (CachedOutputStream cos = testSetStreamedAttachmentProperties(
                AttachmentDeserializer.ATTACHMENT_MEMORY_THRESHOLD, new Object())) {
            // Will throw exception
        }
    }

    @Test
    public void bigIntAsAttachmentMaxSize() throws IOException {
        CachedOutputStream cos = createMock(CachedOutputStream.class);
        BigInteger bigInteger = new BigInteger(String.valueOf(Long.MAX_VALUE));
        cos = testSetStreamedAttachmentProperties(AttachmentDeserializer.ATTACHMENT_MAX_SIZE, bigInteger, cos);
        replay(cos);
        cos.setMaxSize(bigInteger.longValue());
        cos.setThreshold(102400L);
        verify(cos);
        // Overflow long value
        bigInteger = bigInteger.add(BigInteger.ONE);
        cos = createMock(CachedOutputStream.class);
        cos = testSetStreamedAttachmentProperties(AttachmentDeserializer.ATTACHMENT_MAX_SIZE, bigInteger, cos);
        replay(cos);
        cos.setThreshold(102400L);
        verify(cos);
    }

    @Test
    public void longAsAttachmentMaxSize() throws IOException {
        CachedOutputStream cos = createMock(CachedOutputStream.class);
        cos = testSetStreamedAttachmentProperties(AttachmentDeserializer.ATTACHMENT_MAX_SIZE, Long.MAX_VALUE, cos);
        replay(cos);
        cos.setMaxSize(Long.MAX_VALUE);
        cos.setThreshold(102400L);
        verify(cos);
    }

    @Test
    public void integerAsAttachmentMaxSize() throws IOException {
        CachedOutputStream cos = createMock(CachedOutputStream.class);
        cos = testSetStreamedAttachmentProperties(AttachmentDeserializer.ATTACHMENT_MAX_SIZE, Integer.MAX_VALUE, cos);
        replay(cos);
        cos.setMaxSize(Integer.MAX_VALUE);
        cos.setThreshold(102400L);
        verify(cos);
    }

    @Test
    public void shortAsAttachmentMaxSize() throws IOException {
        CachedOutputStream cos = createMock(CachedOutputStream.class);
        cos = testSetStreamedAttachmentProperties(AttachmentDeserializer.ATTACHMENT_MAX_SIZE, Short.MAX_VALUE, cos);
        replay(cos);
        cos.setMaxSize(Short.MAX_VALUE);
        cos.setThreshold(102400L);
        verify(cos);
    }

    @Test
    public void byteAsAttachmentMaxSize() throws IOException {
        CachedOutputStream cos = createMock(CachedOutputStream.class);
        cos = testSetStreamedAttachmentProperties(AttachmentDeserializer.ATTACHMENT_MAX_SIZE, Byte.MAX_VALUE, cos);
        replay(cos);
        cos.setMaxSize(Byte.MAX_VALUE);
        cos.setThreshold(102400L);
        verify(cos);
    }

    @Test
    public void numberStringAsAttachmentMaxSize() throws IOException {
        CachedOutputStream cos = createMock(CachedOutputStream.class);
        cos = testSetStreamedAttachmentProperties(AttachmentDeserializer.ATTACHMENT_MAX_SIZE, "12345", cos);
        replay(cos);
        cos.setMaxSize(12345);
        cos.setThreshold(102400L);
        verify(cos);
    }

    @Test(expected = IOException.class)
    public void nonNumberStringAsAttachmentMaxSize() throws IOException {
        try (CachedOutputStream cos = testSetStreamedAttachmentProperties(AttachmentDeserializer.ATTACHMENT_MAX_SIZE,
                "test")) {
            // Will throw exception
        }
    }

    @Test(expected = IOException.class)
    public void objectAsAttachmentMaxSize() throws IOException {
        try (CachedOutputStream cos = testSetStreamedAttachmentProperties(AttachmentDeserializer.ATTACHMENT_MAX_SIZE,
                new Object())) {
            // Will throw exception
        }
    }

    @Test
    public void fileAsAttachmentDirectory() throws IOException {
        File attachmentDirectory = new File("/dev/null");
        CachedOutputStream cos = createMock(CachedOutputStream.class);
        cos = testSetStreamedAttachmentProperties(AttachmentDeserializer.ATTACHMENT_DIRECTORY, attachmentDirectory,
                cos);
        replay(cos);
        cos.setOutputDir(attachmentDirectory);
        cos.setThreshold(102400L);
        verify(cos);
    }

    @Test
    public void stringAsAttachmentDirectory() throws IOException {
        String attachmentDirectory = "/dev/null";
        CachedOutputStream cos = createMock(CachedOutputStream.class);
        cos = testSetStreamedAttachmentProperties(AttachmentDeserializer.ATTACHMENT_DIRECTORY, attachmentDirectory,
                cos);
        replay(cos);
        cos.setOutputDir(new File(attachmentDirectory));
        cos.setThreshold(102400L);
        verify(cos);
    }

    @Test(expected = IOException.class)
    public void objectAsAttachmentDirectory() throws IOException {
        try (CachedOutputStream cos = testSetStreamedAttachmentProperties(AttachmentDeserializer.ATTACHMENT_DIRECTORY,
                new Object())) {
            // Will throw exception
        }
    }
}
