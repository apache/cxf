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
package org.apache.cxf.jaxrs.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.impl.UriBuilderImpl;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.message.MessageImpl;

public class AbstractClient implements Client {
    
    protected static final MediaType WILDCARD = MediaType.valueOf("*/*");
    
    private MultivaluedMap<String, String> requestHeaders = new MetadataMap<String, String>();
    private ResponseBuilder responseBuilder;
    
    private URI baseURI;
    private UriBuilder currentBuilder;
    
    protected AbstractClient(URI baseURI, URI currentURI) {
        this.baseURI = baseURI;
        this.currentBuilder = new UriBuilderImpl(currentURI);
    }
    
    protected AbstractClient(Client client, boolean inheritHeaders) {
        this.baseURI = client.getCurrentURI();
        this.currentBuilder = new UriBuilderImpl(client.getCurrentURI());
        if (inheritHeaders) {
            this.requestHeaders = client.getHeaders();
        }
    }
    
    public Client header(String name, Object... values) {
        if (values == null) {
            throw new IllegalArgumentException();
        }
        if (HttpHeaders.CONTENT_TYPE.equals(name) && values.length > 1) {
            throw new WebApplicationException();
        }
        for (Object o : values) {
            requestHeaders.add(name, o.toString());
        }
        return this;
    }

    public Client headers(MultivaluedMap<String, String> map) {
        requestHeaders.putAll(map);
        return this;
    }
    
    public Client accept(MediaType... types) {
        for (MediaType mt : types) {
            requestHeaders.add(HttpHeaders.ACCEPT, mt.toString());
        }
        return this;
    }

    public Client type(MediaType ct) {
        return type(ct.toString());
    }
    
    public Client type(String type) {
        requestHeaders.putSingle(HttpHeaders.CONTENT_TYPE, type);
        return this;
    }

    public Client accept(String... types) {
        for (String type : types) {
            requestHeaders.add(HttpHeaders.ACCEPT, type);
        }
        return this;
    }

    public Client cookie(Cookie cookie) {
        requestHeaders.add(HttpHeaders.COOKIE, cookie.toString());
        return this;
    }

    public Client modified(Date date, boolean ifNot) {
        SimpleDateFormat dateFormat = HttpUtils.getHttpDateFormat();
        String hName = ifNot ? HttpHeaders.IF_UNMODIFIED_SINCE : HttpHeaders.IF_MODIFIED_SINCE;
        requestHeaders.putSingle(hName, dateFormat.format(date));
        return this;
    }

    public Client language(String language) {
        requestHeaders.putSingle(HttpHeaders.CONTENT_LANGUAGE, language);
        return this;
    }

    public Client match(EntityTag tag, boolean ifNot) {
        String hName = ifNot ? HttpHeaders.IF_NONE_MATCH : HttpHeaders.IF_MATCH; 
        requestHeaders.putSingle(hName, tag.toString());
        return this;
    }

    public Client acceptLanguage(String... languages) {
        for (String s : languages) {
            requestHeaders.add(HttpHeaders.ACCEPT_LANGUAGE, s);
        }
        return this;
    }

    public Client acceptEncoding(String... encs) {
        for (String s : encs) {
            requestHeaders.add(HttpHeaders.ACCEPT_ENCODING, s);
        }
        return this;
    }

    public Client encoding(String enc) {
        requestHeaders.putSingle(HttpHeaders.CONTENT_ENCODING, enc);
        return this;
    }
    
    protected List<MediaType> getAccept() {
        List<String> headers = requestHeaders.get(HttpHeaders.ACCEPT);
        if (headers == null || headers.size() == 0) {
            return null;
        }
        List<MediaType> types = new ArrayList<MediaType>();
        for (String s : headers) {
            types.add(MediaType.valueOf(s));
        }
        return types;
    }

    public MultivaluedMap<String, String> getHeaders() {
        MultivaluedMap<String, String> map = new MetadataMap<String, String>();
        map.putAll(requestHeaders);
        return map;
    }
    
    protected MediaType getType() {
        String type = requestHeaders.getFirst(HttpHeaders.CONTENT_TYPE);
        return type == null ? null : MediaType.valueOf(type);
    }

    public URI getBaseURI() {
        return baseURI;
    }

