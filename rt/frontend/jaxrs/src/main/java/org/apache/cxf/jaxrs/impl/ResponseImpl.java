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

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ResponseProcessingException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.RuntimeDelegate;
import javax.ws.rs.ext.RuntimeDelegate.HeaderDelegate;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;

public final class ResponseImpl extends Response {
    private int status;
    private Object entity;
    private Annotation[] entityAnnotations; 
    private MultivaluedMap<String, Object> metadata;
    
    private Message responseMessage;
    private boolean entityClosed;    
    private boolean entityBufferred;
    private Object lastEntity;
    
    ResponseImpl(int s) {
        this.status = s;
    }
    
    ResponseImpl(int s, Object e) {
        this.status = s;
        this.entity = e;
    }
    
    void addMetadata(MultivaluedMap<String, Object> meta) { 
        this.metadata = meta;
    }
    
    public void setStatus(int s) { 
        this.status = s;
    }
    
    public void setEntity(Object e, Annotation[] anns) { 
        this.entity = e;
        this.entityAnnotations = anns;
    }
    
    public void setEntityAnnotations(Annotation[] anns) { 
        this.entityAnnotations = anns;
    }
    
    public Annotation[] getEntityAnnotations() {
        return entityAnnotations;
    }

    //TODO: This method is needed because on the client side the
    // Response processing is done after the chain completes, thus
    // PhaseInterceptorChain.getCurrentMessage() returns null.
    // The refactoring will be required
    public void setMessage(Message message) {
        this.responseMessage = message;
    }
    
    public int getStatus() {
        return status;
    }

    public StatusType getStatusInfo() {
        return new Response.StatusType() {

            public Family getFamily() {
                return Response.Status.Family.familyOf(ResponseImpl.this.status);
            }

            public String getReasonPhrase() {
                Response.Status statusEnum = Response.Status.fromStatusCode(ResponseImpl.this.status); 
                return statusEnum != null ? statusEnum.getReasonPhrase() : "";
            }

            public int getStatusCode() {
                return ResponseImpl.this.status;
            } 
            
        };
    }
    
    public Object getActualEntity() {
        checkEntityIsClosed();
        return lastEntity != null ? lastEntity : entity;
    }
    
    public Object getEntity() {
        return InjectionUtils.getEntity(getActualEntity());
    }

    public boolean hasEntity() {
        return getActualEntity() != null;
    }
    
    public MultivaluedMap<String, Object> getMetadata() {
        return getHeaders();
    }
    
    public MultivaluedMap<String, Object> getHeaders() {
        return metadata;
    }
    
    public MultivaluedMap<String, String> getStringHeaders() {
        MetadataMap<String, String> headers = new MetadataMap<String, String>(metadata.size());
        for (Map.Entry<String, List<Object>> entry : metadata.entrySet()) {
            String headerName = entry.getKey();
            headers.put(headerName, toListOfStrings(headerName, entry.getValue()));
        }
        return headers;
    }

    public String getHeaderString(String header) {
        List<Object> methodValues = metadata.get(header);
        return HttpUtils.getHeaderString(toListOfStrings(header, methodValues));
    }
    
    // This conversion is needed as some values may not be Strings
    private List<String> toListOfStrings(String headerName, List<Object> values) {
        if (values == null) {
            return null; 
        } else {
            List<String> stringValues = new ArrayList<String>(values.size());
            RuntimeDelegate rd = RuntimeDelegate.getInstance();
            @SuppressWarnings("unchecked")
            HeaderDelegate<Object> hd = rd == null || values.isEmpty() 
                ? null : (HeaderDelegate<Object>)rd.createHeaderDelegate(values.get(0).getClass());
            for (Object value : values) {
                String actualValue = hd == null ? value.toString() : hd.toString(value); 
                stringValues.add(actualValue);
            }
            return stringValues;
        }
    }
    
    public Set<String> getAllowedMethods() {
        List<Object> methodValues = metadata.get(HttpHeaders.ALLOW);
        if (methodValues == null) {
            return Collections.emptySet();
        } else {
            Set<String> methods = new HashSet<String>();
            for (Object o : methodValues) {
                methods.add(o.toString());
            }
            return methods;
        }
    }

    
    
    public Map<String, NewCookie> getCookies() {
        List<Object> cookieValues = metadata.get(HttpHeaders.SET_COOKIE);
        if (cookieValues == null) {
            return Collections.emptyMap();
        } else {
            Map<String, NewCookie> cookies = new HashMap<String, NewCookie>();
            for (Object o : cookieValues) {
                NewCookie newCookie = NewCookie.valueOf(o.toString());
                cookies.put(newCookie.getName(), newCookie);
            }
            return cookies;
        }
    }

    public Date getDate() {
        return doGetDate(HttpHeaders.DATE);
    }

    private Date doGetDate(String dateHeader) {
        Object value = metadata.getFirst(dateHeader);
        return value == null || value instanceof Date ? (Date)value
            : HttpUtils.getHttpDate(value.toString());
    }
    
    public EntityTag getEntityTag() {
        Object header = metadata.getFirst(HttpHeaders.ETAG);
        return header == null || header instanceof EntityTag ? (EntityTag)header
            : EntityTag.valueOf(header.toString());
    }

    public Locale getLanguage() {
        Object header = metadata.getFirst(HttpHeaders.CONTENT_LANGUAGE);
        return header == null || header instanceof Locale ? (Locale)header
            : HttpUtils.getLocale(header.toString());
    }

    public Date getLastModified() {
        return doGetDate(HttpHeaders.LAST_MODIFIED);
    }

    public int getLength() {
        Object header = metadata.getFirst(HttpHeaders.CONTENT_LENGTH);
        return HttpUtils.getContentLength(header == null ? null : header.toString());
    }

