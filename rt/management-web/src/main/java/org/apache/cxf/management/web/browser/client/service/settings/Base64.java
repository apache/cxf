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

package org.apache.cxf.management.web.browser.client.service.settings;

//TODO this class isn't mine write appropriate comment

/**
 * Custom Base64 encode/decode implementation suitable for use in GWT applications
 * (uses only translatable classes).
 */
public final class Base64 {

    private static final String ETAB = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";

    private Base64() {
    }

    public static String encode(String data) {
        StringBuffer out = new StringBuffer();

        int i = 0;
        int r = data.length();
        while (r > 0) {
            byte d0;
            byte d1;
            byte d2;
            byte e0;
            byte e1;
            byte e2;
            byte e3;

            d0 = (byte)data.charAt(i++);
            --r;
            e0 = (byte)(d0 >>> 2);
            e1 = (byte)((d0 & 0x03) << 4);

            if (r > 0) {
                d1 = (byte)data.charAt(i++);
                --r;
                e1 += (byte)(d1 >>> 4);
                e2 = (byte)((d1 & 0x0f) << 2);
            } else {
                e2 = 64;
            }

            if (r > 0) {
                d2 = (byte)data.charAt(i++);
                --r;
                e2 += (byte)(d2 >>> 6);
                e3 = (byte)(d2 & 0x3f);
            } else {
                e3 = 64;
            }
            out.append(ETAB.charAt(e0));
            out.append(ETAB.charAt(e1));
            out.append(ETAB.charAt(e2));
            out.append(ETAB.charAt(e3));
        }

        return out.toString();
    }
}
