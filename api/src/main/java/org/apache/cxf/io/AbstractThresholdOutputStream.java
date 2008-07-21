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

package org.apache.cxf.io;

import java.io.IOException;

import org.apache.cxf.helpers.LoadingByteArrayOutputStream;

/**
 * Outputstream that will buffer a certain amount before writing anything to the underlying
 * stream.   When the threshold is reached, provides a callback point to allow the
 * subclass to update headers, replace/set the output stream, etc...
 * 
 * Also provides a callback for when the stream is closed without it reaching the threshold.
 */
public abstract class AbstractThresholdOutputStream extends AbstractWrappedOutputStream {
    
    protected int threshold;
    protected LoadingByteArrayOutputStream buffer;
    
    public AbstractThresholdOutputStream(int threshold) {
        this.threshold = threshold;
        if (threshold > 0) {
            buffer = new LoadingByteArrayOutputStream(threshold + 1);
        }
    }
    
    
    public abstract void thresholdReached() throws IOException;
    public abstract void thresholdNotReached() throws IOException;
    
    
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (buffer != null) {
            int space = threshold - buffer.size();
            if (space > len) {
                space = len;
            }
            buffer.write(b, off, space);
            len -= space;
            off += space;
            
            if (buffer.size() >= threshold) {
                thresholdReached();
                unBuffer();
            }
            if (len == 0) {
                return;
            }
        }
        super.write(b, off, len);
    }


    @Override
    public void write(int b) throws IOException {
        if (buffer != null) {
            buffer.write(b);
            if (buffer.size() >= threshold) {
                thresholdReached();
                unBuffer();
            }
            return;
        }
        super.write(b);
    }

    public void unBuffer() throws IOException {
        if (buffer != null) {
            if (buffer.size() > 0) {
                super.write(buffer.getRawBytes(), 0, buffer.size());
            }
            buffer = null;
        }  
    }


    @Override
    public void close() throws IOException {
        if (buffer != null) {
            thresholdNotReached();
            unBuffer();
        }
        super.close();
    }

}
