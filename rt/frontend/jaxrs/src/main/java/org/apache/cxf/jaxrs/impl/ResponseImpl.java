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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
import javax.ws.rs.ext.RuntimeDelegate.HeaderDelegate;
import javax.xml.stream.XMLStreamReader;

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
    
    private Message outMessage;
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
    
    public void addMetadata(MultivaluedMap<String, Object> meta) { 
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

    public void setOutMessage(Message message) {
        this.outMessage = message;
    }
    
    public Message getOutMessage() {
        return this.outMessage;
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
            HeaderDelegate<Object> hd = HttpUtils.getHeaderDelegate(values.get(0));
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
        List<Object> linkValues = metadata.get(HttpHeaders.LINK);
        if (linkValues != null) {
            for (Object o : linkValues) {
                Link link = o instanceof Link ? (Link)o : Link.valueOf(o.toString());
                if (relation.equals(link.getRel())) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public Link getLink(String relation) {
        Set<Link> links = getAllLinks();
        for (Link link : links) {
            if (link.getRel() != null && link.getRel().equals(relation)) {
                return link;
            }
        }
        return null;
    }

    public Link.Builder getLinkBuilder(String relation) {
        Link link = getLink(relation);
        return link == null ? null : Link.fromLink(link);
    }

    public Set<Link> getLinks() {
        return new HashSet<Link>(getAllLinks());
    }

    private Set<Link> getAllLinks() {
        List<Object> linkValues = metadata.get(HttpHeaders.LINK);
        if (linkValues == null) {
            return Collections.emptySet();
        } else {
            Set<Link> links = new LinkedHashSet<Link>();
            for (Object o : linkValues) {
                Link link = o instanceof Link ? (Link)o : Link.valueOf(o.toString());
                if (!link.getUri().isAbsolute()) {
                    URI requestURI = URI.create((String)outMessage.get(Message.REQUEST_URI));
                    link = Link.fromLink(link).baseUri(requestURI).build();
                }
                links.add(link);
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
        
        MediaType mediaType = getMediaType();
        if (mediaType == null) {
            mediaType = MediaType.WILDCARD_TYPE;
        }
        
        // the stream is available if entity is IS or 
        // message contains XMLStreamReader or Reader
        boolean entityStreamAvailable = entityStreamAvailable();
        InputStream entityStream = null;
        if (!entityStreamAvailable) {
            // try create a stream if the entity is String or Number
            entityStream = convertEntityToStreamIfPossible();
            entityStreamAvailable = entityStream != null;
        } else if (entity instanceof InputStream) {
            entityStream = InputStream.class.cast(entity);
        }
        
        // we need to check for readers even if no IS is set - the readers may still do it
        List<ReaderInterceptor> readers = outMessage == null ? null : ProviderFactory.getInstance(outMessage)
            .createMessageBodyReaderInterceptor(cls, t, anns, mediaType, outMessage, entityStreamAvailable, null);
        
        if (readers != null) {
            try {
                if (entityBufferred) {
                    InputStream.class.cast(entity).reset();
                }
                
                Message responseMessage = getResponseMessage();
                responseMessage.put(Message.PROTOCOL_HEADERS, getHeaders());
                
                lastEntity = JAXRSUtils.readFromMessageBodyReader(readers, cls, t, 
                                                                       anns, 
                                                                       entityStream, 
                                                                       mediaType, 
                                                                       responseMessage);
                autoClose(cls, false); 
                return castLastEntity();
            } catch (Exception ex) {
                autoClose(cls, true);
                reportMessageHandlerProblem("MSG_READER_PROBLEM", cls, mediaType, ex);
            } finally {
                ProviderFactory pf = ProviderFactory.getInstance(outMessage);
                if (pf != null) {
                    pf.clearThreadLocalProxies();
                }
            }
        } else if (entity != null && cls.isAssignableFrom(entity.getClass())) {
            lastEntity = entity;
            return castLastEntity();
        } else if (entityStreamAvailable) {
            reportMessageHandlerProblem("NO_MSG_READER", cls, mediaType, null);
        } 
        
        throw new IllegalStateException("The entity is not backed by an input stream, entity class is : "
            + (entity != null ? entity.getClass().getName() : cls.getName()));
        
    }
    
    @SuppressWarnings("unchecked")
    private <T> T castLastEntity() {
        return (T)lastEntity;
    }
    
    public InputStream convertEntityToStreamIfPossible() {
        String stringEntity = null;
        if (entity instanceof String || entity instanceof Number) {
            stringEntity = entity.toString();
        }
        if (stringEntity != null) {
            try {
                return new ByteArrayInputStream(stringEntity.getBytes("UTF-8"));
            } catch (Exception ex) {
                throw new ProcessingException(ex);
            }
        } else {
            return null;
        }
    }
    
    private boolean entityStreamAvailable() {
        if (entity == null) {
            Message inMessage = getResponseMessage();    
            return inMessage != null && (inMessage.getContent(XMLStreamReader.class) != null
                || inMessage.getContent(Reader.class) != null);
        } else {
            return entity instanceof InputStream;
        }
    }
    
    private Message getResponseMessage() {
        Message responseMessage = outMessage.getExchange().getInMessage();
        if (responseMessage == null) {
            responseMessage = outMessage.getExchange().getInFaultMessage();    
        }
        return responseMessage;
    }
    
    private void reportMessageHandlerProblem(String name, Class<?> cls, MediaType ct, Throwable cause) {
        String errorMessage = JAXRSUtils.logMessageHandlerProblem(name, cls, ct);
        throw new ResponseProcessingException(this, errorMessage, cause);
    }
    
    protected void autoClose(Class<?> cls, boolean exception) {
        if (!entityBufferred && cls != InputStream.class
            && (exception || MessageUtils.isTrue(outMessage.getContextualProperty("response.stream.auto.close")))) {
            close();
        }
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
