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
import java.util.Date;
import java.util.List;

import javax.ws.rs.ext.ParamConverterProvider;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.ext.search.tika.TikaContentExtractor.TikaContent;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.Parser;

import static org.apache.cxf.jaxrs.ext.search.ParamConverterUtils.getString;
import static org.apache.cxf.jaxrs.ext.search.ParamConverterUtils.getValue;

public class TikaLuceneContentExtractor {
    private final LuceneDocumentMetadata defaultDocumentMetadata;    
    private final TikaContentExtractor extractor;
    
    /**
     * Create new Tika-based content extractor using the provided parser instance.  
     * @param parser parser instance
     */
    public TikaLuceneContentExtractor(final Parser parser) {
        this(parser, true);
    }
    
    /**
     * Create new Tika-based content extractor using the provided parser instance and
     * optional media type validation. If validation is enabled, the implementation 
     * will try to detect the media type of the input and validate it against media typesthis.contentFieldName
     * supported by the parser.
     * @param parser parser instance
     * @param validateMediaType enabled or disable media type validation
     */
    public TikaLuceneContentExtractor(final Parser parser, final boolean validateMediaType) {
        this(parser, validateMediaType, new LuceneDocumentMetadata());
    }
    
    /**
     * Create new Tika-based content extractor using the provided parser instance and
     * optional media type validation. If validation is enabled, the implementation 
     * will try to detect the media type of the input and validate it against media types
     * supported by the parser.
     * @param parser parser instancethis.contentFieldName
     * @param documentMetadata documentMetadata
     */
    public TikaLuceneContentExtractor(final Parser parser, 
                                      final LuceneDocumentMetadata documentMetadata) {
        this(parser, false, new LuceneDocumentMetadata());
    }
    
    /**
     * Create new Tika-based content extractor using the provided parser instance and
     * optional media type validation. If validation is enabled, the implementation 
     * will try to detect the media type of the input and validate it against media types
     * supported by the parser.
     * @param parser parser instancethis.contentFieldName
     * @param validateMediaType enabled or disable media type validation
     * @param documentMetadata documentMetadata
     */
    public TikaLuceneContentExtractor(final Parser parser, 
                                      final boolean validateMediaType, 
                                      final LuceneDocumentMetadata documentMetadata) {
        this.extractor = new TikaContentExtractor(parser, validateMediaType);
        this.defaultDocumentMetadata = documentMetadata;
    }
    
    /**
     * Create new Tika-based content extractor using the provided parser instance and
     * optional media type validation. If validation is enabled, the implementation 
     * will try to detect the media type of the input and validate it against media types
     * supported by the parser.
     * @param parser parser instancethis.contentFieldName
     * @param validateMediaType enabled or disable media type validation
     * @param documentMetadata documentMetadata
     */
    public TikaLuceneContentExtractor(final List<Parser> parsers, 
                                      final LuceneDocumentMetadata documentMetadata) {
        this.extractor = new TikaContentExtractor(parsers);
        this.defaultDocumentMetadata = documentMetadata;
    }
    
    /**
     * Extract the content and metadata from the input stream. Depending on media type validation,
     * the detector could be run against input stream in order to ensure that parser supports this
     * type of content. 
     * @param in input stream to extract the content and metadata from  
     * @return the extracted document or null if extraction is not possible or was unsuccessful
     */
    public Document extract(final InputStream in) {
        return extractAll(in, null, true, true);
    }
    
    /**
     * Extract the content and metadata from the input stream. Depending on media type validation,
     * the detector could be run against input stream in order to ensure that parser supports this
     * type of content. 
     * @param in input stream to extract the content and metadata from  
     * @param documentMetadata documentMetadata
     * @return the extracted document or null if extraction is not possible or was unsuccessful
     */
    public Document extract(final InputStream in, final LuceneDocumentMetadata documentMetadata) {
        return extractAll(in, documentMetadata, true, true);
    }
    
    /**
     * Extract the content only from the input stream. Depending on media type validation,
     * the detector could be run against input stream in order to ensure that parser supports this
     * type of content. 
     * @param in input stream to extract the content from  
     * @return the extracted document or null if extraction is not possible or was unsuccessful
     */
    public Document extractContent(final InputStream in) {
        return extractAll(in, null, true, false);
    }
    
    /**
     * Extract the metadata only from the input stream. Depending on media type validation,
     * the detector could be run against input stream in order to ensure that parser supports this
     * type of content. 
     * @param in input stream to extract the metadata from  
     * @return the extracted document or null if extraction is not possible or was unsuccessful
     */
    public Document extractMetadata(final InputStream in) {
        return extractAll(in, null, false, true);
    }
    
    /**
     * Extract the metadata only from the input stream. Depending on media type validation,
     * the detector could be run against input stream in order to ensure that parser supports this
     * type of content. 
     * @param in input stream to extract the metadata from
     * @param documentMetadata documentMetadata  
     * @return the extracted document or null if extraction is not possible or was unsuccessful
     */
    public Document extractMetadata(final InputStream in, final LuceneDocumentMetadata documentMetadata) {
        return extractAll(in, documentMetadata, false, true);
    }
    
    private Document extractAll(final InputStream in, 
                                LuceneDocumentMetadata documentMetadata, 
                                boolean extractContent, 
                                boolean extractMetadata) {
        
        TikaContent content = extractor.extract(in, extractContent);
        
        if (content == null) {
            return null;
        }
        final Document document = new Document();
        
        if (documentMetadata == null) { 
            documentMetadata = defaultDocumentMetadata;
        }
        if (content.getContent() != null) {
            document.add(getContentField(documentMetadata, content.getContent()));
        } 
        
        if (extractMetadata) {
            Metadata metadata = content.getMetadata();
            for (final String property: metadata.names()) {
                document.add(getField(documentMetadata, property, metadata.get(property)));
            }
        }
        
        if (!StringUtils.isEmpty(documentMetadata.getSource())) {
            document.add(new StringField(documentMetadata.getSourceFieldName(), 
                documentMetadata.getSource(), Store.YES));
        }
        
        return document;
        
    }
    
    private static Field getContentField(final LuceneDocumentMetadata documentMetadata, final String content) {
        return new TextField(documentMetadata.getContentFieldName(), content, Store.YES);
    }
    
    
    private static Field getField(final LuceneDocumentMetadata documentMetadata, 
                                  final String name, final String value) { 
        final Class< ? > type = documentMetadata.getFieldType(name);
        final ParamConverterProvider provider = documentMetadata.getFieldTypeConverter();
        
        if (type != null) {
            if (Number.class.isAssignableFrom(type)) {
                if (Double.class.isAssignableFrom(type)) {
                    return new DoubleField(name, getValue(Double.class, provider, value), Store.YES);
                } else if (Float.class.isAssignableFrom(type)) {
                    return new FloatField(name, getValue(Float.class, provider, value), Store.YES);
                } else if (Long.class.isAssignableFrom(type)) {
                    return new LongField(name, getValue(Long.class, provider, value), Store.YES);
                } else if (Integer.class.isAssignableFrom(type) || Byte.class.isAssignableFrom(type)) {
                    return new IntField(name, getValue(Integer.class, provider, value), Store.YES);
                }
            } else if (Date.class.isAssignableFrom(type)) {
                final Date date = getValue(Date.class, provider, value);                
                Field field = null;
                
                if (date != null) {
                    field = new StringField(name, getString(Date.class, provider, date), Store.YES);
                } else {
                    field = new StringField(name, value, Store.YES); 
                }
                
                return field;
            }                
        }
        
        return new StringField(name, value, Store.YES);
    }    
}
