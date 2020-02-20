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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.common.util.StringUtils;

public class JwsJsonOutputStream extends FilterOutputStream {
    private boolean flushed;
    private final List<String> protectedHeaders;
    private final List<JwsSignature> signatures;
    private final ExecutorService executor;

    public JwsJsonOutputStream(OutputStream out,
                               List<String> protectedHeaders,
                               List<JwsSignature> signatures) {
        super(out);
        this.protectedHeaders = protectedHeaders;
        this.signatures = signatures;
        // This can be further optimized by having a dedicated thread per signature,
        // can make a difference if a number of signatures to be created is > 2
        this.executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void write(int value) throws IOException {
        byte[] bytes = ByteBuffer.allocate(Integer.SIZE / 8).putInt(value).array();
        write(bytes, 0, bytes.length);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        //TODO: Review if it is at least theoretically possible that a given b[] region
        // can be modified in a subsequent write which might affect the signature calculation
        executor.execute(new Runnable() {
            public void run() {
                for (JwsSignature signature : signatures) {
                    signature.update(b, off, len);
                }
            }
        });
        out.write(b, off, len);
        out.flush();
    }
    @Override
    public void flush() throws IOException {
        if (flushed) {
            return;
        }
        out.write(StringUtils.toBytesUTF8("\",\"signatures\":["));
        shutdownExecutor();
        for (int i = 0; i < signatures.size(); i++) {
            if (i > 0) {
                out.write(new byte[]{','});
            }
            out.write(StringUtils.toBytesUTF8("{\"protected\":\""
                                             + protectedHeaders.get(i)
                                             + "\",\"signature\":\""));
            byte[] sign = signatures.get(i).sign();
            Base64UrlUtility.encodeAndStream(sign, 0, sign.length, out);
            out.write(StringUtils.toBytesUTF8("\"}"));
        }
        out.write(StringUtils.toBytesUTF8("]}"));
        flushed = true;
    }
    private void shutdownExecutor() {
        executor.shutdown();
        while (!executor.isTerminated()) {
            try {
                executor.awaitTermination(1, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
