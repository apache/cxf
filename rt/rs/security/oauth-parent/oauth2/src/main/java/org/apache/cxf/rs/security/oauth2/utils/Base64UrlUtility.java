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

package org.apache.cxf.rs.security.oauth2.utils;

/**
 * Base64 URL Encoding/Decoding utility.
 *  
 * Character 62 ('+') is '-', Character 63 ('/') is '_';
 * Padding characters are dropped after the encoding.   
 *                  
 */

import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64Utility;


public final class Base64UrlUtility {
    private static final Logger LOG = LogUtils.getL7dLogger(Base64UrlUtility.class);
    
    private Base64UrlUtility() {
        //utility class, never constructed
    }
    
    public static byte[] decode(String encoded) throws Base64Exception {
        encoded = encoded.replace("-", "+").replace('_', '/');
        switch (encoded.length() % 4) {
        case 0: 
            break; 
        case 2: 
            encoded += "=="; 
            break; 
        case 3: 
            encoded += "="; 
            break; 
        default: 
            throw new Base64Exception(new Message("BASE64_RUNTIME_EXCEPTION", LOG));
        }
        return Base64Utility.decode(encoded);
    }

    public static String encode(String str) throws Base64Exception {
        try {
            return encode(str.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static String encode(byte[] id) {
        return encodeChunk(id, 0, id.length);
    }

    public static String encodeChunk(byte[] id, int offset, int length) {
        String encoded = new String(Base64Utility.encodeChunk(id, offset, length));
        return encoded.replace("+", "-").replace('/', '_').replace("=", "");
    }
     

}
