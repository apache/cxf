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

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.cxf.helpers.JavaUtils;


public final class PackageUtils {

    private PackageUtils() {

    }

    static String getPackageName(String className) {
        int pos = className.lastIndexOf('.');
        if (pos != -1) {
            return className.substring(0, pos);
        }
        return "";
    }

    public static String getPackageName(Class<?> clazz) {
        String className = clazz.getName();
        if (className.startsWith("[L")) {
            className = className.substring(2);
        }
        return getPackageName(className);
    }

    public static String getSharedPackageName(List<Class<?>> classes) {
        if (classes.isEmpty()) {
            return "";
        }
        List<List<String>> lParts = new ArrayList<>(classes.size());
        List<String> currentParts = new ArrayList<>();
        for (Class<?> cls : classes) {
            if (!Proxy.isProxyClass(cls)) {
                lParts.add(Arrays.asList(getPackageName(cls).split("\\.")));
            }
        }
        for (int i = 0; i < lParts.get(0).size(); i++) {
            int j = 1;
            for (; j < lParts.size(); j++) {
                if (i > (lParts.get(j).size() - 1) || !lParts.get(j).get(i).equals(lParts.get(0).get(i))) {
                    break;
                }
            }
            if (j == lParts.size()) {
                currentParts.add(lParts.get(j - 1).get(i));
            } else {
                break;
            }
        }
        return String.join(".", currentParts);
    }

    public static String parsePackageName(String namespace, String defaultPackageName) {
        return (defaultPackageName != null && !defaultPackageName.trim().isEmpty())
            ? defaultPackageName : getPackageNameByNameSpaceURI(namespace.trim());
    }

    public static String getPackageNameByNameSpaceURI(String nameSpaceURI) {
        int idx = nameSpaceURI.indexOf(':');
        boolean urnScheme = false;
        if (idx >= 0) {
            final String scheme = nameSpaceURI.substring(0, idx);
            urnScheme = "urn".equalsIgnoreCase(scheme);
            if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme) || urnScheme) {
                nameSpaceURI = nameSpaceURI.substring(idx + (urnScheme ? 1 : 3)); //
            }
        }

        List<String> tokens = tokenize(nameSpaceURI, "/:");
        if (tokens.isEmpty()) {
            return null;
        }

        if (tokens.size() > 1) {
            String lastToken = tokens.get(tokens.size() - 1);
            idx = lastToken.lastIndexOf('.');
            if (idx > 0) {
                lastToken = lastToken.replace('.', '_');
                tokens.set(tokens.size() - 1, lastToken);
            }
        }

        String domain = tokens.remove(0);
        List<String> r = tokenize(domain, urnScheme ? ".-" : ".");
        Collections.reverse(r);
        if ("www".equalsIgnoreCase(r.get(r.size() - 1))) {
            // remove leading www
            r.remove(r.size() - 1);
        }

        // replace the domain name with tokenized items
        tokens.addAll(0, r);

        // iterate through the tokens and apply xml->java name algorithm
        for (int i = 0; i < tokens.size(); i++) {

            // get the token and remove illegal chars
            String token = tokens.get(i);
            token = removeIllegalIdentifierChars(token);

            token = token.toLowerCase();

            // this will check for reserved keywords
            if (JavaUtils.isJavaKeyword(token)) {
                token = '_' + token;
            }

            tokens.set(i, token);
        }

        // concat all the pieces and return it
        return String.join(".", tokens);
    }

    private static List<String> tokenize(String str, String sep) {
        StringTokenizer tokens = new StringTokenizer(str, sep);
        List<String> r = new ArrayList<>();

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
                newToken.append('_').append(c);
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

    public static String getNamespace(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return null;
        }
        final List<String> parts = Arrays.asList(packageName.split("\\."));
        Collections.reverse(parts);
        return "http://" + String.join(".", parts) + '/';
    }

}
