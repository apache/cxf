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
package org.apache.cxf.rs.security.saml;

import java.io.InputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;

import org.apache.cxf.common.util.CompressionUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;

public class DeflateEncoderDecoder {
    public InputStream inflateToken(byte[] deflatedToken)
        throws DataFormatException {
        return CompressionUtils.inflate(deflatedToken);
    }

    public byte[] deflateToken(byte[] tokenBytes) {

        return deflateToken(tokenBytes, true);
    }

    public byte[] deflateToken(byte[] tokenBytes, boolean nowrap) {

        return deflateToken(tokenBytes, getDeflateLevel(), nowrap);
    }

    public byte[] deflateToken(byte[] tokenBytes, int level, boolean nowrap) {

        return CompressionUtils.deflate(tokenBytes, level, nowrap);
    }

    private static int getDeflateLevel() {
        Integer level = null;

        Message m = PhaseInterceptorChain.getCurrentMessage();
        if (m != null) {
            level = PropertyUtils.getInteger(m, "deflate.level");
        }
        if (level == null) {
            level = Deflater.DEFLATED;
        }
        return level;
    }
}
