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
package org.apache.cxf.common.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public final class CompressionUtils {
    private CompressionUtils() {

    }
    public static InputStream inflate(byte[] deflatedToken)
        throws DataFormatException {
        return inflate(deflatedToken, true);
    }
    public static InputStream inflate(byte[] deflatedToken, boolean nowrap)
        throws DataFormatException {
        Inflater inflater = new Inflater(true);
        inflater.setInput(deflatedToken);

        byte[] buffer = new byte[deflatedToken.length];
        int inflateLen;
        ByteArrayOutputStream inflatedToken = new ByteArrayOutputStream();
        while (!inflater.finished()) {
            inflateLen = inflater.inflate(buffer, 0, deflatedToken.length);
            if (inflateLen == 0 && !inflater.finished()) {
                if (inflater.needsInput()) {
                    throw new DataFormatException("Inflater can not inflate all the token bytes");
                }
                break;
            }

            inflatedToken.write(buffer, 0, inflateLen);
        }

        return new ByteArrayInputStream(inflatedToken.toByteArray());
    }

    public static byte[] deflate(byte[] tokenBytes) {
        return deflate(tokenBytes, true);
    }

    public static byte[] deflate(byte[] tokenBytes, boolean nowrap) {
        return deflate(tokenBytes, Deflater.DEFLATED, nowrap);
    }

    public static byte[] deflate(byte[] tokenBytes, int level, boolean nowrap) {
        Deflater compresser = new Deflater(level, nowrap);

        compresser.setInput(tokenBytes);
        compresser.finish();

        byte[] output = new byte[tokenBytes.length * 2];

        int compressedDataLength = compresser.deflate(output);

        byte[] result = new byte[compressedDataLength];
        System.arraycopy(output, 0, result, 0, compressedDataLength);
        return result;
    }
}
