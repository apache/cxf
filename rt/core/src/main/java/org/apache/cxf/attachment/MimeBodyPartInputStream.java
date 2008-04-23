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
import java.io.PushbackInputStream;

public class MimeBodyPartInputStream extends InputStream {

    PushbackInputStream inStream;

    boolean boundaryFound;

    byte[] boundary;

    public MimeBodyPartInputStream(PushbackInputStream inStreamParam, byte[] boundaryParam) {
        super();
        this.inStream = inStreamParam;
        this.boundary = boundaryParam;
    }

    public int read() throws IOException {
        boolean needUnread0d0a = false;
        if (boundaryFound) {
            return -1;
        }

        // read the next value from stream
        int value = inStream.read();
        // A problem occurred because all the mime parts tends to have a /r/n
        // at the end. Making it hard to transform them to correct
        // DataSources.
        // This logic introduced to handle it
        if (value == 13) {
            value = inStream.read();
            if (value != 10) {
                inStream.unread(value);
                return 13;
            } else {
                value = inStream.read();
                if ((byte) value != boundary[0]) {
                    inStream.unread(value);
                    inStream.unread(10);
                    return 13;
                } else {
                    needUnread0d0a = true;
                }
            }
        } else if ((byte) value != boundary[0]) {
            return value;
        }
        // read value is the first byte of the boundary. Start matching the
        // next characters to find a boundary
        int boundaryIndex = 0;
        while ((boundaryIndex < boundary.length) && ((byte) value == boundary[boundaryIndex])) {
            value = inStream.read();
            boundaryIndex++;
        }
        if (boundaryIndex == boundary.length) {
            // boundary found
            boundaryFound = true;
            int dashNext = inStream.read();
            // read the end of line character
            if (dashNext == 45 && value == 45) {
                // Last mime boundary should have a succeeding "--"
                // as we are on it, read the terminating CRLF
                inStream.read();
                inStream.read();
            }
            return -1;
        }
        // Boundary not found. Restoring bytes skipped.
        // write first skipped byte, push back the rest
        if (value != -1) {
            // Stream might have ended
            inStream.unread(value);
        }
        if (needUnread0d0a) {
            inStream.unread(boundary, 0, boundaryIndex);
            inStream.unread(10);
            value = 13;
        } else {
            inStream.unread(boundary, 1, boundaryIndex - 1);                
            value = boundary[0];
        }
        return value;
    }
}
