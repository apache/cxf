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

import jakarta.ws.rs.ext.ParamConverterProvider;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.ext.search.ParamConverterUtils;
import org.apache.cxf.jaxrs.ext.search.tika.TikaContentExtractor.TikaContent;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.ToTextContentHandler;

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
        this(parser, false, documentMetadata);
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
     * @param parsers parsers instancethis.contentFieldName
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

        TikaContent content =
            extractor.extract(in, extractContent ? new ToTextContentHandler() : null);

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
                addField(document, documentMetadata, property, metadata.get(property));
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


    private static void addField(final Document document,
                                  final LuceneDocumentMetadata documentMetadata,
                                  final String name, final String value) {
        final Class< ? > type = documentMetadata.getFieldType(name);
        final ParamConverterProvider provider = documentMetadata.getFieldTypeConverter();

        if (type != null) {
            if (Number.class.isAssignableFrom(type)) {
                if (Double.class.isAssignableFrom(type)) {
                    Double number = ParamConverterUtils.getValue(Double.class, provider, value);
                    document.add(new DoublePoint(name, number));
                    document.add(new StoredField(name, number));
                } else if (Float.class.isAssignableFrom(type)) {
                    Float number = ParamConverterUtils.getValue(Float.class, provider, value);
                    document.add(new FloatPoint(name, number));
                    document.add(new StoredField(name, number));
                } else if (Long.class.isAssignableFrom(type)) {
                    Long number = ParamConverterUtils.getValue(Long.class, provider, value);
                    document.add(new LongPoint(name, number));
                    document.add(new StoredField(name, number));
                } else if (Integer.class.isAssignableFrom(type) || Byte.class.isAssignableFrom(type)) {
                    Integer number = ParamConverterUtils.getValue(Integer.class, provider, value);
                    document.add(new IntPoint(name, number));
                    document.add(new StoredField(name, number));
                } else {
                    document.add(new StringField(name, value, Store.YES));
                }
                return;
            } else if (Date.class.isAssignableFrom(type)) {
                final Date date = ParamConverterUtils.getValue(Date.class, provider, value);
                final Field field;

                if (date != null) {
                    field = new StringField(name,
                                            ParamConverterUtils.getString(Date.class, provider, date), Store.YES);
                } else {
                    field = new StringField(name, value, Store.YES);
                }

                document.add(field);
                return;
            }
        }

        document.add(new StringField(name, value, Store.YES));
    }
}
