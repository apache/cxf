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
    int pbAmount;
    byte[] boundary;
    byte[] boundaryBuffer;

    public MimeBodyPartInputStream(PushbackInputStream inStreamParam, 
                                   byte[] boundaryParam,
                                   int pbsize) {
        super();
        this.inStream = inStreamParam;
        this.boundary = boundaryParam;
        this.pbAmount = pbsize;
    }

    public int read(byte buf[], int origOff, int origLen) throws IOException {
        byte b[] = buf;
        int off = origOff;
        int len = origLen;
        if (boundaryFound) {
            return -1;
        }
        if ((off < 0) || (off > b.length) || (len < 0) 
            || ((off + len) > b.length) || ((off + len) < 0)) {

            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }
        boolean bufferCreated = false;
        if (len < boundary.length * 2) {
            //buffer is too short to detect boundaries with it.  We'll need to create a larger buffer   
            bufferCreated = true;
            if (boundaryBuffer == null) {
                boundaryBuffer = new byte[boundary.length * 2];
            }
            b = boundaryBuffer;
            off = 0;
            len = boundaryBuffer.length;
        }
        if (len > pbAmount) {
            len = pbAmount;  //can only pushback that much so make sure we can
        }
        int read = 0;
        int idx = 0;
        while (read >= 0 && idx < len && idx < (boundary.length * 2)) {
            //make sure we read enough to detect the boundary
            read = inStream.read(b, off + idx, len - idx);
            if (read != -1) {
                idx += read;
            }
        }
        if (read == -1 && idx == 0) {
            return -1;
        }
        len = idx;
        
        int i = processBuffer(b, off, len);
        if (bufferCreated && i > 0) {
            // read more than we need, push it back
            if (origLen >= i) {
                System.arraycopy(b, 0, buf, origOff, i);
            } else {
                System.arraycopy(b, 0, buf, origOff, origLen);
                inStream.unread(b, origLen, i - origLen);
                i = origLen;
            }
        } else if (i == 0 && boundaryFound) {
            return -1;
        }
        
        return i;
    }

    //Has Data after encountering CRLF
    private boolean hasData(byte[] b, int initialPointer, int pointer, int off, int len)
        throws IOException {
        if (pointer < (off + len)) {
            return true;
        } else if (pointer >= 1000000000) {
            inStream.unread(b, initialPointer, (off + len) - initialPointer);
            return false;            
        } else {
            int x = inStream.read();
            if (x != -1) {
                inStream.unread(x);
                inStream.unread(b, initialPointer, (off + len) - initialPointer);
                return false;
            }
            return true;
        }
    }

    protected int processBuffer(byte[] buffer, int off, int len) throws IOException {
        for (int i = off; i < (off + len); i++) {
            boolean needUnread0d0a = false;
            int value = buffer[i];
            int initialI = i;
            if (value == 13) {
                if (!hasData(buffer, initialI, initialI + 1, off, len)) {
                    return initialI - off;
                }
                value = buffer[initialI + 1];
                if (value != 10) {
                    continue;
                } else {  //if it comes here then 13, 10 are values and will try to match boundaries
                    if (!hasData(buffer, initialI, initialI + 2, off, len)) {
                        return initialI - off;
                    }
                    value = buffer[initialI + 2];
                    if ((byte) value != boundary[0]) {
                        i++;
                        continue;
                    } else { //13, 10, boundaries first value matched
                        needUnread0d0a = true;
                        i += 2; //i after this points to boundary[0] element
                    }
                }
            } else if (value != boundary[0]) {
                continue;
            }

            int boundaryIndex = 0;
            while ((boundaryIndex < boundary.length) && (value == boundary[boundaryIndex])) {
                if (!hasData(buffer, initialI, i + 1, off, len)) {
                    return initialI - off;
                }                
                value = buffer[++i];
                boundaryIndex++;
            }
            if (boundaryIndex == boundary.length) {
                // read the end of line character
                if (initialI != off) {
                    i = 1000000000;
                }
                if (initialI - off != 0 
                    && !hasData(buffer, initialI, i + 1, off, len)) {
                    return initialI - off;
                }
                boundaryFound = true;
                int j = i + 1;
                if (j < len && buffer[j] == 45 && value == 45) {
                    // Last mime boundary should have a succeeding "--"
                    // as we are on it, read the terminating CRLF
                    i += 2;
                    //last mime boundary
                }

                //boundary matched (may or may not be last mime boundary)
                int processed = initialI - off;
                if ((len - (i + 2)) > 0) {
                    inStream.unread(buffer, i + 2, len - (i + 2));
                }
                return processed;
            }

            // Boundary not found. Restoring bytes skipped.
            // write first skipped byte, push back the rest
            if (value != -1) { //pushing back first byte of boundary
                // Stream might have ended
                i--;
            }
            if (needUnread0d0a) { //Pushing all,  returning 13
                i = i - boundaryIndex;
                i--; //for 10
                value = 13;
            } else {
                i = i - boundaryIndex;
                i++;
                value = boundary[0];
            }
        }
        return len;
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
