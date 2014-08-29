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

/**
 * 
 */
public final class WebSocketUtils {
    static final String URI_KEY = "$uri";
    static final String METHOD_KEY = "$method";
    static final String SC_KEY = "$sc";
    static final String SM_KEY = "$sm";
    static final String FLUSHED_KEY = "$flushed";
    private static final byte[] CRLF = "\r\n".getBytes();
    private static final byte[] COLSP = ": ".getBytes();
    private static final String DEFAULT_SC = "200";

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
     * @return a map of name value pairs.
     * @throws IOException
     */
    public static Map<String, String> readHeaders(InputStream in) throws IOException {
        Map<String, String> headers = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        // read the request line
        String line = readLine(in);
        int del = line.indexOf(' ');
        if (del < 0) {
            throw new IOException("invalid request: " + line);
        }
        headers.put(METHOD_KEY, line.substring(0, del).trim());
        headers.put(URI_KEY, line.substring(del + 1).trim());
        
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
        sb.append(v == null ? DEFAULT_SC : v).append(CRLF);
        appendHeaders(headers, sb);
        
        byte[] longdata = sb.toByteArray();
        if (data != null && length > 0) {
            longdata = buildResponse(longdata, data, offset, length);
        }
        return longdata;
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
        byte[] longdata = new byte[length + 2 + hlen];

        if (hlen > 0) {
            System.arraycopy(headers, 0, longdata, 0, hlen);
        }
        longdata[hlen] = 0x0d;
        longdata[hlen + 1] = 0x0a;
        System.arraycopy(data, offset, longdata, hlen + 2, length);
        return longdata;
    }

    /**
     * Build response bytes without status and type information.
     *
     * @param headers
     * @param data
     * @param offset
     * @param length
     * @return
     */
    public static byte[] buildResponse(byte[] data, int offset, int length) {
        return buildResponse((byte[])null, data, offset, length);
    }
    
    static byte[] buildHeaderLine(String name, String value) {
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
        sb.append(method).append(' ').append(url).append(CRLF);
        appendHeaders(headers, sb);
        sb.append(CRLF);

        byte[] longdata = sb.toByteArray();
        if (data != null && length > 0) {
            final byte[] hb = longdata;
            longdata = new byte[hb.length + length];
            System.arraycopy(hb, 0, longdata, 0, hb.length);
            System.arraycopy(data, offset, longdata, hb.length, length);
        }
        return longdata;
    }

    private static void appendHeaders(Map<String, String> headers, ByteArrayBuilder sb) {
        for (Entry<String, String> header : headers.entrySet()) {
            if (!header.getKey().startsWith("$")) {
                sb.append(header.getKey()).append(COLSP).append(header.getValue()).append(CRLF);
            }
        }
    }
    
    private static class ByteArrayBuilder {
        private ByteArrayOutputStream baos;
        public ByteArrayBuilder() {
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
        
        public ByteArrayBuilder append(String s) {
            try {
                baos.write(s.getBytes());
            } catch (IOException e) {
                // ignore
            }
            return this;
        }
        
        public ByteArrayBuilder append(char c) {
            baos.write(c);
            return this;
        }
        
        public byte[] toByteArray() {
            return baos.toByteArray();
        }
    }
}
