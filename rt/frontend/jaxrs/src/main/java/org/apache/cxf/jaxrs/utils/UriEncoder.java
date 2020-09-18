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

package org.apache.cxf.jaxrs.utils;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * URI Encoding and Decoding
 */
public final class UriEncoder {

    private static final Charset CHARSET_UTF_8 = Charset.forName("UTF-8"); //$NON-NLS-1$

    /** Hexadecimal digits for escaping. */
    private static final char[]    HEX_DIGITS            =
    {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    private static final byte[]    NORMALIZED_HEX_DIGITS = new byte[128];

    private static final boolean[] IS_HEX_DIGIT          = new boolean[128];

    /**
     * Unreserved characters according to RFC 3986. Each character below ASCII
     * 128 has single array item with true if it is unreserved and false if it
     * is reserved.
     */
    private static final boolean[]  UNRESERVED_CHARS     = new boolean[128];
    private static final boolean[]  USER_INFO_CHARS      = new boolean[128];
    private static final boolean[]  SEGMENT_CHARS        = new boolean[128];
    private static final boolean[]  MATRIX_CHARS         = new boolean[128];
    private static final boolean[]  PATH_CHARS           = new boolean[128];
    private static final boolean[]  QUERY_CHARS          = new boolean[128];
    private static final boolean[]  QUERY_PARAM_CHARS    = new boolean[128];
    private static final boolean[]  FRAGMENT_CHARS       = new boolean[128];
    private static final boolean[]  URI_CHARS            = new boolean[128];
    private static final boolean[]  URI_TEMPLATE_CHARS   = new boolean[128];

    static {
        // unreserved - ALPHA / DIGIT / "-" / "." / "_" / "~"
        Arrays.fill(UNRESERVED_CHARS, false);
        Arrays.fill(UNRESERVED_CHARS, 'a', 'z' + 1, true);
        Arrays.fill(UNRESERVED_CHARS, 'A', 'Z' + 1, true);
        Arrays.fill(UNRESERVED_CHARS, '0', '9' + 1, true);
        UNRESERVED_CHARS['-'] = true;
        UNRESERVED_CHARS['_'] = true;
        UNRESERVED_CHARS['.'] = true;
        UNRESERVED_CHARS['~'] = true;

        // sub delimiters - "!" / "$" / "&" / "'" / "(" / ")" / "*" / "+" / ","
        // / ";" / "="
        // user info chars - *( unreserved / pct-encoded / sub-delims / ":" )
        System.arraycopy(UNRESERVED_CHARS, 0, USER_INFO_CHARS, 0, 128);
        USER_INFO_CHARS['!'] = true;
        USER_INFO_CHARS['$'] = true;
        USER_INFO_CHARS['&'] = true;
        USER_INFO_CHARS['\''] = true;
        USER_INFO_CHARS['('] = true;
        USER_INFO_CHARS[')'] = true;
        USER_INFO_CHARS['*'] = true;
        USER_INFO_CHARS['+'] = true;
        USER_INFO_CHARS[','] = true;
        USER_INFO_CHARS[';'] = true;
        USER_INFO_CHARS['='] = true;
        USER_INFO_CHARS[':'] = true;

        // segment - *(unreserved / pct-encoded / sub-delims / ":" / "@")
        System.arraycopy(USER_INFO_CHARS, 0, SEGMENT_CHARS, 0, 128);
        SEGMENT_CHARS['@'] = true;

        // matrix - *(unreserved / pct-encoded / sub-delims / ":" / "@") without
        // "=" and ";"
        System.arraycopy(SEGMENT_CHARS, 0, MATRIX_CHARS, 0, 128);
        MATRIX_CHARS['='] = false;
        MATRIX_CHARS[';'] = false;

        // path - *(unreserved / pct-encoded / sub-delims / ":" / "@" / "/")
        System.arraycopy(SEGMENT_CHARS, 0, PATH_CHARS, 0, 128);
        PATH_CHARS['/'] = true;

        // query - *(unreserved / pct-encoded / sub-delims / ":" / "@" / "/" /
        // "?")
        System.arraycopy(PATH_CHARS, 0, QUERY_CHARS, 0, 128);
        QUERY_CHARS['?'] = true;

        // fragment - *(unreserved / pct-encoded / sub-delims / ":" / "@" / "/"
        // / "?")
        System.arraycopy(QUERY_CHARS, 0, FRAGMENT_CHARS, 0, 128);

        // query param - *(unreserved / pct-encoded / sub-delims / ":" / "@" /
        // "/" / "?") without
        // "&" and "="
        System.arraycopy(QUERY_CHARS, 0, QUERY_PARAM_CHARS, 0, 128);
        QUERY_PARAM_CHARS['&'] = false;
        QUERY_PARAM_CHARS['='] = false;

        // uri - *(unreserved / pct-encoded / sub-delims / ":" / "@" / "/" / "?"
        // / "#" / "[" / "]" )
        System.arraycopy(QUERY_CHARS, 0, URI_CHARS, 0, 128);
        URI_CHARS['#'] = true;
        URI_CHARS['['] = true;
        URI_CHARS[']'] = true;

        // uri template - *(unreserved / pct-encoded / sub-delims / ":" / "@" /
        // "/" / "?" / "#" /
        // "[" / "]" / "{" / "}" )
        System.arraycopy(URI_CHARS, 0, URI_TEMPLATE_CHARS, 0, 128);
        URI_TEMPLATE_CHARS['{'] = true;
        URI_TEMPLATE_CHARS['}'] = true;

        // fill the isHex array
        Arrays.fill(IS_HEX_DIGIT, false);
        Arrays.fill(IS_HEX_DIGIT, '0', '9' + 1, true);
        Arrays.fill(IS_HEX_DIGIT, 'a', 'f' + 1, true);
        Arrays.fill(IS_HEX_DIGIT, 'A', 'F' + 1, true);

        // fill the NORMALIZED_HEX_DIGITS array
        NORMALIZED_HEX_DIGITS['0'] = '0';
        NORMALIZED_HEX_DIGITS['1'] = '1';
        NORMALIZED_HEX_DIGITS['2'] = '2';
        NORMALIZED_HEX_DIGITS['3'] = '3';
        NORMALIZED_HEX_DIGITS['4'] = '4';
        NORMALIZED_HEX_DIGITS['5'] = '5';
        NORMALIZED_HEX_DIGITS['6'] = '6';
        NORMALIZED_HEX_DIGITS['7'] = '7';
        NORMALIZED_HEX_DIGITS['8'] = '8';
        NORMALIZED_HEX_DIGITS['9'] = '9';
        NORMALIZED_HEX_DIGITS['A'] = 'A';
        NORMALIZED_HEX_DIGITS['B'] = 'B';
        NORMALIZED_HEX_DIGITS['C'] = 'C';
        NORMALIZED_HEX_DIGITS['D'] = 'D';
        NORMALIZED_HEX_DIGITS['E'] = 'E';
        NORMALIZED_HEX_DIGITS['F'] = 'F';
        NORMALIZED_HEX_DIGITS['a'] = 'A';
        NORMALIZED_HEX_DIGITS['b'] = 'B';
        NORMALIZED_HEX_DIGITS['c'] = 'C';
        NORMALIZED_HEX_DIGITS['d'] = 'D';
        NORMALIZED_HEX_DIGITS['e'] = 'E';
        NORMALIZED_HEX_DIGITS['f'] = 'F';
    }

    private UriEncoder() {
        // no instances
    }

    private static int decodeHexDigit(char c) {
        // Decode single hexadecimal digit. On error returns 0 (ignores errors).
        if (c >= '0' && c <= '9') {
            return c - '0';
        } else if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        } else if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        } else {
            return 0;
        }
    }

    /**
     * Encode all characters other than unreserved according to <a
     * href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>.
     * 
     * @param string string to encode
     * @return encoded US-ASCII string
     */
    public static String encodeString(String string) {
        return encode(string, false, UNRESERVED_CHARS);
    }

    /**
     * Encode user info according to <a
     * href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>.
     * 
     * @param userInfo the user info to encode
     * @param relax if true, then any sequence of chars in the input string that
     *            have the form '%XX', where XX are two HEX digits, will not be
     *            encoded
     * @return encoded user info string
     */
    public static String encodeUserInfo(String userInfo, boolean relax) {
        return encode(userInfo, relax, USER_INFO_CHARS);
    }

    /**
     * Encode a path segment (without matrix parameters) according to <a
     * href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>.
     * 
     * @param segment the segment (without matrix parameters) to encode
     * @param relax if true, then any sequence of chars in the input string that
     *            have the form '%XX', where XX are two HEX digits, will not be
     *            encoded
     * @return encoded segment string
     */
    public static String encodePathSegment(String segment, boolean relax) {
        return encode(segment, relax, SEGMENT_CHARS);
    }

    /**
     * Encode a matrix parameter (name or value) according to <a
     * href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>.
     * 
     * @param matrix the matrix parameter (name or value) to encode
     * @param relax if true, then any sequence of chars in the input string that
     *            have the form '%XX', where XX are two HEX digits, will not be
     *            encoded
     * @return encoded matrix string
     */
    public static String encodeMatrix(String matrix, boolean relax) {
        return encode(matrix, relax, MATRIX_CHARS);
    }

    /**
     * Encode a complete path string according to <a
     * href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>.
     * 
     * @param path the path string to encode
     * @param relax if true, then any sequence of chars in the input string that
     *            have the form '%XX', where XX are two HEX digits, will not be
     *            encoded
     * @return encoded path string
     */
    public static String encodePath(String path, boolean relax) {
        return encode(path, relax, PATH_CHARS);
    }

    /**
     * Encode a query parameter (name or value) according to <a
     * href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>.
     * 
     * @param queryParam the query parameter string to encode
     * @param relax if true, then any sequence of chars in the input string that
     *            have the form '%XX', where XX are two HEX digits, will not be
     *            encoded
     * @return encoded query parameter string
     */
    public static String encodeQueryParam(String queryParam, boolean relax) {
        boolean[] unreserved = QUERY_PARAM_CHARS;
        String string = queryParam;

        if (queryParam == null) {
            return null;
        }

        if (!needsEncoding(queryParam, false, unreserved)) {
            return string;
        }

        // Encode to UTF-8
        ByteBuffer buffer = CHARSET_UTF_8.encode(string);
        // Prepare string buffer
        StringBuilder sb = new StringBuilder(buffer.remaining());
        // Now encode the characters
        while (buffer.hasRemaining()) {
            int c = buffer.get();

            if ((c == '%') && relax && (buffer.remaining() >= 2)) {
                int position = buffer.position();
                if (isHex(buffer.get(position)) && isHex(buffer.get(position + 1))) {
                    sb.append((char)c);
                    continue;
                }
            }

            if (c >= ' ' && unreserved[c]) {
                sb.append((char)c);
            } else if (c == ' ') {
                sb.append('+');
            } else {
                sb.append('%');
                sb.append(HEX_DIGITS[(c & 0xf0) >> 4]);
                sb.append(HEX_DIGITS[c & 0xf]);
            }
        }

        return sb.toString();
    }

    /**
     * Encode a complete query string according to <a
     * href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>.
     * 
     * @param query the query string to encode
     * @param relax if true, then any sequence of chars in the input string that
     *            have the form '%XX', where XX are two HEX digits, will not be
     *            encoded
     * @return encoded query string
     */
    public static String encodeQuery(String query, boolean relax) {
        return encode(query, relax, QUERY_CHARS);
    }

    /**
     * Encode a fragment string according to <a
     * href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>.
     * 
     * @param fragment the fragment string to encode
     * @param relax if true, then any sequence of chars in the input string that
     *            have the form '%XX', where XX are two HEX digits, will not be
     *            encoded
     * @return encoded fragment string
     */
    public static String encodeFragment(String fragment, boolean relax) {
        return encode(fragment, relax, FRAGMENT_CHARS);
    }

    /**
     * Encode a uri according to <a
     * href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>, escaping all
     * reserved characters.
     * 
     * @param uri string to encode
     * @param relax if true, then any sequence of chars in the input of the form
     *            '%XX', where XX are two HEX digits, will not be encoded.
     * @return encoded US-ASCII string
     */
    public static String encodeUri(String uri, boolean relax) {
        return encode(uri, relax, URI_CHARS);
    }

    /**
     * Encode a uri template according to <a
     * href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>, escaping all
     * reserved characters, except for '{' and '}'.
     * 
     * @param uriTemplate template to encode
     * @param relax if true, then any sequence of chars in the input of the form
     *            '%XX', where XX are two HEX digits, will not be encoded.
     * @return encoded US-ASCII string
     */
    public static String encodeUriTemplate(String uriTemplate, boolean relax) {
        return encode(uriTemplate, relax, URI_TEMPLATE_CHARS);
    }

    /**
     * Encode a string according to <a
     * href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>, escaping all
     * characters where <code>unreserved[char] == false</code>, where
     * <code>char</code> is a single character such as 'a'.
     * 
     * @param string string to encode
     * @param relax if true, then any sequence of chars in the input string that
     *            have the form '%XX', where XX are two HEX digits, will not be
     *            encoded.
     * @param unreserved an array of booleans that indicates which characters
     *            are considered unreserved. a character is considered
     *            unreserved if <code>unreserved[char] == true</code>, in which
     *            case it will not be encoded
     * @return encoded US-ASCII string
     */
    private static String encode(String string, boolean relax, boolean[] unreserved) {
        if (string == null) {
            return null;
        }

        if (!needsEncoding(string, false, unreserved)) {
            return string;
        }

        // Encode to UTF-8
        ByteBuffer buffer = CHARSET_UTF_8.encode(string);
        // Prepare string buffer
        StringBuilder sb = new StringBuilder(buffer.remaining());
        // Now encode the characters
        while (buffer.hasRemaining()) {
            int c = buffer.get();

            if ((c == '%') && relax && (buffer.remaining() >= 2)) {
                int position = buffer.position();
                if (isHex(buffer.get(position)) && isHex(buffer.get(position + 1))) {
                    sb.append((char)c);
                    continue;
                }
            }

            if (c >= ' ' && unreserved[c]) {
                sb.append((char)c);
            } else {
                sb.append('%');
                sb.append(HEX_DIGITS[(c & 0xf0) >> 4]);
                sb.append(HEX_DIGITS[c & 0xf]);
            }
        }

        return sb.toString();
    }

    private static boolean isHex(int c) {
        return IS_HEX_DIGIT[c];
    }

    /**
     * Determines if the input string contains any invalid URI characters that
     * require encoding
     * 
     * @param uri the string to test
     * @return true if the the input string contains only valid URI characters
     */
    private static boolean needsEncoding(String s, boolean relax, boolean[] unreserved) {
        int len = s.length();
        for (int i = 0; i < len; ++i) {
            char c = s.charAt(i);
            if (c == '%' && relax) {
                continue;
            }
            if (c > unreserved.length || !unreserved[c]) {
                return true;
            }
        }
        return false;
    }

    /**
     * Decode US-ASCII uri according to <a
     * href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a> and replaces all
     * occurrences of the '+' sign with spaces.
     * 
     * @param string query string to decode
     * @return decoded query
     */
    public static String decodeQuery(String string) {
        return decodeString(string, true, null);
    }

    /**
     * Decode US-ASCII uri according to <a
     * href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>.
     * 
     * @param string US-ASCII uri to decode
     * @return decoded uri
     */
    public static String decodeString(String string) {
        return decodeString(string, false, null);
    }

    /**
     * Decodes only the unreserved chars, according to <a
     * href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a> section 6.2.2.2
     * 
     * @param string US-ASCII uri to decode
     * @return decoded uri
     */
    public static String normalize(String string) {
        return decodeString(string, false, UNRESERVED_CHARS);
    }

    private static String decodeString(String string, boolean query, boolean[] decodeChars) {
        if (string == null) {
            return null;
        }

        if (!needsDecoding(string, query)) {
            return string;
        }

        int len = string.length();
        // Prepare byte buffer
        ByteBuffer buffer = ByteBuffer.allocate(len);
        // decode string into byte buffer
        for (int i = 0; i < len; ++i) {
            char c = string.charAt(i);
            if (c == '%' && (i + 2 < len)) {
                int v = 0;
                int d1 = decodeHexDigit(string.charAt(i + 1));
                int d2 = decodeHexDigit(string.charAt(i + 2));
                if (d1 >= 0 && d2 >= 0) {
                    v = d1;
                    v = v << 4 | d2;
                    if (decodeChars != null && (v >= decodeChars.length || !decodeChars[v])) {
                        buffer.put((byte)string.charAt(i));
                        buffer.put(NORMALIZED_HEX_DIGITS[string.charAt(i + 1)]);
                        buffer.put(NORMALIZED_HEX_DIGITS[string.charAt(i + 2)]);
                    } else {
                        buffer.put((byte)v);
                    }
                    i += 2;
                } else {
                    buffer.put((byte)c);
                }
            } else {
                if (query && c == '+') {
                    c = ' ';
                }
                buffer.put((byte)c);
            }
        }
        // Decode byte buffer from UTF-8
        buffer.flip();
        return CHARSET_UTF_8.decode(buffer).toString();
    }

    private static boolean needsDecoding(String s, boolean query) {
        boolean needs = s.indexOf('%') != -1;
        if (!needs && query) {
            needs = s.indexOf('+') != -1;
        }
        return needs;
    }
}
