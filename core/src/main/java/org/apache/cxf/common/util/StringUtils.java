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

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringUtils {

    private static final Predicate<String> NOT_EMPTY = (String s) -> !s.isEmpty();

    private static final char[] HEX = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private StringUtils() {
    }

    public static boolean isEmpty(String str) {
        if (str != null) {
            int len = str.length();
            for (int x = 0; x < len; ++x) {
                if (str.charAt(x) > ' ') {
                    return false;
                }
            }
        }
        return true;
    }

    public static Predicate<String> notEmpty() {
        return NOT_EMPTY;
    }

    public static boolean isEmpty(List<String> list) {
        if (list == null || list.isEmpty()) {
            return true;
        }
        return list.size() == 1 && isEmpty(list.get(0));
    }

    public static String getFirstFound(String contents, String regex) {
        if (isEmpty(regex) || isEmpty(contents)) {
            return null;
        }
        Pattern pattern = Pattern.compile(regex, Pattern.UNICODE_CASE);
        Matcher matcher = pattern.matcher(contents);

        if (matcher.find()) {
            if (matcher.groupCount() > 0) {
                return matcher.group(1);
            } else {
                return matcher.group();
            }
        }
        return null;
    }

    public static String addDefaultPortIfMissing(String urlString) {
        return addDefaultPortIfMissing(urlString, "80");
    }

    public static String addDefaultPortIfMissing(String urlString, String defaultPort) {
        try {
            if (new URL(urlString).getPort() != -1) {
                return urlString;
            }
        } catch (MalformedURLException e) {
            return urlString;
        }
        String regex = "http://([^/]+)";
        String found = StringUtils.getFirstFound(urlString, regex);

        if (!StringUtils.isEmpty(found)) {
            String replacer = "http://" + found + ':' + defaultPort;
            return urlString.replaceFirst(regex, replacer);
        }
        return urlString;
    }

    /**
     * Return input string with first character in upper case.
     * @param name input string.
     * @return capitalized form.
     */
    public static String capitalize(String name) {
        return changeFirstCharacterCase(name, true);
    }

    public static String uncapitalize(String str) {
        return changeFirstCharacterCase(str, false);
    }

    private static String changeFirstCharacterCase(String str, boolean capitalize) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        char baseChar = str.charAt(0);
        char updatedChar = capitalize ? Character.toUpperCase(baseChar) : Character.toLowerCase(baseChar);
        if (baseChar == updatedChar) {
            return str;
        }
        char[] chars = str.toCharArray();
        chars[0] = updatedChar;
        return new String(chars);
    }

    public static byte[] toBytesUTF8(String str) {
        return toBytes(str, StandardCharsets.UTF_8.name());
    }
    public static byte[] toBytesASCII(String str) {
        return toBytes(str, "US-ASCII");
    }
    public static byte[] toBytes(String str, String enc) {
        try {
            return str.getBytes(enc);
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String toHexString(byte[] bytes) {
        final StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            byteToHex(b, sb);
        }
        return sb.toString();
    }

    static void byteToHex(byte b, StringBuilder sb) {
        sb.append(HEX[(0xF0 & b) >> 4]);
        sb.append(HEX[0x0F & b]);
    }

    public static String periodToSlashes(String s) {
        char[] ch = s.toCharArray();
        for (int x = 0; x < ch.length; x++) {
            if (ch[x] == '.') {
                ch[x] = '/';
            }
        }
        return new String(ch);
    }
    public static String slashesToPeriod(String s) {
        return s.replace('/', '.');
    }

}