    public URI getCurrentURI() {
        return getCurrentBuilder().clone().build();
    }
    
    protected UriBuilder getCurrentBuilder() {
        return currentBuilder;
    }

    public Response getResponse() {
        if (responseBuilder == null) {
            throw new IllegalStateException();
        }
        Response r = responseBuilder.build();
        responseBuilder = null;
        return r;
    }
    
    public Client reset() {
        requestHeaders.clear();
        resetResponse();
        return this;
    }
    
    protected void resetResponse() {
        responseBuilder = null;
    }
    
    protected void resetBaseAddress(URI uri) {
        baseURI = uri;
        currentBuilder = new UriBuilderImpl(baseURI);
    }
    
    protected ResponseBuilder setResponseBuilder(HttpURLConnection conn) throws IOException {
        int status = conn.getResponseCode();
        responseBuilder = Response.status(status);
        for (Map.Entry<String, List<String>> entry : conn.getHeaderFields().entrySet()) {
            for (String s : entry.getValue()) {
                responseBuilder.header(entry.getKey(), s);
            }
        }
        if (status >= 400) {
            try {
                InputStream errorStream = conn.getErrorStream();
                if (errorStream != null) {
                    responseBuilder.entity(IOUtils.readStringFromStream(errorStream));
                }
            } catch (Exception ex) {
                // nothing we can do really
            }
        }
        return responseBuilder;
    }

    @SuppressWarnings("unchecked")
    protected static void writeBody(Object o, Class<?> cls, Type type, Annotation[] anns, 
        MultivaluedMap<String, String> headers, OutputStream os) {
        
        if (o == null) {
            return;
        }
        
        MediaType contentType = MediaType.valueOf(headers.getFirst("Content-Type")); 
        
        MessageBodyWriter mbr = ProviderFactory.getInstance().createMessageBodyWriter(
            cls, type, anns, contentType, new MessageImpl());
        if (mbr != null) {
            try {
                mbr.writeTo(o, cls, type, anns, contentType, headers, os);
                os.flush();
            } catch (Exception ex) {
                throw new WebApplicationException();
            }
             
        } else {
            throw new WebApplicationException();
        }
                                                                                 
    }
    
    @SuppressWarnings("unchecked")
    protected static Object readBody(Response r, HttpURLConnection conn, Class<?> cls, Type type, 
                                     Annotation[] anns) {

        try {
            int status = conn.getResponseCode();
            if (status < 200 || status == 204 || status > 300) {
                return null;
            }
        } catch (IOException ex) {
            // won't happen at this stage
        }
        
        MediaType contentType = getResponseContentType(r);
        
        MessageBodyReader mbr = ProviderFactory.getInstance().createMessageBodyReader(
            cls, type, anns, contentType, new MessageImpl());
        if (mbr != null) {
            try {
                return mbr.readFrom(cls, type, anns, contentType, r.getMetadata(), conn.getInputStream());
            } catch (Exception ex) {
                throw new WebApplicationException();
            }
             
        } else {
            throw new WebApplicationException();
        }
                                                                                 
    }
    
    private static MediaType getResponseContentType(Response r) {
        MultivaluedMap<String, Object> map = r.getMetadata();
        if (map.containsKey(HttpHeaders.CONTENT_TYPE)) {
            return MediaType.valueOf(map.getFirst(HttpHeaders.CONTENT_TYPE).toString());
        }
        return WILDCARD;
    }
    
    protected static HttpURLConnection createHttpConnection(URI uri, String methodName) {
        try {
            URL url = uri.toURL();
            HttpURLConnection connect = (HttpURLConnection)url.openConnection();
            connect.setDoOutput(true);
            connect.setRequestMethod(methodName);
            return connect;
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
    }
    
    protected static void setAllHeaders(MultivaluedMap<String, String> headers, HttpURLConnection conn) {
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            StringBuilder b = new StringBuilder();    
            for (int i = 0; i < entry.getValue().size(); i++) {
                String value = entry.getValue().get(i);
                b.append(value);
                if (i + 1 < entry.getValue().size()) {
                    b.append(',');
                }
            }
            conn.setRequestProperty(entry.getKey(), b.toString());
        }
    }

}
