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

package org.apache.cxf.rs.security.saml;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URI;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.jaxrs.ext.form.Form;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.impl.UriInfoImpl;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.provider.FormEncodingProvider;
import org.apache.cxf.message.Message;

public class SamlFormInHandler extends AbstractSamlBase64InHandler {

    private static final String SAML_ELEMENT = "SAMLToken";
    private static final String SAML_RELAY_STATE = "RelayState";
   
    private FormEncodingProvider provider = new FormEncodingProvider();
    
    public SamlFormInHandler() {
        provider.setExpectedEncoded(true);
    }
    
    public Response handleRequest(Message message, ClassResourceInfo resourceClass) {
        
        Form form = readFormData(message);    
        String assertion = form.getData().getFirst(SAML_ELEMENT);
        
        handleToken(message, assertion);         

        // redirect if needed
        String samlRequestURI = form.getData().getFirst(SAML_RELAY_STATE);
        if (samlRequestURI != null) {
            // RelayState may actually represent a reference to a transient local state
            // containing the actual REQUEST URI client was using before being redirected 
            // back to IDP - at the moment assume it's URI
            UriInfoImpl ui = new UriInfoImpl(message); 
            if (!samlRequestURI.startsWith(ui.getBaseUri().toString())) {
                return Response.status(302).location(URI.create(samlRequestURI)).build();
            }
        }
        // restore input stream
        CachedOutputStream os = new CachedOutputStream(); 
        form.getData().remove(SAML_ELEMENT);
        form.getData().remove(SAML_RELAY_STATE);
        try {
            provider.writeTo(form, Form.class, Form.class, new Annotation[]{}, 
                MediaType.APPLICATION_FORM_URLENCODED_TYPE, new MetadataMap<String, Object>(), os);
            message.setContent(InputStream.class, os.getInputStream());
        } catch (Exception ex) {
            throwFault(ex.getMessage(), ex);
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private Form readFormData(Message message) {
        try {
            return (Form)provider.readFrom((Class)Form.class, Form.class, 
                              new Annotation[]{}, MediaType.APPLICATION_FORM_URLENCODED_TYPE, 
                              new MetadataMap<String, String>(), 
                              message.getContent(InputStream.class));
        } catch (Exception ex) {
            throwFault("Error reading the form", ex);    
        }
        return null;
    }
}
