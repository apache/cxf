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

import org.junit.Assert;
import org.junit.Test;

public class AttachmentUtilTest extends Assert {

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

}
