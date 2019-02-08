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
/*
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package org.apache.cxf.transport.https.httpclient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;

/**
 * {@link org.apache.http.conn.util.PublicSuffixMatcher} loader.
 *
 * Copied from httpclient.
 */
public final class PublicSuffixMatcherLoader {

    private static final Logger LOG = LogUtils.getL7dLogger(PublicSuffixMatcherLoader.class);
    private static volatile PublicSuffixMatcher defaultInstance;

    private PublicSuffixMatcherLoader() {
        //
    }

    private static PublicSuffixMatcher load(final InputStream in) throws IOException {
        final List<PublicSuffixList> lists = new PublicSuffixListParser().parseByType(
                new InputStreamReader(in, StandardCharsets.UTF_8));
        return new PublicSuffixMatcher(lists);
    }

    public static PublicSuffixMatcher load(final URL url) throws IOException {
        if (url == null) {
            throw new IllegalArgumentException("URL is null");
        }
        try (InputStream in = url.openStream()) {
            return load(in);
        }
    }

    public static PublicSuffixMatcher load(final File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("File is null");
        }
        try (InputStream in = Files.newInputStream(file.toPath())) {
            return load(in);
        }
    }

    public static PublicSuffixMatcher getDefault() {
        if (defaultInstance == null) {
            synchronized (PublicSuffixMatcherLoader.class) {
                if (defaultInstance == null) {
                    final URL url = PublicSuffixMatcherLoader.class.getResource(
                            "/mozilla/public-suffix-list.txt");
                    if (url != null) {
                        try {
                            defaultInstance = load(url);
                        } catch (final IOException ex) {
                            // Should never happen
                            if (LOG.isLoggable(Level.WARNING)) {
                                LOG.log(Level.WARNING,
                                        "Failure loading public suffix list from default resource",
                                        ex);
                            }
                        }
                    } else {
                        defaultInstance = new PublicSuffixMatcher(Arrays.asList("com"), null);
                    }
                }
            }
        }
        return defaultInstance;
    }

}
