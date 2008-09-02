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
import java.io.OutputStream;

/**
 * This outputstream implementation will both write to the outputstream
 * that is specified and cache the data at the same time. This allows us
 * to go back and retransmit the data at a later time if necessary.
 *
 */
public class CacheAndWriteOutputStream extends CachedOutputStream {

    OutputStream flowThroughStream;
    
    public CacheAndWriteOutputStream(OutputStream stream) {
        super();
        flowThroughStream = stream;
    }

    public void closeFlowthroughStream() throws IOException {
        flowThroughStream.flush();
        flowThroughStream.close();
    }
   
    protected void postClose() throws IOException {
        flowThroughStream.flush();
        flowThroughStream.close();
    }
    
    public OutputStream getFlowThroughStream() {
        return flowThroughStream;
    }
    
    
    @Override
    protected void onWrite() throws IOException {
        // does nothing
    }

    @Override
    public void write(int b) throws IOException {
        flowThroughStream.write(b);
        super.write(b);
    }
    
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        flowThroughStream.write(b, off, len);
        super.write(b, off, len);
    }
    
    @Override
    public void write(byte[] b) throws IOException {
        flowThroughStream.write(b);
        super.write(b);
    }
}
