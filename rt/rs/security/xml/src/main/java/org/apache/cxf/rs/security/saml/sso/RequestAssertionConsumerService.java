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
package org.apache.cxf.rs.security.saml.sso;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.DataFormatException;

import javax.ws.rs.Encoded;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.w3c.dom.Document;

import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.rs.security.saml.DeflateEncoderDecoder;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.saml.ext.OpenSAMLUtil;
import org.opensaml.xml.XMLObject;

@Path("sso")
public class RequestAssertionConsumerService {
    private static final String SAML_RESPONSE = "SAMLResponse"; 
    private static final String RELAY_STATE = "RelayState";

    private boolean useDeflateEncoding = true;
    
    @POST
    @Produces(MediaType.APPLICATION_FORM_URLENCODED)
    public Response processSamlResponse(@Encoded @FormParam(RELAY_STATE) String relayState,
                                     @Encoded @FormParam(SAML_RESPONSE) String encodedSamlResponse) {
        org.opensaml.saml2.core.Response samlResponse = 
            readSAMLResponse(encodedSamlResponse);

        validateSamlResponse(samlResponse);
        
        // TODO: set the security context, 
        // perhaps using the cookie or adding some query parameter 
        // (relayState?) to the redirect URI
        
        // finally, redirect to the service provider endpoint
        String responseTo = samlResponse.getInResponseTo();
        UriBuilder builder = UriBuilder.fromPath(responseTo); 
        // if needed: builder.queryParam("RelayState", relayState);
        
        return Response.seeOther(builder.build()).build();
        
    }
    
    @GET
    public Response getSamlResponse(@Encoded @QueryParam(RELAY_STATE) String relayState,
                                    @Encoded @QueryParam(SAML_RESPONSE) String samlResponse) {
        return processSamlResponse(relayState, samlResponse);       
    }
    
    private org.opensaml.saml2.core.Response readSAMLResponse(String samlResponse) {
        if (StringUtils.isEmpty(samlResponse)) {
            throw new WebApplicationException(400);
        }
        InputStream tokenStream = null;
        try {
            byte[] deflatedToken = Base64Utility.decode(samlResponse);
            tokenStream = useDeflateEncoding() 
                ? new DeflateEncoderDecoder().inflateToken(deflatedToken)
                : new ByteArrayInputStream(deflatedToken); 
        } catch (Base64Exception ex) {
            throw new WebApplicationException(400);
        } catch (DataFormatException ex) {
            throw new WebApplicationException(400);
        }    
        
        Document responseDoc = null;
        try {
            responseDoc = DOMUtils.readXml(new InputStreamReader(tokenStream, "UTF-8"));
        } catch (Exception ex) {
            throw new WebApplicationException(400);
        }
        XMLObject responseObject = null;
        try {
            responseObject = OpenSAMLUtil.fromDom(responseDoc.getDocumentElement());
        } catch (WSSecurityException ex) {
            throw new WebApplicationException(400);
        }
        if (!(responseObject instanceof org.opensaml.saml2.core.Response)) {
            throw new WebApplicationException(400);
        }
        return (org.opensaml.saml2.core.Response)responseObject;
    }
    
    protected void validateSamlResponse(org.opensaml.saml2.core.Response samlResponse) {
        
    }
    
    public void setUseDeflateEncoding(boolean deflate) {
        useDeflateEncoding = deflate;
    }
    public boolean useDeflateEncoding() {
        return useDeflateEncoding;
    }
}