    public URI getLocation() {
        Object header = metadata.getFirst(HttpHeaders.LOCATION);
        return header == null || header instanceof URI ? (URI)header
            : URI.create(header.toString());
    }

    public MediaType getMediaType() {
        Object header = metadata.getFirst(HttpHeaders.CONTENT_TYPE);
        return header == null || header instanceof MediaType ? (MediaType)header 
            : (MediaType)JAXRSUtils.toMediaType(header.toString());
    }
    
    public boolean hasLink(String relation) {
        return getLink(relation) != null;
    }
    
    public Link getLink(String relation) {
        Set<Map.Entry<String, Link>> entries = getAllLinks().entrySet();
        for (Map.Entry<String, Link> entry : entries) {
            if (entry.getKey().contains(relation)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public Link.Builder getLinkBuilder(String relation) {
        Link link = getLink(relation);
        return link == null ? null : Link.fromLink(link);
    }

    public Set<Link> getLinks() {
        List<Object> linkValues = metadata.get(HttpHeaders.LINK);
        Set<Link> links = new HashSet<Link>();
        if (linkValues != null) {
            for (Object o : linkValues) {
                Link link = o instanceof Link ? (Link)o : Link.valueOf(o.toString());
                links.add(link);
            }
        }
        return links;
    }

    private Map<String, Link> getAllLinks() {
        List<Object> linkValues = metadata.get(HttpHeaders.LINK);
        if (linkValues == null) {
            return Collections.emptyMap();
        } else {
            Map<String, Link> links = new LinkedHashMap<String, Link>();
            for (Object o : linkValues) {
                Link link = o instanceof Link ? (Link)o : Link.valueOf(o.toString());
                links.put(link.getRel(), link);
            }
            return links;
        }
    }
    
    public <T> T readEntity(Class<T> cls) throws ProcessingException, IllegalStateException {
        return readEntity(cls, new Annotation[]{});
    }

    public <T> T readEntity(GenericType<T> genType) throws ProcessingException, IllegalStateException {
        return readEntity(genType, new Annotation[]{});
    }

    public <T> T readEntity(Class<T> cls, Annotation[] anns) throws ProcessingException,
        IllegalStateException {
        
        return doReadEntity(cls, cls, anns);
    }

    @SuppressWarnings("unchecked")
    public <T> T readEntity(GenericType<T> genType, Annotation[] anns) throws ProcessingException,
        IllegalStateException {
        return doReadEntity((Class<T>)genType.getRawType(), 
                            genType.getType(), anns);
    }
    
    public <T> T doReadEntity(Class<T> cls, Type t, Annotation[] anns) throws ProcessingException,
        IllegalStateException {
        
        checkEntityIsClosed();
        
        if (lastEntity != null && cls.isAssignableFrom(lastEntity.getClass())
            && !(lastEntity instanceof InputStream)) {
            return cls.cast(lastEntity);
        } 
        
        if (entity instanceof InputStream) {
            
            MediaType mediaType = getMediaType();
            if (mediaType == null) {
                mediaType = MediaType.WILDCARD_TYPE;
            }
            
            List<ReaderInterceptor> readers = ProviderFactory.getInstance(responseMessage)
                .createMessageBodyReaderInterceptor(cls, t, anns, mediaType, 
                                                    responseMessage.getExchange().getOutMessage(), 
                                                    null);
            if (readers != null) {
                try {
                    responseMessage.put(Message.PROTOCOL_HEADERS, this.getMetadata());
                    lastEntity = JAXRSUtils.readFromMessageBodyReader(readers, cls, t, 
                                                                           anns, 
                                                                           InputStream.class.cast(entity), 
                                                                           mediaType, 
                                                                           responseMessage);
                    if (!entityBufferred) {
                        if (responseStreamCanBeClosed(cls)) {
                            InputStream.class.cast(entity).close();
                            entity = null;
                        }
                    } else {
                        InputStream.class.cast(entity).reset();
                    }
                    
                    return cls.cast(lastEntity);
                } catch (Exception ex) {
                    throw new ResponseProcessingException(this, ex.getMessage(), ex);    
                }
            } else {
                throw new ResponseProcessingException(this, "No message body reader for class: " + cls, null);
            }
        } else if (entity != null && cls.isAssignableFrom(entity.getClass())) {
            lastEntity = entity;
            return cls.cast(lastEntity);
        }
        
        throw new IllegalStateException("The entity is not backed by an input stream, entity class is : "
            + entity != null ? entity.getClass().getName() : null);
        
    }
    
    protected boolean responseStreamCanBeClosed(Class<?> cls) {
        return cls != InputStream.class
            && MessageUtils.isTrue(responseMessage.getContextualProperty("response.stream.auto.close"));
    }
    
    public boolean bufferEntity() throws ProcessingException {
        checkEntityIsClosed();
        if (!entityBufferred && entity instanceof InputStream) {
            try {
                InputStream oldEntity = (InputStream)entity;
                entity = IOUtils.loadIntoBAIS(oldEntity);
                oldEntity.close();
                entityBufferred = true;
            } catch (IOException ex) {
                throw new ResponseProcessingException(this, ex);
            }
        }
        return entityBufferred;
    }

    public void close() throws ProcessingException {
        if (!entityClosed) {
            if (!entityBufferred && entity instanceof InputStream) {
                try {
                    ((InputStream)entity).close();
                } catch (IOException ex) {
                    throw new ResponseProcessingException(this, ex);
                }
            }
            entity = null;
            entityClosed = true;
        }
        
    }
    
    private void checkEntityIsClosed() {
        if (entityClosed) {
            throw new IllegalStateException("Entity is not available");
        }
    }
}
