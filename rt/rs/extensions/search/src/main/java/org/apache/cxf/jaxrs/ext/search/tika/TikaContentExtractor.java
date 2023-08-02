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
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.ext.search.SearchBean;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.HttpHeaders;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.ToTextContentHandler;

public class TikaContentExtractor {
    private static final Logger LOG = LogUtils.getL7dLogger(TikaContentExtractor.class);

    private final List<Parser> parsers;
    private final Detector detector;

    /**
     * Create new Tika-based content extractor using AutoDetectParser.
     */
    public TikaContentExtractor() {
        this(new AutoDetectParser(), false);
    }

    /**
     * Create new Tika-based content extractor using the provided parser instance.
     * @param parser parser instance
     */
    public TikaContentExtractor(final Parser parser) {
        this(parser, false);
    }

    /**
     * Create new Tika-based content extractor using the provided parser instances.
     * @param parsers parser instances
     */
    public TikaContentExtractor(final List<Parser> parsers) {
        this(parsers, new DefaultDetector());
    }

    /**
     * Create new Tika-based content extractor using the provided parser instances.
     * @param parsers parser instances
     */
    public TikaContentExtractor(final List<Parser> parsers, Detector detector) {
        this.parsers = parsers;
        this.detector = detector;
    }

    /**
     * Create new Tika-based content extractor using the provided parser instance and
     * optional media type validation. If validation is enabled, the implementation parser
     * will try to detect the media type of the input and validate it against media types
     * supported by the parser.
     * @param parser parser instance
     * @param validateMediaType enabled or disable media type validationparser
     */
    public TikaContentExtractor(final Parser parser, final boolean validateMediaType) {
        this(Collections.singletonList(parser), validateMediaType ? new DefaultDetector() : null);
    }

    /**
     * Extract the content and metadata from the input stream. Depending on media type validation,
     * the detector could be run against input stream in order to ensure that parser supports this
     * type of content.
     * @param in input stream to extract the content and metadata from
     * @return the extracted content and metadata or null if extraction is not possible or was unsuccessful
     */
    public TikaContent extract(final InputStream in) {
        return extract(in, (jakarta.ws.rs.core.MediaType)null);
    }

    /**
     * Extract the content and metadata from the input stream with a media type hint.
     * @param in input stream to extract the content and metadata from
     * @param mt JAX-RS MediaType of the stream content
     * @return the extracted content and metadata or null if extraction is not possible or was unsuccessful
     */
    public TikaContent extract(final InputStream in, jakarta.ws.rs.core.MediaType mt) {
        return extract(in, new ToTextContentHandler(), mt);
    }

    /**
     * Extract the content and metadata from the input stream. Depending on media type validation,
     * the detector could be run against input stream in order to ensure that parser supports this
     * type of content.
     * @param in input stream to extract the content and metadata from
     * @param handler custom ContentHandler
     * @return the extracted content and metadata or null if extraction is not possible
     *         or was unsuccessful
     */
    public TikaContent extract(final InputStream in, final ContentHandler handler) {
        return extract(in, handler, (jakarta.ws.rs.core.MediaType)null);
    }

    /**
     * Extract the content and metadata from the input stream with a media type hint.
     * @param in input stream to extract the content and metadata from
     * @param handler custom ContentHandler
     * @param mt JAX-RS MediaType of the stream content
     * @return the extracted content and metadata or null if extraction is not possible
     *         or was unsuccessful
     */
    public TikaContent extract(final InputStream in, final ContentHandler handler,
                               jakarta.ws.rs.core.MediaType mt) {
        return extract(in, handler, mt, (ParseContext)null);
    }

    /**
     * Extract the content and metadata from the input stream. Depending on media type validation,
     * the detector could be run against input stream in order to ensure that parser supports this
     * type of content.
     * @param in input stream to extract the content and metadata from
     * @param handler custom ContentHandler
     * @param context custom context
     * @return the extracted content and metadata or null if extraction is not possible
     *         or was unsuccessful
     */
    public TikaContent extract(final InputStream in, final ContentHandler handler, ParseContext context) {
        return extract(in, handler, (jakarta.ws.rs.core.MediaType)null, context);
    }

