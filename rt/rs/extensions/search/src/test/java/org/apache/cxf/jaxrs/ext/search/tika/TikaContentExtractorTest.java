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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

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
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.apache.tika.parser.pdf.PDFParser;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TikaContentExtractorTest extends Assert {
    private TikaContentExtractor extractor;
    private Directory directory;
    private IndexWriter writer;
    private SearchConditionParser< SearchBean > parser;
    
    @Before
    public void setUp() throws Exception {
        final Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_40);
        directory = new RAMDirectory();
        
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_40, analyzer);
        writer = new IndexWriter(directory, config);    
        writer.commit();
        
        parser = new FiqlParser<SearchBean>(SearchBean.class);
        extractor = new TikaContentExtractor(new PDFParser());
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

    @Test
    public void testExtractionFromNullFileFails() throws FileNotFoundException {
        assertNull("Document should be null, it is encrypted", extractor.extract((File)null));        
    }
    
    @Test(expected = FileNotFoundException.class)
    public void testExtractionFromNonExistingFileFails() throws FileNotFoundException {
        assertNull("Document should be null, it is encrypted", 
            extractor.extract(new File("a.txt")));        
    }

    private ScoreDoc[] getHits(final String expression) throws IOException {
        IndexReader reader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(reader);        

        try {
            LuceneQueryVisitor<SearchBean> visitor = new LuceneQueryVisitor<SearchBean>("ct", "contents");
            visitor.visit(parser.parse(expression));
    
            ScoreDoc[] hits = searcher.search(visitor.getQuery(), null, 1000).scoreDocs;
            assertNotNull(hits);
            
            return hits;            
        } finally {
            reader.close();
        }
    }
    
    @After
    public void tearDown() throws Exception {
        writer.close();        
        directory.close();
    }
}
