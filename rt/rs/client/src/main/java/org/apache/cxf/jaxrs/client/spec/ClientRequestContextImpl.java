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
package org.apache.cxf.jaxrs.client.spec;

import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.List;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.jaxrs.client.ClientProviderFactory;
import org.apache.cxf.jaxrs.impl.AbstractRequestContextImpl;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;


public class ClientRequestContextImpl extends AbstractRequestContextImpl
    implements ClientRequestContext {

    public ClientRequestContextImpl(Message m,
                                    boolean responseContext) {
        super(m, responseContext);
    }

    @Override
    public MediaType getMediaType() {
        if (!hasEntity()) {
            return null;
        }
        Object mt = HttpUtils.getModifiableHeaders(m).getFirst(HttpHeaders.CONTENT_TYPE);
        return mt instanceof MediaType ? (MediaType)mt : JAXRSUtils.toMediaType(mt.toString());
    }

    @Override
    public Client getClient() {
        return (Client)m.getContextualProperty(Client.class.getName());
    }

    @Override
    public Configuration getConfiguration() {
        ClientProviderFactory cpf = ClientProviderFactory.getInstance(m);
        return cpf.getConfiguration(m);
    }

    private Object getMessageContent() {
        MessageContentsList objs = MessageContentsList.getContentsList(m);
        if (objs == null || objs.isEmpty()) {
            return null;
        }
        return objs.get(0);
    }

    @Override
    public Object getEntity() {
        return getMessageContent();
    }

    @Override
    public Annotation[] getEntityAnnotations() {
        Annotation[] anns = (Annotation[])m.get(Annotation.class.getName());
        return anns == null ? new Annotation[] {} : anns;
    }

    @Override
    public Class<?> getEntityClass() {
        Object entity = getEntity();
        return entity == null ? null : entity.getClass();
    }

    @Override
    public Type getEntityType() {
        Type t = m.get(Type.class);
        return t != null ? t : getEntityClass();
    }

    @Override
    public OutputStream getEntityStream() {
        return m.getContent(OutputStream.class);
    }

    @Override
    public boolean hasEntity() {
        return getEntity() != null;
    }

    @Override
    public void setEntity(Object entity, Annotation[] anns, MediaType mt) {
        if (mt != null) {
            MultivaluedMap<String, Object> headers = getHeaders();
            headers.putSingle(HttpHeaders.CONTENT_TYPE, mt);
            m.put(Message.CONTENT_TYPE, mt.toString());
        }
        if (anns != null) {
            m.put(Annotation.class.getName(), anns);
        }
        doSetEntity(entity);
    }

    @Override
    public void setEntity(Object entity) {
        doSetEntity(entity);
    }

    private void doSetEntity(Object entity) {
        Object actualEntity = InjectionUtils.getEntity(entity);
        m.setContent(List.class, actualEntity == null ? new MessageContentsList()
            : new MessageContentsList(actualEntity));
        if (entity != null) {
            final Type type;
            if (GenericEntity.class.isAssignableFrom(entity.getClass())) {
                type = ((GenericEntity<?>)entity).getType();
            } else {
                type = entity.getClass();
            }
            m.put(Type.class, type);
            m.remove("org.apache.cxf.empty.request");
        }

    }

    @Override
    public URI getUri() {
        String requestURI = (String)m.get(Message.REQUEST_URI);
        if (requestURI  == null) {
            return null;
        }
        if (requestURI.startsWith("/")) {
            String endpointAddress = (String)m.get(Message.ENDPOINT_ADDRESS);
            requestURI = requestURI.length() == 1 ? endpointAddress : endpointAddress + requestURI;
        }
        return URI.create(requestURI);
    }

    @Override
    public void setEntityStream(OutputStream os) {
        m.setContent(OutputStream.class, os);

    }

    @Override
    public void setUri(URI requestURI) {
        m.put(Message.ENDPOINT_ADDRESS, requestURI.toString());
        m.put(Message.REQUEST_URI, requestURI.toString());

    }

    @Override
    public MultivaluedMap<String, Object> getHeaders() {
        h = null;
        return HttpUtils.getModifiableHeaders(m);
    }

    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        h = null;
        return HttpUtils.getModifiableStringHeaders(m);
    }

}
