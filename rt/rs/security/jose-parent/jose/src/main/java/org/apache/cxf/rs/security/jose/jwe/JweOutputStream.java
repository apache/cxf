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
package org.apache.cxf.rs.security.jose.jwe;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

import javax.crypto.Cipher;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.Base64UrlUtility;

public class JweOutputStream extends FilterOutputStream {
    protected static final Logger LOG = LogUtils.getL7dLogger(JweOutputStream.class);
    private final Cipher encryptingCipher;
    private final int blockSize;
    private final AuthenticationTagProducer authTagProducer;
    private byte[] lastRawDataChunk;
    private byte[] lastEncryptedDataChunk;
    private boolean flushed;

    public JweOutputStream(OutputStream out,
                           Cipher encryptingCipher,
                           AuthenticationTagProducer authTagProducer) {
        super(out);
        this.encryptingCipher = encryptingCipher;
        this.blockSize = encryptingCipher.getBlockSize();
        this.authTagProducer = authTagProducer;
    }

    @Override
    public void write(int value) throws IOException {
        byte[] bytes = ByteBuffer.allocate(Integer.SIZE / 8).putInt(value).array();
        write(bytes, 0, bytes.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (lastRawDataChunk != null) {
            int remaining = blockSize - lastRawDataChunk.length;
            int lenToCopy = remaining < len ? remaining : len;
            lastRawDataChunk = newArray(lastRawDataChunk, 0, lastRawDataChunk.length, b, off, lenToCopy);
            off = off + lenToCopy;
            len -= lenToCopy;
            if (lastRawDataChunk.length < blockSize) {
                return;
            }
            encryptAndWrite(lastRawDataChunk, 0, lastRawDataChunk.length);
            lastRawDataChunk = null;
        }
        int offset = 0;
        int chunkSize = blockSize > len ? blockSize : blockSize * (len / blockSize);
        for (; offset + chunkSize <= len; offset += chunkSize, off += chunkSize) {
            encryptAndWrite(b, off, chunkSize);
        }
        if (offset < len) {
            lastRawDataChunk = newArray(b, off, len - offset);
        }

    }

    private void encryptAndWrite(byte[] chunk, int off, int len) throws IOException {
        byte[] encrypted = encryptingCipher.update(chunk, off, len);
        if (encrypted != null) {
            if (authTagProducer != null) {
                authTagProducer.update(encrypted, 0, encrypted.length);
            }
            encodeAndWrite(encrypted, 0, encrypted.length, false);
        }
    }
    private void encodeAndWrite(byte[] encryptedChunk, int off, int len, boolean finalWrite) throws IOException {
        byte[] theChunk = lastEncryptedDataChunk;
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
        out.flush();
        if (rem > 0) {
            lastEncryptedDataChunk = newArray(theChunk, lenToEncode - rem, rem);
        } else {
            lastEncryptedDataChunk = null;
        }
    }

    public void finalFlush() throws IOException {
        if (flushed) {
            return;
        }
        try {
            byte[] finalBytes = lastRawDataChunk == null
                ? encryptingCipher.doFinal()
                : encryptingCipher.doFinal(lastRawDataChunk, 0, lastRawDataChunk.length);
            final int authTagLengthBits = 128;
            if (authTagProducer != null) {
                authTagProducer.update(finalBytes, 0, finalBytes.length);
                encodeAndWrite(finalBytes, 0, finalBytes.length, true);
            } else {
                encodeAndWrite(finalBytes, 0, finalBytes.length - authTagLengthBits / 8, true);
            }
            out.write(new byte[]{'.'});

            if (authTagProducer == null) {
                encodeAndWrite(finalBytes, finalBytes.length - authTagLengthBits / 8, authTagLengthBits / 8, true);
            } else {
                byte[] authTag = authTagProducer.getTag();
                encodeAndWrite(authTag, 0, authTagLengthBits / 8, true);
            }
        } catch (Exception ex) {
            LOG.warning("Content encryption failure");
            throw new JweException(JweException.Error.CONTENT_ENCRYPTION_FAILURE, ex);
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