    /**
     * Extract the content and metadata from the input stream with a media type hint
     * type of content.
     * @param in input stream to extract the metadata from
     * @param handler custom ContentHandler
     * @param mtHint JAX-RS MediaType of the stream content
     * @param context custom context
     * @return the extracted content and metadata or null if extraction is not possible
     *         or was unsuccessful
     */
    public TikaContent extract(final InputStream in, ContentHandler handler,
                               jakarta.ws.rs.core.MediaType mtHint, ParseContext context) {
        if (in == null) {
            return null;
        }
        final Metadata metadata = new Metadata();

        try {
            // Try to validate that input stream media type is supported by the parser
            MediaType mediaType = null;
            if (mtHint != null) {
                mediaType = MediaType.parse(mtHint.toString());
            } else if (detector != null && in.markSupported()) {
                mediaType = detector.detect(in, metadata);
            }
            if (mediaType != null) {
                metadata.set(HttpHeaders.CONTENT_TYPE, mediaType.toString());
            }

            Parser parser = null;
            if (parsers.size() == 1) {
                parser = parsers.get(0);
            } else {
                for (Parser p : parsers) {
                    if (mediaType != null && !p.getSupportedTypes(context).contains(mediaType)) {
                        continue;
                    }
                    parser = p;
                    break;
                }
            }
            if (parser == null) {
                return null;
            }

            if (context == null) {
                context = new ParseContext();
            }
            if (context.get(Parser.class) == null) {
                // to process the embedded attachments
                context.set(Parser.class,
                            parser instanceof AutoDetectParser ? parser : new AutoDetectParser());
            }


            try {
                parser.parse(in, handler, metadata, context);
            } catch (Exception ex) {
                // Starting from Tika 1.6 PDFParser (with other parsers to be updated in the future) will skip
                // the content processing if the content handler is null. This can be used to optimize the
                // extraction process. If we get an exception with a null handler then a given parser is still
                // not ready to accept null handlers so lets retry with IgnoreContentHandler.
                if (handler == null) {
                    handler = new IgnoreContentHandler();
                    parser.parse(in, handler, metadata, context);
                } else {
                    throw ex;
                }
            }
            return new TikaContent(handler, metadata, mediaType);
        } catch (final IOException ex) {
            LOG.log(Level.WARNING, "Unable to extract media type from input stream", ex);
        } catch (final SAXException ex) {
            LOG.log(Level.WARNING, "Unable to parse input stream", ex);
        } catch (final TikaException ex) {
            LOG.log(Level.WARNING, "Unable to parse input stream", ex);
        }

        return null;
    }
    /**
     * Extract the metadata only from the input stream. Depending on media type validation,
     * the detector could be run against input stream in order to ensure that parser supports this
     * type of content.
     * @param in input stream to extract the metadata from
     * @return the extracted content or null if extraction is not possible or was unsuccessful
     */
    public TikaContent extractMetadata(final InputStream in) {
        return extract(in, (ContentHandler)null);
    }

    /**
     * Extract the metadata only from the input stream. Depending on media type validation,
     * the detector could be run against input stream in order to ensure that parser supports this
     * type of content.
     * @param in input stream to extract the metadata from
     * @return the extracted metadata converted to SearchBean or null if extraction is not possible
     *         or was unsuccessful
     */
    public SearchBean extractMetadataToSearchBean(final InputStream in) {
        TikaContent tc = extractMetadata(in);
        if (tc == null) {
            return null;
        }
        Metadata metadata = tc.getMetadata();
        SearchBean bean = new SearchBean();
        for (final String property: metadata.names()) {
            bean.set(property, metadata.get(property));
        }
        return bean;
    }


    /**
     * Extracted content, metadata and media type container
     */
    public static class TikaContent implements Serializable {
        private static final long serialVersionUID = -1240120543378490963L;
        private ContentHandler content;
        private Metadata metadata;
        private MediaType mediaType;
        public TikaContent(ContentHandler content, Metadata metadata, MediaType mediaType) {
            this.content = content;
            this.metadata = metadata;
            this.mediaType = mediaType;
        }
        /**
         * Return the content cached by ContentHandler
         * @return the content, may be empty or null if a custom non-caching ContentHandler was used
         *         to parse the content
         */
        public String getContent() {
            return content instanceof ToTextContentHandler ? content.toString() : null;
        }
        /**
         * Return the metadata
         * @return the metadata
         */
        public Metadata getMetadata() {
            return metadata;
        }
        /**
         * Return the detected media type of the content
         * @return the media type, null if no auto-detection was done
         */
        public MediaType getMediaType() {
            return mediaType;
        }
    }

    private static final class IgnoreContentHandler extends ToTextContentHandler {
        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            // Complete
        }
        @Override
        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
            // Complete
        }
        @Override
        public String toString() {
            return "";
        }


    }
}
