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
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

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


    @Test
    public void testCreateContentIDWithNullDomainNamePassed() throws UnsupportedEncodingException {
        String actual = AttachmentUtil.createContentID(null);
        assertThat(actual, endsWith("@cxf.apache.org"));
    }

    @Test
    public void testCreateContentIDWithDomainNamePassed() throws UnsupportedEncodingException {
        String domain = "subdomain.example.com";

        String actual = AttachmentUtil.createContentID(domain);

        assertThat(actual, endsWith("@" + domain));
    }

    @Test
    public void testCreateContentIDWithUrlPassed() throws UnsupportedEncodingException {
        String domain = "subdomain.example.com";
        String url = "https://" + domain + "/a/b/c";

        String actual = AttachmentUtil.createContentID(url);

        assertThat(actual, endsWith("@" + domain));
    }

    @Test
    public void testCreateContentIDWithIPv4BasedUrlPassed() throws UnsupportedEncodingException {
        String domain = "127.0.0.1";
        String url = "https://" + domain + "/a/b/c";

        String actual = AttachmentUtil.createContentID(url);

        assertThat(actual, endsWith("@" + domain));
    }

    @Test
    public void testCreateContentIDWithIPv6BasedUrlPassed() throws UnsupportedEncodingException {
        String domain = "[2001:0db8:11a3:09d7:1f34:8a2e:07a0:765d]";
        String url = "http://" + domain + "/a/b/c";

        String actual = AttachmentUtil.createContentID(url);
        assertThat(actual, endsWith("@" + URLEncoder.encode(domain, StandardCharsets.UTF_8)));
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
        CachedOutputStream cos = spy(CachedOutputStream.class);
        BigInteger bigInteger = new BigInteger(String.valueOf(Long.MAX_VALUE));
        cos = testSetStreamedAttachmentProperties(AttachmentDeserializer.ATTACHMENT_MAX_SIZE, bigInteger, cos);
        verify(cos).setMaxSize(bigInteger.longValue());
        verify(cos).setThreshold(102400L);

        // Overflow long value
        bigInteger = bigInteger.add(BigInteger.ONE);
        cos = spy(CachedOutputStream.class);
        cos = testSetStreamedAttachmentProperties(AttachmentDeserializer.ATTACHMENT_MAX_SIZE, bigInteger, cos);
        verify(cos).setThreshold(102400L);
    }

    @Test
    public void longAsAttachmentMaxSize() throws IOException {
        CachedOutputStream cos = spy(CachedOutputStream.class);
        cos = testSetStreamedAttachmentProperties(AttachmentDeserializer.ATTACHMENT_MAX_SIZE, Long.MAX_VALUE, cos);
        verify(cos).setMaxSize(Long.MAX_VALUE);
        verify(cos).setThreshold(102400L);
    }

    @Test
    public void integerAsAttachmentMaxSize() throws IOException {
        CachedOutputStream cos = spy(CachedOutputStream.class);
        cos = testSetStreamedAttachmentProperties(AttachmentDeserializer.ATTACHMENT_MAX_SIZE, Integer.MAX_VALUE, cos);
        verify(cos).setMaxSize(Integer.MAX_VALUE);
        verify(cos).setThreshold(102400L);
    }

    @Test
    public void shortAsAttachmentMaxSize() throws IOException {
        CachedOutputStream cos = spy(CachedOutputStream.class);
        cos = testSetStreamedAttachmentProperties(AttachmentDeserializer.ATTACHMENT_MAX_SIZE, Short.MAX_VALUE, cos);
        verify(cos).setMaxSize(Short.MAX_VALUE);
        verify(cos).setThreshold(102400L);
    }

    @Test
    public void byteAsAttachmentMaxSize() throws IOException {
        CachedOutputStream cos = spy(CachedOutputStream.class);
        cos = testSetStreamedAttachmentProperties(AttachmentDeserializer.ATTACHMENT_MAX_SIZE, Byte.MAX_VALUE, cos);
        verify(cos).setMaxSize(Byte.MAX_VALUE);
        verify(cos).setThreshold(102400L);
    }

    @Test
    public void numberStringAsAttachmentMaxSize() throws IOException {
        CachedOutputStream cos = spy(CachedOutputStream.class);
        cos = testSetStreamedAttachmentProperties(AttachmentDeserializer.ATTACHMENT_MAX_SIZE, "12345", cos);
        verify(cos).setMaxSize(12345);
        verify(cos).setThreshold(102400L);
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
        CachedOutputStream cos = spy(CachedOutputStream.class);
        cos = testSetStreamedAttachmentProperties(AttachmentDeserializer.ATTACHMENT_DIRECTORY, attachmentDirectory,
                cos);
        verify(cos).setOutputDir(attachmentDirectory);
        verify(cos).setThreshold(102400L);
    }

    @Test
    public void stringAsAttachmentDirectory() throws IOException {
        String attachmentDirectory = "/dev/null";
        CachedOutputStream cos = spy(CachedOutputStream.class);
        cos = testSetStreamedAttachmentProperties(AttachmentDeserializer.ATTACHMENT_DIRECTORY, attachmentDirectory,
                cos);
        verify(cos).setOutputDir(new File(attachmentDirectory));
        verify(cos).setThreshold(102400L);
    }

    @Test(expected = IOException.class)
    public void objectAsAttachmentDirectory() throws IOException {
        try (CachedOutputStream cos = testSetStreamedAttachmentProperties(AttachmentDeserializer.ATTACHMENT_DIRECTORY,
                new Object())) {
            // Will throw exception
        }
    }
}
