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

package org.apache.cxf.systest.jaxrs.security.jose;


import java.util.Collections;
import java.util.List;
import java.util.Properties;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionOutput;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweJsonConsumer;
import org.apache.cxf.rs.security.jose.jwe.JweUtils;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;
import org.apache.cxf.systest.jaxrs.security.Book;

@Path("/bookstore")
public class BookStore {

    public BookStore() {
    }

    @POST
    @Path("/books")
    @Produces({"text/plain", "application/json"})
    @Consumes("text/plain")
    public String echoText(String text) {
        return text;
    }
    
    @POST
    @Path("/books")
    @Produces("application/json")
    @Consumes("application/json")
    public Book echoBook(Book book) {
        return book;
    }

    @POST
    @Path("/books")
    @Produces("application/xml")
    @Consumes("application/xml")
    public Book echoBookXml(Book book) {
        return book;
    }
    
    @POST
    @Path("/books")
    @Produces("multipart/related")
    @Consumes("multipart/related")
    @Multipart(type = "application/xml")
    public Book echoBookMultipart(@Multipart(type = "application/xml") Book book) {
        return book;
    }
    @POST
    @Path("/booksModified")
    @Produces("multipart/related")
    @Consumes("multipart/related")
    @Multipart(type = "application/xml")
    public Book echoBookMultipartModified(@Multipart(type = "application/xml") Book book) {
        throw new InternalServerErrorException("Failure to detect the payload has been modified");
    }
    @POST
    @Path("/booksList")
    @Produces("multipart/related")
    @Consumes("multipart/related")
    @Multipart(type = "application/xml")
    public List<Book> echoBooksMultipart(@Multipart(type = "application/xml") List<Book> books) {
        return books;
    }

    @POST
    @Path("/books")
    @Produces({"text/plain"})
    @Consumes("application/jose+json")
    public String echoTextJweJsonIn(String jweJson) {
        
        JweJsonConsumer consumer = new JweJsonConsumer(jweJson);
        
        // Recipient 1
        final String recipient1PropLoc = "org/apache/cxf/systest/jaxrs/security/jwejson1.properties";
        final String recipient1Kid = "AesWrapKey";
        String recipient1DecryptedText = getRecipientText(consumer, recipient1PropLoc, recipient1Kid);
        
        // Recipient 2
        final String recipient2PropLoc = "org/apache/cxf/systest/jaxrs/security/jwejson2.properties";
        final String recipient2Kid = "AesWrapKey2";
        String recipient2DecryptedText = getRecipientText(consumer, recipient2PropLoc, recipient2Kid);
        return recipient1DecryptedText + recipient2DecryptedText;
    }

    private String getRecipientText(JweJsonConsumer consumer, String recipientPropLoc, String recipientKid) { 
        Message message = JAXRSUtils.getCurrentMessage();
        
        
        Properties recipientProps = JweUtils.loadJweProperties(message, recipientPropLoc);
        JsonWebKey recipientKey = JwkUtils.loadJwkSet(message, recipientProps, null).getKey(recipientKid);
        
        ContentAlgorithm contentEncryptionAlgorithm = JweUtils.getContentEncryptionAlgorithm(recipientProps);
        
        JweDecryptionProvider jweRecipient = 
            JweUtils.createJweDecryptionProvider(recipientKey, contentEncryptionAlgorithm);
        
        JweDecryptionOutput jweRecipientOutput = 
            consumer.decryptWith(jweRecipient,
                                 Collections.singletonMap("kid", recipientKid));
        return jweRecipientOutput.getContentText();
    }
}


