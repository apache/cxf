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
package org.apache.cxf.rs.security.oauth2.jws;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.apache.cxf.rs.security.oauth2.utils.Base64UrlUtility;

public class JwsOutputStream extends FilterOutputStream {
    private byte[] lastNonEncodedDataChunk;
    private boolean flushed;
    private JwsSignatureProviderWorker signature;
    public JwsOutputStream(OutputStream out, JwsSignatureProviderWorker signature) {
        super(out);
        this.signature = signature;
    }

    @Override
    public void write(int value) throws IOException {
        byte[] bytes = ByteBuffer.allocate(Integer.SIZE / 8).putInt(value).array();
        write(bytes, 0, bytes.length);
    }
    
    @Override
    public void write(byte b[], int off, int len) throws IOException {
        try {
            signature.update(b, off, len);
        } catch (Throwable ex) {
            throw new SecurityException();
        }
        encodeAndWrite(b, off, len, false);
    }
    
    private void encodeAndWrite(byte[] encryptedChunk, int off, int len, boolean finalWrite) throws IOException {
        byte[] theChunk = lastNonEncodedDataChunk;
        int lenToEncode = len;
        if (theChunk != null) {
            theChunk = newArray(theChunk, 0, theChunk.length, encryptedChunk, off, len);
            lenToEncode = theChunk.length;
            off = 0;
        } else {
            theChunk = encryptedChunk;
        }
        int rem = finalWrite ? 0 : lenToEncode % 3; 
        Base64UrlUtility.encodeAndStream(theChunk, off, lenToEncode - rem, out);
        
        if (rem > 0) {
            lastNonEncodedDataChunk = newArray(theChunk, lenToEncode - rem, rem);
        } else {
            lastNonEncodedDataChunk = null;
        }
    }
    
    @Override
    public void flush() throws IOException {
        if (flushed) {
            return;
        }
        try {
            byte[] finalBytes = signature.sign();
            out.write('.');
            encodeAndWrite(finalBytes, 0, finalBytes.length, true);
        } catch (Exception ex) {
            throw new SecurityException();
        }
        flushed = true;
    }
    private byte[] newArray(byte[] src, int srcPos, int srcLen) {
        byte[] buf = new byte[srcLen];
        System.arraycopy(src, srcPos, buf, 0, srcLen);
        return buf;
    }
    private byte[] newArray(byte[] src, int srcPos, int srcLen, byte[] src2, int srcPos2, int srcLen2) {
        byte[] buf = new byte[srcLen + srcLen2];
        System.arraycopy(src, srcPos, buf, 0, srcLen);
        System.arraycopy(src2, srcPos2, buf, srcLen, srcLen2);
        return buf;
    }
}
