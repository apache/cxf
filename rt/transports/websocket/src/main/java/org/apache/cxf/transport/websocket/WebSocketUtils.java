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

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
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
    private static final String CRLF = "\r\n";
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

    public static byte[] buildResponse(Map<String, String> headers, byte[] data, int offset, int length) {
        StringBuilder sb = new StringBuilder();
        String v = headers.get(SC_KEY);
        sb.append(v == null ? DEFAULT_SC : v).append(CRLF);
        v = headers.get("Content-Type");
        if (v != null) {
            sb.append("Content-Type: ").append(v).append(CRLF);
        }
        sb.append(CRLF);
        
        byte[] hb = sb.toString().getBytes();
        byte[] longdata = new byte[hb.length + length];
        System.arraycopy(hb, 0, longdata, 0, hb.length);
        if (data != null && length > 0) {
            System.arraycopy(data, offset, longdata, hb.length, length);
        }
        return longdata;
    }

    public static byte[] buildResponse(byte[] data, int offset, int length) {
        byte[] longdata = new byte[length + 2];
        longdata[0] = 0x0d;
        longdata[1] = 0x0a;
        System.arraycopy(data, offset, longdata, 2, length);
        return longdata;
    }
    

}
