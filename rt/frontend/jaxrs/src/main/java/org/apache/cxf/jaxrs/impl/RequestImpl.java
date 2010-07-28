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

package org.apache.cxf.jaxrs.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Variant;

import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;

/**
 * TODO : deal with InvalidStateExceptions
 *
 */

public class RequestImpl implements Request {
    
    private final Message m;
    private final HttpHeaders headers;
    
    public RequestImpl(Message m) {
        this.m = m;
        this.headers = new HttpHeadersImpl(m);
    }

    

    public Variant selectVariant(List<Variant> vars) throws IllegalArgumentException {
        if (vars == null || vars.isEmpty()) {
            throw new IllegalArgumentException("List of Variants is either null or empty");
        }
        MediaType inMediaType = headers.getMediaType();
        Locale inLang = headers.getLanguage();
        String inEnc = headers.getRequestHeaders().getFirst(HttpHeaders.CONTENT_ENCODING);
        
        List<Variant> matchingVars = new LinkedList<Variant>();
        for (Variant var : vars) {
            MediaType mt = var.getMediaType();
            Locale lang = var.getLanguage();
            String enc = var.getEncoding();
            
            boolean mtMatched = mt == null || inMediaType == null
                || JAXRSUtils.intersectMimeTypes(Collections.singletonList(inMediaType), mt).size() != 0;
            
            boolean encMatched = inEnc == null || enc == null || inEnc.equalsIgnoreCase(enc);
            
            boolean langMatched = inLang == null || lang == null || inLang.equals(lang);
            
            if (mtMatched && encMatched && langMatched) {
                matchingVars.add(var);
            }
        }
        if (matchingVars.size() > 1) {
            Collections.sort(matchingVars, new VariantComparator());       
        }
        return matchingVars.isEmpty() ? null : matchingVars.get(0);
    }



    public ResponseBuilder evaluatePreconditions(EntityTag eTag) {
        ResponseBuilder rb = evaluateIfMatch(eTag);
        if (rb == null) {
            rb = evaluateIfNonMatch(eTag);
        }
        return rb;
    }

    private ResponseBuilder evaluateIfMatch(EntityTag eTag) {
        List<String> ifMatch = headers.getRequestHeader(HttpHeaders.IF_MATCH);
        
        if (ifMatch == null || ifMatch.size() == 0) {
            return null;
        }
        
        try {
            for (String value : ifMatch) {
                if ("*".equals(value)) {
                    return null;
                }
                EntityTag requestTag = EntityTag.valueOf(value);
                // must be a strong comparison
                if (!requestTag.isWeak() && !eTag.isWeak() && requestTag.equals(eTag)) {
                    return null;
                }
            }
        } catch (IllegalArgumentException ex) {
            // ignore
        }
        return Response.status(Response.Status.PRECONDITION_FAILED).tag(eTag);
    }

    private ResponseBuilder evaluateIfNonMatch(EntityTag eTag) {
        List<String> ifNonMatch = headers.getRequestHeader(HttpHeaders.IF_NONE_MATCH);
        
        if (ifNonMatch == null || ifNonMatch.size() == 0) {
            return null;
        }
        
        String method = getMethod();
        boolean getOrHead = "GET".equals(method) || "HEAD".equals(method);
        try {
            for (String value : ifNonMatch) {
                boolean result = "*".equals(value);
                if (!result) {
                    EntityTag requestTag = EntityTag.valueOf(value);
                    result = getOrHead ? requestTag.equals(eTag) 
                        : !requestTag.isWeak() && !eTag.isWeak() && requestTag.equals(eTag);
                }
                if (result) {
                    Response.Status status = getOrHead ? Response.Status.NOT_MODIFIED
                        : Response.Status.PRECONDITION_FAILED;
                    return Response.status(status).tag(eTag);
                }
            }
        } catch (IllegalArgumentException ex) {
            // ignore
        }
        return null;
    }
    
    public ResponseBuilder evaluatePreconditions(Date lastModified) {
        List<String> ifModifiedSince = headers.getRequestHeader(HttpHeaders.IF_MODIFIED_SINCE);
        
        if (ifModifiedSince == null || ifModifiedSince.size() == 0) {
            return evaluateIfNotModifiedSince(lastModified);
        }
        
        SimpleDateFormat dateFormat = HttpUtils.getHttpDateFormat();

        dateFormat.setLenient(false);
        Date dateSince = null;
        try {
            dateSince = dateFormat.parse(ifModifiedSince.get(0));
        } catch (ParseException ex) {
            // invalid header value, request should continue
            return Response.status(Response.Status.PRECONDITION_FAILED);
        }
        
        if (dateSince.before(lastModified)) {
            // request should continue
            return null;
        }
        
        return Response.status(Response.Status.NOT_MODIFIED);
    }
    
    public ResponseBuilder evaluateIfNotModifiedSince(Date lastModified) {
        List<String> ifNotModifiedSince = headers.getRequestHeader(HttpHeaders.IF_UNMODIFIED_SINCE);
        
        if (ifNotModifiedSince == null || ifNotModifiedSince.size() == 0) {
            return null;
        }
        
        SimpleDateFormat dateFormat = HttpUtils.getHttpDateFormat();

        dateFormat.setLenient(false);
        Date dateSince = null;
        try {
            dateSince = dateFormat.parse(ifNotModifiedSince.get(0));
        } catch (ParseException ex) {
            // invalid header value, request should continue
            return Response.status(Response.Status.PRECONDITION_FAILED);
        }
        
        if (dateSince.before(lastModified)) {
            return Response.status(Response.Status.PRECONDITION_FAILED);
        }
        
        return null;
    }



    public ResponseBuilder evaluatePreconditions(Date lastModified, EntityTag eTag) {
        ResponseBuilder rb = evaluatePreconditions(eTag);
        if (rb != null) {
            return rb;
        }
        return evaluatePreconditions(lastModified);
                
    }
    
    public String getMethod() {
        return m.get(Message.HTTP_REQUEST_METHOD).toString();
    }



    private static class VariantComparator implements Comparator<Variant> {

        public int compare(Variant v1, Variant v2) {
            int result = compareMediaTypes(v1.getMediaType(), v2.getMediaType());
            
            if (result != 0) {
                return result;
            }
            
            result = compareLanguages(v1.getLanguage(), v2.getLanguage());
            
            if (result == 0) {
                result = compareEncodings(v1.getEncoding(), v2.getEncoding());
            }
            
            return result;
        }
        
        private static int compareMediaTypes(MediaType mt1, MediaType mt2) {
            if (mt1 != null && mt2 == null) {
                return -1;
            } else if (mt1 == null && mt2 != null) {
                return 1;
            } 
            return JAXRSUtils.compareMediaTypes(mt1, mt2);
        }
        
        private static int compareLanguages(Locale l1, Locale l2) {
            if (l1 != null && l2 == null) {
                return -1;
            } else if (l1 == null && l2 != null) {
                return 1;
            }
            return 0;
        }
        
        private static int compareEncodings(String enc1, String enc2) {
            if (enc1 != null && enc2 == null) {
                return -1;
            } else if (enc1 == null && enc2 != null) {
                return 1;
            }
            return 0;
        }
    }
}
