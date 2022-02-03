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

package org.apache.cxf.transport.websocket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 *
 */
public final class WebSocketUtils {
    public static final String URI_KEY = "$uri";
    public static final String METHOD_KEY = "$method";
    public static final String SC_KEY = "$sc";
    public static final String FLUSHED_KEY = "$flushed";

    private static final byte[] CRLF = "\r\n".getBytes();
    private static final byte[] COLSP = ": ".getBytes();
    private static final Pattern CR_OR_LF = Pattern.compile("\\r|\\n");

    private WebSocketUtils() {
    }

    /**
     * Read header properties from the specified input stream.
     *
     * Only a restricted syntax is allowed as the syntax is in our control.
     * Not allowed are:
     * - multiline or line-wrapped headers are not not
     * - charset other than utf-8. (although i would have preferred iso-8859-1 ;-)
     *
     * @param in the input stream
     * @param req true if the input stream includes the request line
     * @return a map of name value pairs.
     * @throws IOException
     */
    public static Map<String, String> readHeaders(InputStream in, boolean req) throws IOException {
        Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        String line;
        int del;
        if (req) {
            // read the request line
            line = readLine(in);
            del = line.indexOf(' ');
            if (del < 0) {
                throw new IOException("invalid request: " + line);
            }
            headers.put(METHOD_KEY, line.substring(0, del).trim());
            headers.put(URI_KEY, line.substring(del + 1).trim());
        }

        // read headers
        while ((line = readLine(in)) != null) {
            if (line.length() > 0) {
                del = line.indexOf(':');
                if (del < 0) {
                    headers.put(line.trim(), "");
                } else {
                    headers.put(line.substring(0, del).trim(), line.substring(del + 1).trim());
                }
            }
        }

        return headers;
    }

    public static Map<String, String> readHeaders(InputStream in) throws IOException {
        return readHeaders(in, true);
    }


    /**
     * Read a line terminated by '\n' optionally preceded by '\r' from the
     * specified input stream.
     * @param in the input stream
     * @return
     * @throws IOException
     */
    // this is copied from AttachmentDeserializer with a minor change to restrict the line termination rule.
    public static String readLine(InputStream in) throws IOException {
        StringBuilder buffer = new StringBuilder(128);

        int c;

        while ((c = in.read()) != -1) {
            // a linefeed is a terminator, always.
            if (c == '\n') {
                break;
            } else if (c == '\r') {
                //just ignore the CR.  The next character SHOULD be an NL.  If not, we're
                //just going to discard this
                continue;
            } else {
                // just add to the buffer
                buffer.append((char)c);
            }
        }

        // no characters found...this was either an eof or a null line.
        if (buffer.length() == 0) {
            return null;
        }

        return buffer.toString();
    }

    public static byte[] readBody(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        for (int n = in.read(buf); n > -1; n = in.read(buf)) {
            baos.write(buf, 0, n);
        }
        return baos.toByteArray();
    }

    /**
     * Build response bytes with the status and type information specified in the headers.
     *
     * @param headers
     * @param data
     * @param offset
     * @param length
     * @return
     */
    public static byte[] buildResponse(Map<String, String> headers, byte[] data, int offset, int length) {
        ByteArrayBuilder sb = new ByteArrayBuilder();
        String v = headers.get(SC_KEY);
        if (v != null) {
            sb.append(v).append(CRLF);
        }
        sb.append(headers);

        if (data != null && length > 0) {
            sb.append(CRLF).append(data, offset, length);
        }
        return sb.toByteArray();
    }

    /**
     * Build response bytes with some generated headers.
     *
     * @param headers
     * @param data
     * @param offset
     * @param length
     * @return
     */
    public static byte[] buildResponse(byte[] headers, byte[] data, int offset, int length) {
        final int hlen = headers != null ? headers.length : 0;
        byte[] longdata = new byte[Math.addExact(length, hlen) + 2];

        if (hlen > 0) {
            System.arraycopy(headers, 0, longdata, 0, hlen);
        }
        if (data != null && length > 0) {
            System.arraycopy(CRLF, 0, longdata, hlen, CRLF.length);
            System.arraycopy(data, offset, longdata, hlen + CRLF.length, length);
        }
        return longdata;
    }

    /**
     * Build response bytes without status and type information.
     *
     * @param data
     * @param offset
     * @param length
     * @return
     */
    public static byte[] buildResponse(byte[] data, int offset, int length) {
        return buildResponse((byte[])null, data, offset, length);
    }

    public static byte[] buildHeaderLine(String name, String value) {
        byte[] hl = new byte[name.length() + COLSP.length + value.length() + CRLF.length];
        System.arraycopy(name.getBytes(), 0, hl, 0, name.length());
        System.arraycopy(COLSP, 0, hl, name.length(), COLSP.length);
        System.arraycopy(value.getBytes(), 0, hl, name.length() + COLSP.length, value.length());
        System.arraycopy(CRLF, 0, hl, name.length() + COLSP.length + value.length(), CRLF.length);
        return hl;
    }

    /**
     * Build request bytes with the specified method, url, headers, and content entity.
     *
     * @param method
     * @param url
     * @param headers
     * @param data
     * @param offset
     * @param length
     * @return
     */
    public static byte[] buildRequest(String method, String url, Map<String, String> headers,
                                      byte[] data, int offset, int length) {
        ByteArrayBuilder sb = new ByteArrayBuilder();
        sb.append(method).append(' ').append(url).append(CRLF).append(headers);

        if (data != null && length > 0) {
            sb.append(CRLF).append(data, offset, length);
        }
        return sb.toByteArray();
    }

    public static boolean isContainingCRLF(String value) {
        return CR_OR_LF.matcher(value).find();
    }

    private static class ByteArrayBuilder {
        private ByteArrayOutputStream baos;
        ByteArrayBuilder() {
            baos = new ByteArrayOutputStream();
        }

        public ByteArrayBuilder append(byte[] b) {
            try {
                baos.write(b);
            } catch (IOException e) {
                // ignore;
            }
            return this;
        }

        public ByteArrayBuilder append(byte[] b, int offset, int length) {
            baos.write(b, offset, length);
            return this;
        }

        public ByteArrayBuilder append(String s) {
            try {
                baos.write(s.getBytes("utf-8"));
            } catch (IOException e) {
                // ignore
            }
            return this;
        }

        public ByteArrayBuilder append(int c) {
            baos.write(c);
            return this;
        }

        public ByteArrayBuilder append(Map<String, String> map) {
            for (Entry<String, String> m : map.entrySet()) {
                if (!m.getKey().startsWith("$")) {
                    append(m.getKey()).append(COLSP).append(m.getValue()).append(CRLF);
                }
            }
            return this;
        }

        public byte[] toByteArray() {
            return baos.toByteArray();
        }
    }
}
