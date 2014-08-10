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

package demo.jaxrs.search.server;


import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.activation.DataHandler;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.jaxrs.ext.search.SearchBean;
import org.apache.cxf.jaxrs.ext.search.SearchContext;
import org.apache.cxf.jaxrs.ext.search.lucene.LuceneQueryVisitor;
import org.apache.cxf.jaxrs.ext.search.tika.LuceneDocumentMetadata;
import org.apache.cxf.jaxrs.ext.search.tika.TikaLuceneContentExtractor;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DocumentStoredFieldVisitor;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.apache.tika.parser.pdf.PDFParser;

@Path("/catalog")
public class Catalog {
    private final TikaLuceneContentExtractor extractor = new TikaLuceneContentExtractor(new PDFParser());    
    private final Directory directory = new RAMDirectory();
    private final Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_40);
    private final IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_40, analyzer);
    private final LuceneQueryVisitor<SearchBean> visitor = createVisitor();
    
    public Catalog() throws IOException {
        initIndex();
    }
    
    @POST
    @Consumes("multipart/form-data")
    public Response addBook(@Context final UriInfo uri, final MultipartBody body) throws Exception {
        for (final Attachment attachment: body.getAllAttachments()) {
            final DataHandler handler =  attachment.getDataHandler();
            
            if (handler != null) {
                final String source = handler.getName();                
                final LuceneDocumentMetadata metadata = new LuceneDocumentMetadata()
                    .withSource(source)
                    .withField("modified", Date.class);
                
                final BufferedInputStream in = new BufferedInputStream(handler.getInputStream());
                try {
                    final Document document = extractor.extract(in, metadata);
                    if (document != null) {                    
                        final IndexWriter writer = new IndexWriter(directory, config);
                        
                        try {
                            writer.addDocument(document);
                            writer.commit();
                        } finally {
                            writer.close();
                        }
                    }
                } finally {
                    if (in != null) { 
                        in.close(); 
                    }
                }
                
                return Response.created(uri.getRequestUriBuilder().path(source).build()).build();
            }                       
        }              
        
        return Response.status(Status.BAD_REQUEST).build();
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonArray getBooks() throws IOException {
        final IndexReader reader = DirectoryReader.open(directory);
        final IndexSearcher searcher = new IndexSearcher(reader);
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        
        try {
            final Query query = new MatchAllDocsQuery();
            
            for (final ScoreDoc scoreDoc: searcher.search(query, 1000).scoreDocs) {
                final DocumentStoredFieldVisitor fieldVisitor = 
                    new DocumentStoredFieldVisitor("source");
                
                reader.document(scoreDoc.doc, fieldVisitor);
                builder.add(fieldVisitor.getDocument().getField("source").stringValue());
            }
            
            return builder.build();
        } finally {
            reader.close();
        }
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/search")
    public Collection<ScoreDoc> findBook(@Context SearchContext searchContext) throws IOException {
        final IndexReader reader = DirectoryReader.open(directory);
        final IndexSearcher searcher = new IndexSearcher(reader);

        try {
            visitor.visit(searchContext.getCondition(SearchBean.class));
            return Arrays.asList(searcher.search(visitor.getQuery(), null, 1000).scoreDocs);
        } finally {
            reader.close();
        }
    }
    
    @DELETE
    public Response delete() throws IOException {
        final IndexWriter writer = new IndexWriter(directory, config);
        
        try {
            writer.deleteAll();
            writer.commit();
        } finally {
            writer.close();
        }  
        
        return Response.ok().build();
    }
    
    private void initIndex() throws IOException {
        final IndexWriter writer = new IndexWriter(directory, config);
        
        try {            
            writer.commit();
        } finally {
            writer.close();
        }
    }
    
    private static LuceneQueryVisitor< SearchBean > createVisitor() {
        final Map< String, Class< ? > > fieldTypes = new HashMap< String, Class< ? > >();
        fieldTypes.put("modified", Date.class);
        
        LuceneQueryVisitor<SearchBean> visitor = new LuceneQueryVisitor<SearchBean>("ct", "contents");
        visitor.setPrimitiveFieldTypeMap(fieldTypes);
        return visitor;
    }
}


