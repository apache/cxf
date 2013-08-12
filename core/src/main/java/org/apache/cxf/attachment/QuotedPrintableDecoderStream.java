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
package org.apache.cxf.attachment;

import java.io.IOException;
import java.io.InputStream;

public class QuotedPrintableDecoderStream extends InputStream {
    private static final byte[] ENCODING_TABLE = {
        (byte)'0', (byte)'1', (byte)'2', (byte)'3', (byte)'4', (byte)'5', (byte)'6', (byte)'7', (byte)'8',
        (byte)'9', (byte)'A', (byte)'B', (byte)'C', (byte)'D', (byte)'E', (byte)'F'
    };

    /*
     * set up the decoding table.
     */
    private static final byte[] DECODING_TABLE = new byte[128];

    static {
        // initialize the decoding table
        for (int i = 0; i < ENCODING_TABLE.length; i++) {
            DECODING_TABLE[ENCODING_TABLE[i]] = (byte)i;
        }
    }

    private int deferredWhitespace;
    private int cachedCharacter = -1;
    private final InputStream in;

    public QuotedPrintableDecoderStream(InputStream is) {
        this.in = is;
    }

    private int decodeNonspaceChar(int ch) throws IOException {
        if (ch != '=') {
            return ch;
        }
        // we need to get two characters after the quotation marker
        byte[] b = new byte[2];
        if (in.read(b) < 2) {
            throw new IOException("Truncated quoted printable data");
        }
        if (b[0] == '\r') {
            // we've found an encoded carriage return. The next char needs to be a newline
            if (b[1] != '\n') {
                throw new IOException("Invalid quoted printable encoding");
            }
            // this was a soft linebreak inserted by the encoding. We just toss this away
            // on decode. We need to return something, so recurse and decode the next.
            return read();
        }
        // this is a hex pair we need to convert back to a single byte.
        b[0] = DECODING_TABLE[b[0]];
        b[1] = DECODING_TABLE[b[1]];
        return (b[0] << 4) | b[1];
    }

    @Override
    public int read() throws IOException {
        // we potentially need to scan over spans of whitespace characters to determine if they're real
        // we just return blanks until the count goes to zero.
        if (deferredWhitespace > 0) {
            deferredWhitespace--;
            return ' ';
        }
        // we may have needed to scan ahead to find the first non-blank character, which we would store here.
        // hand that back once we're done with the blanks.
        if (cachedCharacter != -1) {
            int result = cachedCharacter;
            cachedCharacter = -1;
            return result;
        }
        int ch = in.read();
        if (ch != ' ') {
            return decodeNonspaceChar(ch);
        }
        // space characters are a pain. We need to scan ahead until we find a non-space character.
        // if the character is a line terminator, we need to discard the blanks.
        // scan forward, counting the characters.
        while ((ch = in.read()) == ' ') {
            deferredWhitespace++;
        }
        // is this a lineend at the current location?
        if (ch == -1 || ch == '\r' || ch == '\n') {
            // those blanks we so zealously counted up don't really exist. Clear out the counter.
            deferredWhitespace = 0;
            // return the real significant character now.
            return ch;
        }
        // remember this character for later, after we've used up the deferred blanks.
        cachedCharacter = decodeNonspaceChar(ch);
        // return this space. We did not include this one in the deferred count, so we're right in sync.
        return ' ';
    }
}
