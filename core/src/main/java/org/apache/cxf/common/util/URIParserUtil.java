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
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.cxf.common.classloader.ClassLoaderUtils;

public final class URIParserUtil {
    private static final Set<String> KEYWORDS = new HashSet<String>(Arrays
        .asList(new String[] {"abstract", "boolean", "break", "byte", "case", "catch", "char", "class",
                              "const", "continue", "default", "do", "double", "else", "extends", "final",
                              "finally", "float", "for", "goto", "if", "implements", "import", "instanceof",
                              "int", "interface", "long", "native", "new", "package", "private", "protected",
                              "public", "return", "short", "static", "strictfp", "super", "switch",
                              "synchronized", "this", "throw", "throws", "transient", "try", "void",
                              "volatile", "while", "true", "false", "null", "assert", "enum"}));
    private static final String EXCLUDED_CHARS = "<>\"{}|\\^`";
    private static final String HEX_DIGITS = "0123456789abcdef";

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

    public static String parsePackageName(String namespace, String defaultPackageName) {
        String packageName = (defaultPackageName != null && defaultPackageName.trim().length() > 0)
            ? defaultPackageName : null;

        if (packageName == null) {
            packageName = getPackageName(namespace);
        }
        return packageName;
    }

    public static String getPackageName(String nameSpaceURI) {
        int idx = nameSpaceURI.indexOf(':');
        String scheme = "";
        if (idx >= 0) {
            scheme = nameSpaceURI.substring(0, idx);
            if ("http".equalsIgnoreCase(scheme) || "urn".equalsIgnoreCase(scheme)) {
                nameSpaceURI = nameSpaceURI.substring(idx + 1);
            }
        }

        List<String> tokens = tokenize(nameSpaceURI, "/: ");
        if (tokens.size() == 0) {
            return "cxf"; 
        }

        if (tokens.size() > 1) {
            String lastToken = tokens.get(tokens.size() - 1);
            idx = lastToken.lastIndexOf('.');
            if (idx > 0) {
                //lastToken = lastToken.substring(0, idx);
                lastToken = lastToken.replace('.', '_');
                tokens.set(tokens.size() - 1, lastToken);
            }
        }

        String domain = tokens.get(0);
        idx = domain.indexOf(':');
        if (idx >= 0) {
            domain = domain.substring(0, idx);
        }
        List<String> r = reverse(tokenize(domain, "urn".equals(scheme) ? ".-" : "."));
        if ("www".equalsIgnoreCase(r.get(r.size() - 1))) {
            // remove leading www
            r.remove(r.size() - 1);
        }

        // replace the domain name with tokenized items
        tokens.addAll(1, r);
        tokens.remove(0);

        // iterate through the tokens and apply xml->java name algorithm
        for (int i = 0; i < tokens.size(); i++) {

            // get the token and remove illegal chars
            String token = tokens.get(i);
            token = removeIllegalIdentifierChars(token);

            // this will check for reserved keywords
            if (containsReservedKeywords(token)) {
                token = '_' + token;
            }

            tokens.set(i, token.toLowerCase());
        }

        // concat all the pieces and return it
        return combine(tokens, '.');
    }

    public static String getNamespace(String packageName) {
        if (packageName == null || packageName.length() == 0) {
            return null;
        }
        StringTokenizer tokenizer = new StringTokenizer(packageName, ".");
        String[] tokens;
        if (tokenizer.countTokens() == 0) {
            tokens = new String[0];
        } else {
            tokens = new String[tokenizer.countTokens()];
            for (int i = tokenizer.countTokens() - 1; i >= 0; i--) {
                tokens[i] = tokenizer.nextToken();
            }
        }
        StringBuilder namespace = new StringBuilder("http://");
        String dot = "";
        for (int i = 0; i < tokens.length; i++) {
            if (i == 1) {
                dot = ".";
            }
            namespace.append(dot + tokens[i]);
        }
        namespace.append('/');
        return namespace.toString();
    }

    private static List<String> tokenize(String str, String sep) {
        StringTokenizer tokens = new StringTokenizer(str, sep);
        List<String> r = new ArrayList<String>();

        while (tokens.hasMoreTokens()) {
            r.add(tokens.nextToken());
        }
        return r;
    }

    private static String removeIllegalIdentifierChars(String token) {
        StringBuilder newToken = new StringBuilder();
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);

            if (i == 0 && !Character.isJavaIdentifierStart(c)) {
                // prefix an '_' if the first char is illegal
                newToken.append("_" + c);
            } else if (!Character.isJavaIdentifierPart(c)) {
                // replace the char with an '_' if it is illegal
                newToken.append('_');
            } else {
                // add the legal char
                newToken.append(c);
            }
        }
        return newToken.toString();
    }

    private static String combine(List<String> r, char sep) {
        StringBuilder buf = new StringBuilder(r.get(0));

        for (int i = 1; i < r.size(); i++) {
            buf.append(sep);
            buf.append(r.get(i));
        }

        return buf.toString();
    }

    private static <T> List<T> reverse(List<T> a) {
        List<T> r = new ArrayList<T>();

        for (int i = a.size() - 1; i >= 0; i--) {
            r.add(a.get(i));
        }
        return r;
    }

    public static boolean containsReservedKeywords(String token) {
        return KEYWORDS.contains(token);
    }

    public static String escapeChars(String s) {
        StringBuilder b = new StringBuilder(s.length());
        
        for (int x = 0; x < s.length(); x++) {
            char ch = s.charAt(x);
            if (isExcluded(ch)) {
                try {
                    byte[] bytes = Character.toString(ch).getBytes("UTF-8");
                    for (int y = 0; y < bytes.length; y++) {
                        b.append("%");
                        b.append(HEX_DIGITS.charAt((bytes[y] & 0xFF) >> 4));
                        b.append(HEX_DIGITS.charAt(bytes[y] & 0x0F));
                    }
                } catch (UnsupportedEncodingException e) {
                    //should not happen
                }
            } else {
                b.append(ch);
            }
        }
        return b.toString();
    }
    public static String normalize(final String uri) {
        URL url = null;
        String result = null;
        try {
            url = new URL(uri);
            result = escapeChars(url.toURI().normalize().toString().replace("\\", "/"));
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
                String f = null;
                if (uri.indexOf(":") != -1 && !uri.startsWith("/")) {
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
            } else {
                return normalize(arg);
            }
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
     * @param baseURI           The base URI
     * @param toBeRelativizedURI The URI to be realivized
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
