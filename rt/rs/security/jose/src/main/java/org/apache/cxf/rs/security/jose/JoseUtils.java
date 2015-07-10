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
package org.apache.cxf.rs.security.jose;

import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.rt.security.crypto.CryptoUtils;

public final class JoseUtils {
    private static final Logger LOG = LogUtils.getL7dLogger(JoseUtils.class);
    private JoseUtils() {
        
    }
    public static String[] getCompactParts(String compactContent) {
        if (compactContent.startsWith("\"") && compactContent.endsWith("\"")) {
            compactContent = compactContent.substring(1, compactContent.length() - 1);
        }
        return StringUtils.split(compactContent, "\\.");    
    }
    public static void setJoseContextProperty(JoseHeaders headers) {    
        String context = (String)JAXRSUtils.getCurrentMessage().get(JoseConstants.JOSE_CONTEXT_PROPERTY);
        if (context != null) {
            headers.setHeader(JoseConstants.JOSE_CONTEXT_PROPERTY, context);
        }
    }
    public static void setJoseMessageContextProperty(JoseHeaders headers, String value) {    
        headers.setHeader(JoseConstants.JOSE_CONTEXT_PROPERTY, value);
        JAXRSUtils.getCurrentMessage().put(JoseConstants.JOSE_CONTEXT_PROPERTY, value);
    }
    public static void setMessageContextProperty(JoseHeaders headers) {    
        String context = (String)headers.getHeader(JoseConstants.JOSE_CONTEXT_PROPERTY);
        if (context != null) {
            JAXRSUtils.getCurrentMessage().put(JoseConstants.JOSE_CONTEXT_PROPERTY, context);
        }
    }
    public static void validateRequestContextProperty(JoseHeaders headers) {
        Object requestContext = JAXRSUtils.getCurrentMessage().get(JoseConstants.JOSE_CONTEXT_PROPERTY);
        Object headerContext = headers.getHeader(JoseConstants.JOSE_CONTEXT_PROPERTY);
        if (requestContext == null && headerContext == null) {
            return;
        }
        if (requestContext == null && headerContext != null
            || requestContext != null && headerContext == null
            || !requestContext.equals(headerContext)) {
            LOG.warning("Invalid JOSE context property");
            throw new JoseException();
        }
    }
    
    public static String checkContentType(String contentType, String defaultType) {
        if (contentType != null) {
            int paramIndex = contentType.indexOf(';');
            String typeWithoutParams = paramIndex == -1 ? contentType : contentType.substring(0, paramIndex);
            if (typeWithoutParams.indexOf('/') == -1) {
                contentType = "application/" + contentType;
            }
        } else {
            contentType = defaultType;
        }
        return contentType;
    }
    public static String expandContentType(String contentType) {
        int paramIndex = contentType.indexOf(';');
        String typeWithoutParams = paramIndex == -1 ? contentType : contentType.substring(0, paramIndex);
        if (typeWithoutParams.indexOf('/') == -1) {
            contentType = "application/" + contentType;
        }
        return contentType;
    }
    
    public static String decodeToString(String encoded) {
        try {
            return new String(decode(encoded), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new JoseException(ex);
        }
        
    }
    public static byte[] decode(String encoded) {
        return CryptoUtils.decodeSequence(encoded);
    }
    
    public static boolean validateCriticalHeaders(JoseHeaders headers) {
        List<String> critical = headers.getCritical();
        if (critical == null) {
            return true;
        }
        // The "crit" value MUST NOT be empty "[]" or contain either duplicate values or "crit"
        if (critical.isEmpty() 
            || detectDoubleEntry(critical)
            || critical.contains(JoseConstants.HEADER_CRITICAL)) {
            return false;
        }
        
        // Check that the headers contain these critical headers
        return headers.asMap().keySet().containsAll(critical);
    }
    private static boolean detectDoubleEntry(List<?> list) {
        Set<Object> inputSet = new HashSet<Object>(list);
        return list.size() > inputSet.size();
    }
}
