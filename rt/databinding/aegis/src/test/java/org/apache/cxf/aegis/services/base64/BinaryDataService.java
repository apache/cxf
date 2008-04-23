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
package org.apache.cxf.aegis.services.base64;

import java.util.zip.CRC32;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BinaryDataService {
    private final Log log = LogFactory.getLog(getClass());

    public String verifyDataIntegrity(byte[] data, int length, long crc32) {
        log.debug("verifyDataIntegrity([" + data.length + " bytes of data], " + length + ", " + crc32
                  + ") called.");

        String status = getStatusForData(data, length, crc32);

        log.debug("verifyDataIntegrity status: " + status);
        return status;
    }

    /**
     * @param data
     * @param length
     * @param crc32
     * @return
     */
    private String getStatusForData(byte[] data, int length, long crc32) {

        String status;
        if (data.length != length) {
            status = "data.length == " + data.length + ", should be " + length;
        } else {
            CRC32 computedCrc32 = new CRC32();
            computedCrc32.update(data);
            if (computedCrc32.getValue() != crc32) {
                status = "Computed crc32 == " + computedCrc32.getValue() + ", should be " + crc32;
            } else {
                status = "OK";
            }
        }
        return status;
    }

}
