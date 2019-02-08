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

package org.apache.cxf.rs.security.oauth.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import net.oauth.OAuth;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.provider.FormEncodingProvider;
import org.apache.cxf.rs.security.oauth.utils.OAuthConstants;

@Produces({"application/x-www-form-urlencoded" })
@Consumes({"application/x-www-form-urlencoded" })
@Provider
public class OOBResponseProvider implements
    MessageBodyReader<OOBAuthorizationResponse>, MessageBodyWriter<OOBAuthorizationResponse> {

    private FormEncodingProvider<Form> formProvider = new FormEncodingProvider<>();

    public boolean isReadable(Class<?> type, Type genericType,
                              Annotation[] annotations, MediaType mt) {
        return OOBAuthorizationResponse.class.isAssignableFrom(type);
    }

    public OOBAuthorizationResponse readFrom(
        Class<OOBAuthorizationResponse> clazz, Type genericType, Annotation[] annotations, MediaType mt,
        MultivaluedMap<String, String> headers, InputStream is) throws IOException {
        Form form = formProvider.readFrom(Form.class, Form.class, annotations, mt, headers, is);
        MultivaluedMap<String, String> data = form.asMap();
        OOBAuthorizationResponse resp = new OOBAuthorizationResponse();

        resp.setRequestToken(data.getFirst(OAuth.OAUTH_TOKEN));
        resp.setVerifier(data.getFirst(OAuth.OAUTH_VERIFIER));
        resp.setState(data.getFirst(OAuthConstants.X_OAUTH_STATE));

        return resp;
    }


    public long getSize(OOBAuthorizationResponse t, Class<?> type,
                        Type genericType, Annotation[] annotations,
                        MediaType mediaType) {
        return -1;
    }

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations,
                               MediaType mt) {
        return OOBAuthorizationResponse.class.isAssignableFrom(type);
    }

    public void writeTo(OOBAuthorizationResponse obj, Class<?> c, Type t,
                        Annotation[] anns,
                        MediaType mt, MultivaluedMap<String, Object> headers, OutputStream os)
        throws IOException, WebApplicationException {

        Form form = new Form(new MetadataMap<String, String>());
        form.param(OAuth.OAUTH_VERIFIER, obj.getVerifier());
        form.param(OAuth.OAUTH_TOKEN, obj.getRequestToken());
        if (obj.getState() != null) {
            form.param(OAuthConstants.X_OAUTH_STATE, obj.getState());
        }
        formProvider.writeTo(form, Form.class, Form.class, anns, mt, headers, os);
    }

}
