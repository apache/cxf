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
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xml.sax.SAXException;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.ext.search.SearchBean;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.ToTextContentHandler;

public class TikaContentExtractor {
    private static final Logger LOG = LogUtils.getL7dLogger(TikaContentExtractor.class);
    
    private final Parser parser;
    private final DefaultDetector detector;
    private final boolean validateMediaType;
    
    /**
     * Create new Tika-based content extractor using the provided parser instance.  
     * @param parser parser instance
     */
    public TikaContentExtractor(final Parser parser) {
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
    public TikaContentExtractor(final Parser parser, final boolean validateMediaType) {
        this.parser = parser;
        this.validateMediaType = validateMediaType;
        this.detector = validateMediaType ? new DefaultDetector() : null;
    }
    
    /**
     * Extract the content and metadata from the input stream. Depending on media type validation,
     * the detector could be run against input stream in order to ensure that parser supports this
     * type of content. 
     * @param in input stream to extract the content and metadata from  
     * @return the extracted document or null if extraction is not possible or was unsuccessful
     */
    public TikaContent extract(final InputStream in) {
        return extractAll(in, true);
    }
    
    /**
     * Extract the metadata only from the input stream. Depending on media type validation,
     * the detector could be run against input stream in order to ensure that parser supports this
     * type of content. 
     * @param in input stream to extract the metadata from  
     * @return the extracted document or null if extraction is not possible or was unsuccessful
     */
    public TikaContent extractMetadata(final InputStream in) {
        return extractAll(in, false);
    }
    
    /**
     * Extract the metadata only from the input stream. Depending on media type validation,
     * the detector could be run against input stream in order to ensure that parser supports this
     * type of content. 
     * @param in input stream to extract the metadata from  
     * @return the extracted document or null if extraction is not possible or was unsuccessful
     */
    public SearchBean extractMetadataToSearchBean(final InputStream in) {
        Metadata metadata = extractMetadata(in).getMetadata();
        SearchBean bean = new SearchBean();
        for (final String property: metadata.names()) {
            bean.set(property, metadata.get(property));
        }
        return bean;
    }
    
    TikaContent extractAll(final InputStream in, boolean extractContent) {
        if (in == null) {
            return null;
        }
        
        try {
            final Metadata metadata = new Metadata();            
            final ParseContext context = new ParseContext();
            
            // Try to validate that input stream media type is supported by the parser 
            if (validateMediaType) {
                final MediaType mediaType = detector.detect(in, metadata);
                if (mediaType == null || !parser.getSupportedTypes(context).contains(mediaType)) {
                    return null;
                }
            }
            
            final ToTextContentHandler handler = new ToTextContentHandler();
            parser.parse(in, handler, metadata, context);
            // TODO: use a content handler which will ignore parser content events 
            String content = extractContent ? handler.toString() : ""; 
            return new TikaContent(content, metadata);
        } catch (final IOException ex) {
            LOG.log(Level.WARNING, "Unable to extract media type from input stream", ex);
        } catch (final SAXException ex) {
            LOG.log(Level.WARNING, "Unable to parse input stream", ex);
        } catch (final TikaException ex) {
            LOG.log(Level.WARNING, "Unable to parse input stream", ex);
        }
     
        return null;
    }
    public static class TikaContent {
        private String content;
        private Metadata metadata;
        public TikaContent(String content, Metadata metadata) {
            this.content = content;
            this.metadata = metadata;
        }
        public String getContent() {
            return content;
        }
        public Metadata getMetadata() {
            return metadata;
        }
    }
    
}
