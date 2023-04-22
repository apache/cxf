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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.cxf.jaxrs.ext.search.SearchBean;
import org.apache.cxf.jaxrs.ext.search.SearchConditionParser;
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;
import org.apache.cxf.jaxrs.ext.search.lucene.LuceneQueryVisitor;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.tika.parser.pdf.PDFParser;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TikaLuceneContentExtractorTest {
    private TikaLuceneContentExtractor extractor;
    private Directory directory;
    private IndexWriter writer;
    private SearchConditionParser< SearchBean > parser;
    private Path tempDirectory;

    @Before
    public void setUp() throws Exception {
        final Analyzer analyzer = new StandardAnalyzer();
        tempDirectory = Files.createTempDirectory("lucene");
        directory = new MMapDirectory(tempDirectory);

        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        writer = new IndexWriter(directory, config);
        writer.commit();

        parser = new FiqlParser<>(SearchBean.class);
        extractor = new TikaLuceneContentExtractor(new PDFParser());
    }

    @After
    public void tearDown() throws Exception {
        writer.close();
        directory.close();
        FileUtils.deleteQuietly(tempDirectory.toFile());
    }

    @Test
    public void testExtractedTextContentMatchesSearchCriteria() throws Exception {
        final Document document = extractor.extract(getClass().getResourceAsStream("/files/testPDF.pdf"));
        assertNotNull("Document should not be null", document);

        writer.addDocument(document);
        writer.commit();

        assertEquals(1, getHits("ct==tika").length);
        assertEquals(1, getHits("ct==incubation").length);
        assertEquals(0, getHits("ct==toolsuite").length);
        // meta-data
        assertEquals(1, getHits("dc:creator==Bertrand*").length);
    }

    @Test
    public void testExtractedTextContentMatchesTypesAndDateSearchCriteria() throws Exception {
        final LuceneDocumentMetadata documentMetadata = new LuceneDocumentMetadata("contents")
                .withField("modified", Date.class)
                .withField("dcterms:modified", Date.class);

        final Document document = extractor.extract(
            getClass().getResourceAsStream("/files/testPDF.pdf"), documentMetadata);
        assertNotNull("Document should not be null", document);

        writer.addDocument(document);
        writer.commit();
        // testPDF.pdf 'modified' is set to '2007-09-14T09:02:31Z'
        assertEquals(1, getHits("dcterms:modified=gt=2007-09-14T09:02:31Z", documentMetadata.getFieldTypes()).length);
        assertEquals(1, getHits("dcterms:modified=le=2007-09-15T09:02:31-0500",
                documentMetadata.getFieldTypes()).length);
        assertEquals(1, getHits("dcterms:modified=ge=2007-09-15", documentMetadata.getFieldTypes()).length);
        assertEquals(1, getHits("dcterms:modified==2007-09-15", documentMetadata.getFieldTypes()).length);
        assertEquals(0, getHits("dcterms:modified==2007-09-16", documentMetadata.getFieldTypes()).length);
        assertEquals(0, getHits("dcterms:modified=gt=2007-09-16", documentMetadata.getFieldTypes()).length);
        assertEquals(0, getHits("dcterms:modified=lt=2007-09-15", documentMetadata.getFieldTypes()).length);
        assertEquals(0, getHits("dcterms:modified=gt=2007-09-16T09:02:31", documentMetadata.getFieldTypes()).length);
        assertEquals(0, getHits("dcterms:modified=lt=2007-09-01T09:02:31", documentMetadata.getFieldTypes()).length);
    }

    @Test
    public void testExtractedTextContentMatchesTypesAndIntegerSearchCriteria() throws Exception {
        final LuceneDocumentMetadata documentMetadata = new LuceneDocumentMetadata("contents")
            .withField("xmpTPg:NPages", Integer.class);

        final Document document = extractor.extract(
            getClass().getResourceAsStream("/files/testPDF.pdf"), documentMetadata);
        assertNotNull("Document should not be null", document);

        writer.addDocument(document);
        writer.commit();

        assertEquals(1, getHits("xmpTPg:NPages=gt=0", documentMetadata.getFieldTypes()).length);
        assertEquals(1, getHits("xmpTPg:NPages==1", documentMetadata.getFieldTypes()).length);
        assertEquals(1, getHits("xmpTPg:NPages=ge=1", documentMetadata.getFieldTypes()).length);
        assertEquals(0, getHits("xmpTPg:NPages=gt=1", documentMetadata.getFieldTypes()).length);
        assertEquals(0, getHits("xmpTPg:NPages=lt=1", documentMetadata.getFieldTypes()).length);
    }

    @Test
    public void testExtractedTextContentMatchesTypesAndByteSearchCriteria() throws Exception {
        final LuceneDocumentMetadata documentMetadata = new LuceneDocumentMetadata("contents")
            .withField("xmpTPg:NPages", Byte.class);

        final Document document = extractor.extract(
            getClass().getResourceAsStream("/files/testPDF.pdf"), documentMetadata);
        assertNotNull("Document should not be null", document);

        writer.addDocument(document);
        writer.commit();

        assertEquals(1, getHits("xmpTPg:NPages=gt=0", documentMetadata.getFieldTypes()).length);
        assertEquals(1, getHits("xmpTPg:NPages==1", documentMetadata.getFieldTypes()).length);
        assertEquals(1, getHits("xmpTPg:NPages=ge=1", documentMetadata.getFieldTypes()).length);
        assertEquals(0, getHits("xmpTPg:NPages=gt=1", documentMetadata.getFieldTypes()).length);
        assertEquals(0, getHits("xmpTPg:NPages=lt=1", documentMetadata.getFieldTypes()).length);
    }

    @Test
    public void testExtractedTextContentMatchesTypesAndLongSearchCriteria() throws Exception {
        final LuceneDocumentMetadata documentMetadata = new LuceneDocumentMetadata("contents")
            .withField("xmpTPg:NPages", Long.class);

        final Document document = extractor.extract(
            getClass().getResourceAsStream("/files/testPDF.pdf"), documentMetadata);
        assertNotNull("Document should not be null", document);

        writer.addDocument(document);
        writer.commit();

        assertEquals(1, getHits("xmpTPg:NPages=gt=0", documentMetadata.getFieldTypes()).length);
        assertEquals(1, getHits("xmpTPg:NPages==1", documentMetadata.getFieldTypes()).length);
        assertEquals(1, getHits("xmpTPg:NPages=ge=1", documentMetadata.getFieldTypes()).length);
        assertEquals(0, getHits("xmpTPg:NPages=gt=1", documentMetadata.getFieldTypes()).length);
        assertEquals(0, getHits("xmpTPg:NPages=lt=1", documentMetadata.getFieldTypes()).length);
    }

    @Test
    public void testExtractedTextContentMatchesTypesAndDoubleSearchCriteria() throws Exception {
        final LuceneDocumentMetadata documentMetadata = new LuceneDocumentMetadata("contents")
            .withField("xmpTPg:NPages", Double.class);

        final Document document = extractor.extract(
            getClass().getResourceAsStream("/files/testPDF.pdf"), documentMetadata);
        assertNotNull("Document should not be null", document);

        writer.addDocument(document);
        writer.commit();

        assertEquals(1, getHits("xmpTPg:NPages=gt=0.0", documentMetadata.getFieldTypes()).length);
        assertEquals(1, getHits("xmpTPg:NPages==1.0", documentMetadata.getFieldTypes()).length);
        assertEquals(1, getHits("xmpTPg:NPages=ge=1.0", documentMetadata.getFieldTypes()).length);
        assertEquals(0, getHits("xmpTPg:NPages=gt=1.0", documentMetadata.getFieldTypes()).length);
        assertEquals(0, getHits("xmpTPg:NPages=lt=1.0", documentMetadata.getFieldTypes()).length);
    }

    @Test
    public void testExtractedTextContentMatchesTypesAndFloatSearchCriteria() throws Exception {
        final LuceneDocumentMetadata documentMetadata = new LuceneDocumentMetadata("contents")
            .withField("xmpTPg:NPages", Float.class);

        final Document document = extractor.extract(
            getClass().getResourceAsStream("/files/testPDF.pdf"), documentMetadata);
        assertNotNull("Document should not be null", document);

        writer.addDocument(document);
        writer.commit();

        assertEquals(1, getHits("xmpTPg:NPages=gt=0.0", documentMetadata.getFieldTypes()).length);
        assertEquals(1, getHits("xmpTPg:NPages==1.0", documentMetadata.getFieldTypes()).length);
        assertEquals(1, getHits("xmpTPg:NPages=ge=1.0", documentMetadata.getFieldTypes()).length);
        assertEquals(0, getHits("xmpTPg:NPages=gt=1.0", documentMetadata.getFieldTypes()).length);
        assertEquals(0, getHits("xmpTPg:NPages=lt=1.0", documentMetadata.getFieldTypes()).length);
    }

    @Test
    public void testContentSourceMatchesSearchCriteria() throws Exception {
        final LuceneDocumentMetadata documentMetadata = new LuceneDocumentMetadata()
            .withSource("testPDF.pdf");

        final Document document = extractor.extract(
            getClass().getResourceAsStream("/files/testPDF.pdf"), documentMetadata);
        assertNotNull("Document should not be null", document);

        writer.addDocument(document);
        writer.commit();

        // Should work by exact match only
        assertEquals(1, getHits("source==testPDF.pdf").length);
        assertEquals(0, getHits("source==testPDF").length);
    }

    private ScoreDoc[] getHits(final String expression) throws IOException {
        return getHits(expression, new HashMap<String, Class<?>>());
    }

    private ScoreDoc[] getHits(final String expression, final Map< String, Class<?> > fieldTypes) throws IOException {

        try (IndexReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            LuceneQueryVisitor<SearchBean> visitor = new LuceneQueryVisitor<>("ct", "contents");
            visitor.setPrimitiveFieldTypeMap(fieldTypes);
            visitor.visit(parser.parse(expression));

            ScoreDoc[] hits = searcher.search(visitor.getQuery(), 1000).scoreDocs;
            assertNotNull(hits);

            return hits;
        }
    }

}