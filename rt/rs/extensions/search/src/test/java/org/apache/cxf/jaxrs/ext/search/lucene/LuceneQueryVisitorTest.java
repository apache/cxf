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
package org.apache.cxf.jaxrs.ext.search.lucene;

import java.util.Collections;

import org.apache.cxf.jaxrs.ext.search.SearchBean;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchConditionVisitor;
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LuceneQueryVisitorTest extends Assert {

    private DirectoryReader ireader;
    private IndexSearcher isearcher;
    private Directory directory;
    private Analyzer analyzer;
    
    @Before
    public void setUp() throws Exception {
        analyzer = new StandardAnalyzer(Version.LUCENE_40);
        directory = new RAMDirectory();
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_40, analyzer);
        IndexWriter iwriter = new IndexWriter(directory, config);
        
        Document doc = new Document();
        doc.add(new Field("contents", "name=text", TextField.TYPE_STORED));
        
        IntField intField = new IntField("intfield", 4, Field.Store.YES);
        doc.add(intField);
        iwriter.addDocument(doc);
        
        iwriter.close();
        ireader = DirectoryReader.open(directory);
        isearcher = new IndexSearcher(ireader);
    }
    
    @After
    public void tearDown() throws Exception {
        ireader.close();
        directory.close();
    }
    
    @Test
    public void testTextContentMatchEqual() throws Exception {
        
        doTestTextContentMatch("ct==text");
    }
    
    @Test
    public void testTextContentMatchNotEqual() throws Exception {
        
        Query query = createTermQuery("contents", "ct!=text");
        doTestNoMatch(query);
            
    }
    
    @Test
    public void testTextContentMatchNotEqualPositive() throws Exception {
        
        Query query = createTermQuery("contents", "ct!=bar");
        doTestNoMatch(query);
            
    }
    
    @Test
    public void testTextContentMatchWildcardEnd() throws Exception {
        doTestTextContentMatch("ct==tex*");
    }
    
    @Test
    public void testTextContentMatchWildcardStart() throws Exception {
        doTestTextContentMatch("ct==*ext");
    }
    
    @Test
    public void testIntContentMatchGreater() throws Exception {
        doTestIntContentMatch("ct=gt=3");
    }
    
    @Test
    public void testIntContentMatchGreaterWithClassFiled() throws Exception {
        Query query = createTermQueryWithFieldClass("intfield", "ct=gt=3", Integer.class);
        doTestIntContentMatchWithQuery(query);
    }
    
    @Test
    public void testIntContentMatchGreaterNoMatch() throws Exception {
        Query query = createTermQuery("intfield", "ct=gt=5");
        doTestNoMatch(query);
    }
    
    @Test
    public void testIntContentMatchGreaterOrEqual() throws Exception {
        doTestIntContentMatch("ct=ge=4");
        doTestIntContentMatch("ct=ge=3");
    }
    
    @Test
    public void testIntContentMatchGreaterOrEqualNoMatch() throws Exception {
        Query query = createTermQuery("intfield", "ct=ge=5");
        doTestNoMatch(query);
    }
    
    @Test
    public void testIntContentMatchLess() throws Exception {
        doTestIntContentMatch("ct=lt=5");
    }
    
    @Test
    public void testIntContentMatchLessNoMatch() throws Exception {
        Query query = createTermQuery("intfield", "ct=lt=3");
        doTestNoMatch(query);
    }
    
    @Test
    public void testIntContentMatchLessOrEqual() throws Exception {
        doTestIntContentMatch("ct=le=4");
        doTestIntContentMatch("ct=le=5");
    }
    
    @Test
    public void testIntContentMatchLessOrEqualNoMatch() throws Exception {
        Query query = createTermQuery("intfield", "ct=le=3");
        doTestNoMatch(query);
    }
    
    @Test
    public void testIntContentMatchEquals() throws Exception {
        Query query = createTermQueryWithFieldClass("intfield", "ct==4", Integer.class);
        doTestIntContentMatchWithQuery(query);
    }
    
    @Test
    public void testTextAndContentMatch() throws Exception {
        Query query = createTermQuery("contents==name;contents==text");
        doTestTextContentMatchWithQuery(query);
        
    }
    
    @Test
    public void testTextAndContentNoMatch() throws Exception {
        Query query = createTermQuery("contents==bar;contents==text");
        doTestNoMatch(query);
    }
    
    @Test
    public void testTextOrContentMatch() throws Exception {
        Query query = createTermQuery("contents==bar,contents==text");
        doTestTextContentMatchWithQuery(query);
        
    }
    
    @Test
    public void testTextOrContentNoMatch() throws Exception {
        Query query = createTermQuery("contents==bar,contents==foo");
        doTestNoMatch(query);
    }
    
    @Test
    public void testIntAndTextContentMatch() throws Exception {
        
        Query query = createTermQueryWithFieldClass("intfield==4;contents==text", Integer.class);
        doTestIntContentMatchWithQuery(query);
        doTestTextContentMatchWithQuery(query);
        
    }
    
    @Test
    public void testIntAndTextContentNoMatch() throws Exception {
        Query query = createTermQuery("intfield==3;contents==text");
        doTestNoMatch(query);
    }
    
    @Test
    public void testIntOrTextContentMatch() throws Exception {
        Query query = createTermQuery("intfield==3,contents==text");
        doTestTextContentMatchWithQuery(query);
        doTestIntContentMatchWithQuery(query);
        
    }
    
    @Test
    public void testIntOrTextContentNoMatch() throws Exception {
        Query query = createTermQuery("intfield==3,contents==bar");
        doTestNoMatch(query);
    }
    
    @Test
    public void testTextContentMatchEqualPhrase() throws Exception {
        Query query = createPhraseQuery("contents", "name==text");
        doTestTextContentMatchWithQuery(query);
    }
    
    @Test
    public void testTextContentMatchNotEqualPhrase() throws Exception {
        
        Query query = createPhraseQuery("contents", "name!=text");
        doTestNoMatch(query);
    }
    
    @Test
    public void testTextContentMatchEqualPhraseWildcard() throws Exception {
        Query query = createPhraseQuery("contents", "name==tex*");
        doTestTextContentMatchWithQuery(query);
    }
    
    private void doTestTextContentMatch(String expression) throws Exception {
        
        Query query = createTermQuery("contents", expression);
        doTestTextContentMatchWithQuery(query);
            
    }
    
    private void doTestNoMatch(Query query) throws Exception {
        ScoreDoc[] hits = isearcher.search(query, null, 1000).scoreDocs;
        assertEquals(0, hits.length);
    }
    
    private void doTestTextContentMatchWithQuery(Query query) throws Exception {
        ScoreDoc[] hits = isearcher.search(query, null, 1000).scoreDocs;
        assertEquals(1, hits.length);
        // Iterate through the results:
        for (int i = 0; i < hits.length; i++) {
            Document hitDoc = isearcher.doc(hits[i].doc);
            assertEquals("name=text", hitDoc.get("contents"));
        }
            
    }
    
    private void doTestIntContentMatch(String expression) throws Exception {
        
        Query query = createTermQuery("intfield", expression);
        doTestIntContentMatchWithQuery(query);
            
    }
    
    private void doTestIntContentMatchWithQuery(Query query) throws Exception {
        
        ScoreDoc[] hits = isearcher.search(query, null, 1000).scoreDocs;
        assertEquals(1, hits.length);
        // Iterate through the results:
        for (int i = 0; i < hits.length; i++) {
            Document hitDoc = isearcher.doc(hits[i].doc);
            IndexableField field = hitDoc.getField("intfield");
            assertEquals(4, field.numericValue().intValue());
        }
            
    }
    
    private Query createTermQuery(String expression) throws Exception {
        SearchCondition<SearchBean> filter = 
            new FiqlParser<SearchBean>(SearchBean.class).parse(expression);
        SearchConditionVisitor<SearchBean, Query> lucene = new LuceneQueryVisitor<SearchBean>();
        lucene.visit(filter);
        return lucene.getQuery();
    }
    
    private Query createTermQueryWithFieldClass(String expression, Class<?> cls) throws Exception {
        SearchCondition<SearchBean> filter = 
            new FiqlParser<SearchBean>(SearchBean.class).parse(expression);
        LuceneQueryVisitor<SearchBean> lucene = new LuceneQueryVisitor<SearchBean>();
        lucene.setPrimitiveFieldTypeMap(Collections.<String, Class<?>>singletonMap("intfield", cls));
        lucene.visit(filter);
        return lucene.getQuery();
    }
    
    private Query createTermQuery(String fieldName, String expression) throws Exception {
        SearchCondition<SearchBean> filter = 
            new FiqlParser<SearchBean>(SearchBean.class).parse(expression);
        LuceneQueryVisitor<SearchBean> lucene = 
            new LuceneQueryVisitor<SearchBean>("ct", fieldName);
        lucene.visit(filter);
        return lucene.getQuery();
    }
    
    private Query createTermQueryWithFieldClass(String fieldName, String expression, Class<?> cls) 
        throws Exception {
        SearchCondition<SearchBean> filter = 
            new FiqlParser<SearchBean>(SearchBean.class).parse(expression);
        LuceneQueryVisitor<SearchBean> lucene = 
            new LuceneQueryVisitor<SearchBean>("ct", fieldName);
        lucene.setPrimitiveFieldTypeMap(Collections.<String, Class<?>>singletonMap(fieldName, cls));
        lucene.visit(filter);
        return lucene.getQuery();
    }
    
    private Query createPhraseQuery(String fieldName, String expression) throws Exception {
        SearchCondition<SearchBean> filter = 
            new FiqlParser<SearchBean>(SearchBean.class).parse(expression);
        LuceneQueryVisitor<SearchBean> lucene = 
            new LuceneQueryVisitor<SearchBean>(fieldName);
        lucene.visit(filter);
        return lucene.getQuery();
    }
}
