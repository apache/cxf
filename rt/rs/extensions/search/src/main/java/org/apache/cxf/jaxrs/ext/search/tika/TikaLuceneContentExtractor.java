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

import org.apache.cxf.jaxrs.ext.search.tika.TikaContentExtractor.TikaContent;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.Parser;

public class TikaLuceneContentExtractor {
    private final TikaContentExtractor extractor;
    private final String contentFieldName;
    
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
     * will try to detect the media type of the input and validate it against media types
     * supported by the parser.
     * @param parser parser instance
     * @param validateMediaType enabled or disable media type validation
     */
    public TikaLuceneContentExtractor(final Parser parser, final boolean validateMediaType) {
        this(parser, validateMediaType, "contents");
    }
    
    /**
     * Create new Tika-based content extractor using the provided parser instance and
     * optional media type validation. If validation is enabled, the implementation 
     * will try to detect the media type of the input and validate it against media types
     * supported by the parser.
     * @param parser parser instance
     * @param validateMediaType enabled or disable media type validation
     * @param contentFieldName name of the content field, default is "contents"
     */
    public TikaLuceneContentExtractor(final Parser parser, final boolean validateMediaType, 
                                final String contentFieldName) {
        extractor = new TikaContentExtractor(parser, validateMediaType);
        this.contentFieldName = contentFieldName;
    }
    
    /**
     * Extract the content and metadata from the input stream. Depending on media type validation,
     * the detector could be run against input stream in order to ensure that parser supports this
     * type of content. 
     * @param in input stream to extract the content and metadata from  
     * @return the extracted document or null if extraction is not possible or was unsuccessful
     */
    public Document extract(final InputStream in) {
        return extractAll(in, true, true);
    }
    
    /**
     * Extract the content only from the input stream. Depending on media type validation,
     * the detector could be run against input stream in order to ensure that parser supports this
     * type of content. 
     * @param in input stream to extract the content from  
     * @return the extracted document or null if extraction is not possible or was unsuccessful
     */
    public Document extractContent(final InputStream in) {
        return extractAll(in, true, false);
    }
    
    /**
     * Extract the metadata only from the input stream. Depending on media type validation,
     * the detector could be run against input stream in order to ensure that parser supports this
     * type of content. 
     * @param in input stream to extract the metadata from  
     * @return the extracted document or null if extraction is not possible or was unsuccessful
     */
    public Document extractMetadata(final InputStream in) {
        return extractAll(in, false, true);
    }
    
    private Document extractAll(final InputStream in, boolean extractContent, boolean extractMetadata) {
        
        TikaContent content = extractor.extractAll(in, extractContent);
        
        if (content == null) {
            return null;
        }
        final Document document = new Document();
        if (content.getContent() != null) {
            document.add(new Field(contentFieldName, content.getContent(), TextField.TYPE_STORED));
        } 
        if (extractMetadata) {
            Metadata metadata = content.getMetadata();
            for (final String property: metadata.names()) {
                document.add(new StringField(property, metadata.get(property), Store.YES));
            }
        }
        
        return document;
        
    }
}
