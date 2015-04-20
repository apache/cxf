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
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.activation.DataHandler;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.jaxrs.ext.search.SearchBean;
import org.apache.cxf.jaxrs.ext.search.SearchContext;
import org.apache.cxf.jaxrs.ext.search.lucene.LuceneQueryVisitor;
import org.apache.cxf.jaxrs.ext.search.tika.LuceneDocumentMetadata;
import org.apache.cxf.jaxrs.ext.search.tika.TikaLuceneContentExtractor;
import org.apache.cxf.rs.security.cors.CrossOriginResourceSharing;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DocumentStoredFieldVisitor;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.odf.OpenDocumentParser;
import org.apache.tika.parser.pdf.PDFParser;

@Path("/catalog")
public class Catalog {
    private final TikaLuceneContentExtractor extractor = new TikaLuceneContentExtractor(
        Arrays.< Parser >asList(new PDFParser(), new OpenDocumentParser()), 
        new LuceneDocumentMetadata());    
    private final Directory directory = new RAMDirectory();
    private final Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_4_9);    
    private final Storage storage; 
    private final LuceneQueryVisitor<SearchBean> visitor;
    private final ExecutorService executor = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors());
    
    public Catalog() throws IOException {
        this(new Storage());
    }
    
    public Catalog(final Storage storage) throws IOException {
        this.storage = storage;
        this.visitor = createVisitor();
        initIndex();
    }
    
    @POST
    @CrossOriginResourceSharing(allowAllOrigins = true)
    @Consumes("multipart/form-data")
    public void addBook(@Suspended final AsyncResponse response, @Context final UriInfo uri, 
            final MultipartBody body)  {
        
        executor.submit(new Runnable() {
            public void run() {
                for (final Attachment attachment: body.getAllAttachments()) {
                    final DataHandler handler =  attachment.getDataHandler();
                    
                    if (handler != null) {
                        final String source = handler.getName();
                        if (StringUtils.isEmpty(source)) {
                            response.resume(Response.status(Status.BAD_REQUEST).build());
                            return;
                        }
                                                
                        final LuceneDocumentMetadata metadata = new LuceneDocumentMetadata()
                            .withSource(source)
                            .withField("modified", Date.class);
                        
                        try {
                            if (exists(source)) {
                                response.resume(Response.status(Status.CONFLICT).build());
                                return;
                            }

                            final byte[] content = IOUtils.readBytesFromStream(handler.getInputStream());
                            storeAndIndex(metadata, content);
                        } catch (final Exception ex) {
                            response.resume(Response.serverError().build());  
                        } 
                        
                        if (response.isSuspended()) {
                            response.resume(Response.created(uri.getRequestUriBuilder().path(source).build())
                                    .build());
                        }
                    }                       
                }              
                
                if (response.isSuspended()) {
                    response.resume(Response.status(Status.BAD_REQUEST).build());
                }
            }
        });
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
                    new DocumentStoredFieldVisitor(LuceneDocumentMetadata.SOURCE_FIELD);                
                
                reader.document(scoreDoc.doc, fieldVisitor);
                builder.add(fieldVisitor
                        .getDocument()
                        .getField(LuceneDocumentMetadata.SOURCE_FIELD)
                        .stringValue());
            }
            
            return builder.build();
        } finally {
            reader.close();
        }
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @CrossOriginResourceSharing(allowAllOrigins = true)
    @Path("/search")
    public Response findBook(@Context SearchContext searchContext, 
            @Context final UriInfo uri) throws IOException {
        
        final IndexReader reader = DirectoryReader.open(directory);
        final IndexSearcher searcher = new IndexSearcher(reader);
        final JsonArrayBuilder builder = Json.createArrayBuilder();

        try {            
            visitor.reset();
            visitor.visit(searchContext.getCondition(SearchBean.class));
            
            final Query query = visitor.getQuery();            
            if (query != null) {
                final TopDocs topDocs = searcher.search(query, 1000);
                for (final ScoreDoc scoreDoc: topDocs.scoreDocs) {
                    final Document document = reader.document(scoreDoc.doc);
                    final String source = document
                        .getField(LuceneDocumentMetadata.SOURCE_FIELD)
                        .stringValue();
                    
                    builder.add(
                        Json.createObjectBuilder()
                            .add("source", source)
                            .add("score", scoreDoc.score)
                            .add("url", uri.getBaseUriBuilder()
                                    .path(Catalog.class)
                                    .path(source)
                                    .build().toString())
                    );
                }
            }
            
            return Response.ok(builder.build()).build();
        } finally {
            reader.close();
        }
    }
    
    @GET
    @Path("/{source}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public StreamingOutput getBook(@PathParam("source") final String source) throws IOException {            
        return new StreamingOutput() {            
            @Override
            public void write(final OutputStream out) throws IOException, WebApplicationException {
                try (InputStream in = storage.getDocument(source)) {
                    out.write(IOUtils.readBytesFromStream(in));
                } catch (final FileNotFoundException ex) {
                    throw new NotFoundException("Document does not exist: " + source);
                }
            }
        };
    }
    
    @DELETE
    public Response delete() throws IOException {
        final IndexWriter writer = getIndexWriter();
        
        try {
            storage.deleteAll();
            writer.deleteAll();
            writer.commit();
        } finally {
            writer.close();
        }  
        
        return Response.ok().build();
    }
    
    private void initIndex() throws IOException {
        final IndexWriter writer = getIndexWriter();
        
        try {
            writer.commit();
        } finally {
            writer.close();
        }
    }

    private IndexWriter getIndexWriter() throws IOException {
        return new IndexWriter(directory, new IndexWriterConfig(Version.LUCENE_4_9, analyzer));
    }
    
    private LuceneQueryVisitor< SearchBean > createVisitor() {
        final Map< String, Class< ? > > fieldTypes = new HashMap< String, Class< ? > >();
        fieldTypes.put("modified", Date.class);
        
        LuceneQueryVisitor<SearchBean> newVisitor = new LuceneQueryVisitor<SearchBean>(
            "ct", "contents", analyzer);
        newVisitor.setPrimitiveFieldTypeMap(fieldTypes);
        
        return newVisitor;
    }
    
    private boolean exists(final String source) throws IOException {
        final IndexReader reader = DirectoryReader.open(directory);
        final IndexSearcher searcher = new IndexSearcher(reader);

        try {            
            return searcher.search(new TermQuery(
                new Term(LuceneDocumentMetadata.SOURCE_FIELD, source)), 1).totalHits > 0;
        } finally {
            reader.close();
        }
    }
    
    private void storeAndIndex(final LuceneDocumentMetadata metadata, final byte[] content)
        throws IOException {
        
        try (BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(content))) {
            
            final Document document = extractor.extract(in, metadata);
            if (document != null) {                    
                final IndexWriter writer = getIndexWriter();
                
                try {                                              
                    storage.addDocument(metadata.getSource(), content);
                    writer.addDocument(document);
                    writer.commit();
                } finally {
                    writer.close();
                }
            }
        }
    }
}


