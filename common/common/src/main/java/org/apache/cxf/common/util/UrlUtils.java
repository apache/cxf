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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;

/**
 * Utility class for decoding and encoding URLs
 *
 */
public final class UrlUtils {
    
    private static final Logger LOG = LogUtils.getL7dLogger(UrlUtils.class);
    
    private static final String[] RESERVED_CHARS = {"+"};
    private static final String[] ENCODED_CHARS = {"%2b"};
    
    private UrlUtils() {
        
    }

    /**
     * Decodes using URLDecoder - use when queries or form post values are decoded
     * @param value value to decode
     * @return
     */
    public static String urlDecode(String value) {
        try {
            value = URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOG.warning("UTF-8 encoding can not be used to decode " + value);          
        }
        return value;
    }
    
    /**
     * URL path segments may contain '+' symbols which should not be decoded into ' '
     * This method replaces '+' with %2B and delegates to URLDecoder
     * @param value value to decode
     * @return
     */
    public static String pathDecode(String value) {
        // TODO: we actually need to do a proper URI analysis here according to
        // http://tools.ietf.org/html/rfc3986
        for (int i = 0; i < RESERVED_CHARS.length; i++) {
            if (value.indexOf(RESERVED_CHARS[i]) != -1) {
                value = value.replace(RESERVED_CHARS[i], ENCODED_CHARS[i]);
            }
        }
        
        return urlDecode(value);
    }
    
}
