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

package org.apache.cxf.jaxrs.utils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.IOUtils;

public final class FormUtils {
    
    private FormUtils() {
        
    }
    
    public static String readBody(InputStream is) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            IOUtils.copy(is, bos, 1024);
            return new String(bos.toByteArray(), "UTF-8");
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }
    
    public static void populateMap(MultivaluedMap<String, String> params, 
                                   String postBody, boolean decode) {
        if (!StringUtils.isEmpty(postBody)) {
            List<String> parts = Arrays.asList(postBody.split("&"));
            for (String part : parts) {
                String[] keyValue = part.split("=");
                // Change to add blank string if key but not value is specified
                if (keyValue.length == 2) {
                    if (decode) {
                        params.add(keyValue[0], 
                            JAXRSUtils.uriDecode(keyValue[1]));
                    } else {
                        params.add(keyValue[0], keyValue[1]);
                    }
                } else {
                    params.add(keyValue[0], "");
                }
            }
        }
    }
}
