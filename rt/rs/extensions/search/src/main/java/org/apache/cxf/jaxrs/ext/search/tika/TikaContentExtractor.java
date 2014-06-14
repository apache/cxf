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
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.ToTextContentHandler;

public class TikaContentExtractor {
    private static final Logger LOG = LogUtils.getL7dLogger(TikaContentExtractor.class);
    
    private final PDFParser parser;
    private final DefaultDetector detector;
    
    public TikaContentExtractor() {
        detector = new DefaultDetector();
        parser = new PDFParser();
    }
    
    public Document extract(final InputStream in) {
        try {
            final Metadata metadata = new Metadata();
            final MediaType mediaType = detector.detect(in, metadata);
            final ParseContext context = new ParseContext(); 
            if (mediaType == null || !parser.getSupportedTypes(context).contains(mediaType)) {
                return null;
            }
            
            final ToTextContentHandler handler = new ToTextContentHandler();
            parser.parse(in, handler, metadata, context);
            
            final Document document = new Document();
            document.add(new Field("contents", handler.toString(), TextField.TYPE_STORED));
            
            for (final String property: metadata.names()) {
                document.add(new StringField(property, metadata.get(property), Store.YES));
            }
            
            return document;
        } catch (final IOException ex) {
            LOG.log(Level.WARNING, "Unable to extract media type from input stream", ex);
        } catch (final SAXException ex) {
            LOG.log(Level.WARNING, "Unable to parse input stream", ex);
        } catch (final TikaException ex) {
            LOG.log(Level.WARNING, "Unable to parse input stream", ex);
        }
     
        return null;
    }
}
