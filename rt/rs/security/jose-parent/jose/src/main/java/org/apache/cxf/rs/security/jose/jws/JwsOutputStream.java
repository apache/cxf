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
package org.apache.cxf.rs.security.jose.jws;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.apache.cxf.common.util.Base64UrlUtility;

public class JwsOutputStream extends FilterOutputStream {
    private boolean flushed;
    private final JwsSignature signature;
    private final boolean writeSignature;

    public JwsOutputStream(OutputStream out, JwsSignature signature, boolean writeSignature) {
        super(out);
        this.signature = signature;
        this.writeSignature = writeSignature;
    }

    @Override
    public void write(int value) throws IOException {
        byte[] bytes = ByteBuffer.allocate(Integer.SIZE / 8).putInt(value).array();
        write(bytes, 0, bytes.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        signature.update(b, off, len);
        out.write(b, off, len);
        out.flush();
    }
    @Override
    public void flush() throws IOException {
        if (flushed) {
            return;
        }
        if (writeSignature) {
            byte[] finalBytes = signature.sign();
            out.write(new byte[]{'.'});
            Base64UrlUtility.encodeAndStream(finalBytes, 0, finalBytes.length, out);
        } else {
            super.flush();
        }
        flushed = true;
    }

}
