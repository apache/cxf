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
package org.apache.cxf.jaxrs.ext.search.tika;

import java.io.InputStream;

import org.apache.cxf.jaxrs.ext.search.SearchBean;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchConditionParser;
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;
import org.apache.tika.parser.pdf.PDFParser;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TikaContentExtractorTest {
    private TikaContentExtractor extractor;
    private SearchConditionParser< SearchBean > parser;

    @Before
    public void setUp() throws Exception {
        parser = new FiqlParser<>(SearchBean.class);
        extractor = new TikaContentExtractor(new PDFParser());
    }

    @Test
    public void testExtractedTextContentMatchesSearchCriteria() throws Exception {
        SearchCondition<SearchBean> sc = parser.parse("dc:creator==Bertrand*");
        final SearchBean bean = extractor.extractMetadataToSearchBean(
            getClass().getResourceAsStream("/files/testPDF.pdf"));
        assertNotNull("Document should not be null", bean);
        assertTrue(sc.isMet(bean));
    }
    @Test
    public void testExtractedTextContentDoesNotMatchSearchCriteria() throws Exception {
        SearchCondition<SearchBean> sc = parser.parse("dc:creator==Barry*");
        final SearchBean bean = extractor.extractMetadataToSearchBean(
            getClass().getResourceAsStream("/files/testPDF.pdf"));
        assertNotNull("Document should not be null", bean);
        assertFalse(sc.isMet(bean));
    }

    @Test
    public void testExtractionFromTextFileUsingPdfParserFails() {
        assertNull("Document should be null, it is not a PDF",
            extractor.extract(getClass().getResourceAsStream("/files/testTXT.txt")));
    }

    @Test
    public void testExtractionFromRtfFileUsingPdfParserWithoutMediaTypeValidationFails() {
        final TikaContentExtractor another = new TikaContentExtractor(new PDFParser(), false);
        assertNull("Document should be null, it is not a PDF",
            another.extract(getClass().getResourceAsStream("/files/testRTF.rtf")));
    }

    @Test
    public void testExtractionFromEncryptedPdfFails() {
        assertNull("Document should be null, it is encrypted",
            extractor.extract(getClass().getResourceAsStream("/files/testPDF.Encrypted.pdf")));
    }

    @Test
    public void testExtractionFromNullInputStreamFails() {
        assertNull("Document should be null, it is encrypted", extractor.extract((InputStream)null));
    }
}