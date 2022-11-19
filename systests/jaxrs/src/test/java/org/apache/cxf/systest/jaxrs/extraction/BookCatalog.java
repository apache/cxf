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

package org.apache.cxf.systest.jaxrs.extraction;


import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jakarta.activation.DataHandler;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.tika.parser.pdf.PDFParser;

@Path("/catalog")
public class BookCatalog {
    private final TikaLuceneContentExtractor extractor = new TikaLuceneContentExtractor(new PDFParser());
    private final Directory directory = new ByteBuffersDirectory();
    private final Analyzer analyzer = new StandardAnalyzer();
    private final LuceneQueryVisitor<SearchBean> visitor = createVisitor();
    
    @POST
    @Consumes("multipart/form-data")
    public Response addBook(final MultipartBody body) throws Exception {
        for (final Attachment attachment: body.getAllAttachments()) {
            final DataHandler handler = attachment.getDataHandler();

            if (handler != null) {
                final String source = handler.getName();
                final LuceneDocumentMetadata metadata = new LuceneDocumentMetadata()
                        .withSource(source)
                        .withField("modified", Date.class)
                        .withField("dcterms:modified", Date.class);

                final Document document = extractor.extract(handler.getInputStream(), metadata);
                if (document != null) {
                    try (IndexWriter writer = getIndexWriter()) {
                        writer.addDocument(document);
                        writer.commit();
                    }
                }
            }
        }

        return Response.ok().build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<ScoreDoc> findBook(@Context SearchContext searchContext) throws IOException {
        try (IndexReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            visitor.visit(searchContext.getCondition(SearchBean.class));
            return Arrays.asList(searcher.search(visitor.getQuery(), 1000).scoreDocs);
        }
    }

    @DELETE
    public Response delete() throws IOException {
        try (IndexWriter writer = getIndexWriter()) {
            writer.deleteAll();
            writer.commit();
        }

        return Response.ok().build();
    }

    private IndexWriter getIndexWriter() throws IOException {
        return new IndexWriter(directory, new IndexWriterConfig(analyzer));
    }

    private static LuceneQueryVisitor< SearchBean > createVisitor() {
        final Map< String, Class< ? > > fieldTypes = new HashMap<>();
        fieldTypes.put("modified", Date.class);
        fieldTypes.put("dcterms:modified", Date.class);

        LuceneQueryVisitor<SearchBean> visitor = new LuceneQueryVisitor<>("ct", "contents");
        visitor.setPrimitiveFieldTypeMap(fieldTypes);
        return visitor;
    }

}


