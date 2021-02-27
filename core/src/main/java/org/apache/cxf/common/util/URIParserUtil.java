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

package org.apache.cxf.common.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.StringTokenizer;

import org.apache.cxf.common.classloader.ClassLoaderUtils;

public final class URIParserUtil {
    private static final String EXCLUDED_CHARS = "<>\"{}|\\^`";

    private URIParserUtil() {
        // complete
    }

    private static boolean isExcluded(char ch) {
        return ch <= 0x20 || ch >= 0x7F || EXCLUDED_CHARS.indexOf(ch) != -1;
    }

    public static URL[] pathToURLs(String path) {
        StringTokenizer st = new StringTokenizer(path, File.pathSeparator);
        URL[] urls = new URL[st.countTokens()];
        int count = 0;
        while (st.hasMoreTokens()) {
            File file = new File(st.nextToken());
            URL url = null;
            try {
                url = file.toURI().toURL();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            if (url != null) {
                urls[count++] = url;
            }
        }
        if (urls.length != count) {
            URL[] tmp = new URL[count];
            System.arraycopy(urls, 0, tmp, 0, count);
            urls = tmp;
        }
        return urls;
    }

    public static String escapeChars(String s) {
        StringBuilder sb = new StringBuilder(s.length());

        for (int x = 0; x < s.length(); x++) {
            char ch = s.charAt(x);
            if (isExcluded(ch)) {
                byte[] bytes = Character.toString(ch).getBytes(StandardCharsets.UTF_8);
                for (byte b : bytes) {
                    sb.append('%');
                    StringUtils.byteToHex(b, sb);
                }
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
    public static String normalize(final String uri) {
        URL url = null;
        String result;
        try {
            url = new URL(uri);
            result = escapeChars(url.toURI().normalize().toString().replace('\\', '/'));
        } catch (MalformedURLException e1) {
            try {
                if (uri.startsWith("classpath:")) {
                    url = ClassLoaderUtils.getResource(uri.substring(10), URIParserUtil.class);
                    return url != null ? url.toExternalForm() : uri;
                }
                File file = new File(uri);
                if (file.exists()) {
                    return file.toURI().normalize().toString();
                }
                final String f;
                if (uri.indexOf(':') != -1 && !uri.startsWith("/")) {
                    f = "file:/" + uri;
                } else {
                    f = "file:" + uri;
                }
                url = new URL(f);
                return escapeChars(url.toString().replace("\\", "/"));
            } catch (Exception e2) {
                return escapeChars(uri.replace("\\", "/"));
            }
        } catch (URISyntaxException e) {
            result = escapeChars(url.toString().replace("\\", "/"));
        }
        return result;
    }

    public static String getAbsoluteURI(final String arg) {
        if (arg == null) {
            return null;
        }

        try {
            URI uri = new URI(arg);
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                if (!uri.isOpaque()) {
                    return uri.normalize().toString();
                }
                return new File("").toURI().resolve(uri.getPath()).toString();
            }
            return normalize(arg);
        } catch (Exception e2) {
            return normalize(arg);
        }
    }

    public static String relativize(String base, String toBeRelativized) throws URISyntaxException {
        if (base == null || toBeRelativized == null) {
            return null;
        }
        return relativize(new URI(base), new URI(toBeRelativized));
    }

    /**
     * This is a custom implementation for doing what URI.relativize(URI uri) should be
     * doing but is not actually doing when URI roots do not fully match.
     * See http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6226081
     *
     * @param baseURI              The base URI
     * @param toBeRelativizedURI   The URI to be relativized
     * @return                  The string value of the URI you'd expect to get as result
     *                          of calling baseURI.relativize(toBeRelativizedURI).
     *                          null is returned if the parameters are null or are not
     *                          both absolute or not absolute.
     * @throws URISyntaxException
     */
    public static String relativize(URI baseURI, URI toBeRelativizedURI) throws URISyntaxException {
        if (baseURI == null || toBeRelativizedURI == null) {
            return null;
        }
        if (baseURI.isAbsolute() ^ toBeRelativizedURI.isAbsolute()) {
            return null;
        }
        final String base = baseURI.getSchemeSpecificPart();
        final String toBeRelativized = toBeRelativizedURI.getSchemeSpecificPart();
        final int l1 = base.length();
        final int l2 = toBeRelativized.length();
        if (l1 == 0) {
            return toBeRelativized;
        }
        int slashes = 0;
        StringBuilder sb = new StringBuilder();
        boolean differenceFound = false;
        for (int i = 0; i < l1; i++) {
            char c = base.charAt(i);
            if (i < l2) {
                if (!differenceFound && c == toBeRelativized.charAt(i)) {
                    sb.append(c);
                } else {
                    differenceFound = true;
                    if (c == '/') {
                        slashes++;
                    }
                }
            } else {
                if (c == '/') {
                    slashes++;
                }
            }
        }
        String rResolved = new URI(getRoot(sb.toString())).relativize(new URI(toBeRelativized)).toString();
        StringBuilder relativizedPath = new StringBuilder();
        for (int i = 0; i < slashes; i++) {
            relativizedPath.append("../");
        }
        relativizedPath.append(rResolved);
        return relativizedPath.toString();
    }

    private static String getRoot(String uri) {
        int idx = uri.lastIndexOf('/');
        if (idx == uri.length() - 1) {
            return uri;
        } else if (idx == -1) {
            return "";
        } else {
            return uri.substring(0, idx + 1);
        }
    }
}
