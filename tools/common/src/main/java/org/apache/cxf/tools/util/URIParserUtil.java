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

package org.apache.cxf.tools.util;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
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

    private static String combine(List r, char sep) {
        StringBuilder buf = new StringBuilder(r.get(0).toString());

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

    private static String escapeChars(String s) {
        StringBuilder b = new StringBuilder(s);
        int x = 0;
        do {
            char ch = b.charAt(x);
            if (isExcluded(ch)) {
                try {
                    byte[] bytes = Character.toString(ch).getBytes("UTF-8");
                    b.setCharAt(x++, '%');
                    for (int y = 0; y < bytes.length; y++) {
                        b.insert(x++, HEX_DIGITS.charAt((bytes[y] & 0xFF) >> 4));
                        b.insert(x, HEX_DIGITS.charAt(bytes[y] & 0x0F));
                    }
                } catch (UnsupportedEncodingException e) {
                    //should not happen
                }
            }
            x++;
        } while (x < b.length());
        return b.toString();
    }
    public static String normalize(final String uri) {
        URL url = null;
        try {
            url = new URL(uri);
            return escapeChars(url.toString().replace("\\", "/"));
        } catch (MalformedURLException e1) {
            try {
                if (uri.startsWith("classpath:")) {
                    
                    url = ClassLoaderUtils.getResource(uri.substring(10), URIParserUtil.class);
                    if (url != null) {
                        return url.toExternalForm();
                    }
                    return uri;
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
        }
    }

    public static String getAbsoluteURI(final String arg) {
        if (arg == null) {
            return null;
        }

        try {
            URL url = new URL(normalize(arg));
            if (url.toURI().isOpaque()
                && "file".equalsIgnoreCase(url.getProtocol())) {
                return new File("").toURI().resolve(url.getPath()).toString();
            } else {
                return normalize(arg);
            }
        } catch (MalformedURLException e1) {
            return normalize(arg);
        } catch (URISyntaxException e2) {
            return normalize(arg);
        }
    }
}
