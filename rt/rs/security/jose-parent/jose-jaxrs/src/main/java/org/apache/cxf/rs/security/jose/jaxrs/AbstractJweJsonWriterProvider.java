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
package org.apache.cxf.rs.security.jose.jaxrs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.ContentEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweEncryption;
import org.apache.cxf.rs.security.jose.jwe.JweEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweException;
import org.apache.cxf.rs.security.jose.jwe.JweHeaders;
import org.apache.cxf.rs.security.jose.jwe.JweUtils;
import org.apache.cxf.rs.security.jose.jwe.KeyEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;
import org.apache.cxf.rs.security.jose.jwk.KeyOperation;
import org.apache.cxf.rs.security.jose.jws.JwsJsonProducer;

public class AbstractJweJsonWriterProvider {
    protected static final Logger LOG = LogUtils.getL7dLogger(AbstractJweJsonWriterProvider.class);

    private List<JweEncryptionProvider> encProviders;

    public void setEncryptionProvider(JweEncryptionProvider provider) {
        setEncryptionProviders(Collections.singletonList(provider));
    }
    public void setEncryptionProviders(List<JweEncryptionProvider> providers) {
        this.encProviders = providers;
    }

    protected List<String> getPropertyLocations() {
        Message m = JAXRSUtils.getCurrentMessage();
        Object propLocsProp =
            MessageUtils.getContextualProperty(m, JoseConstants.RSSEC_ENCRYPTION_OUT_PROPS,
                                               JoseConstants.RSSEC_ENCRYPTION_PROPS);
        if (propLocsProp == null) {
            if (encProviders == null) {
                LOG.warning("JWE JSON init properties resource is not identified");
                throw new JweException(JweException.Error.NO_INIT_PROPERTIES);
            }
            return Collections.emptyList();
        }
        final List<String> propLocs;
        if (propLocsProp instanceof String) {
            String[] props = ((String)propLocsProp).split(",");
            propLocs = Arrays.asList(props);
        } else {
            propLocs = CastUtils.cast((List<?>)propLocsProp);
        }
        return propLocs;
    }
    
    protected List<JweEncryptionProvider> getInitializedEncryptionProviders(List<String> propLocs,
                                              JweHeaders sharedProtectedHeaders,
                                              List<JweHeaders> perRecipientUnprotectedHeaders) {
        if (encProviders != null) {
            return encProviders;
        }
        // The task is to have a single ContentEncryptionProvider instance, 
        // configured to generate CEK only once, paired with all the loaded
        // KeyEncryptionProviders to have JweEncryptionProviders initialized
        
        Message m = JAXRSUtils.getCurrentMessage();
        // Load all the properties
        List<Properties> propsList = new ArrayList<>(propLocs.size());
        for (int i = 0; i < propLocs.size(); i++) {
            propsList.add(JweUtils.loadJweProperties(m, propLocs.get(i)));
        }
        
        
        ContentAlgorithm ctAlgo = null;
        // This set is to find out how many key encryption algorithms are used
        // If only one then save it in the shared protected headers as opposed to
        // per-recipient specific not protected ones
        Set<KeyAlgorithm> keyAlgos = EnumSet.noneOf(KeyAlgorithm.class);
        
        List<KeyEncryptionProvider> keyProviders = new LinkedList<>();
        for (int i = 0; i < propLocs.size(); i++) {
            Properties props = propsList.get(i);
            ContentAlgorithm currentCtAlgo = JweUtils.getContentEncryptionAlgorithm(m, props, ContentAlgorithm.A128GCM);
            if (ctAlgo == null) {
                ctAlgo = currentCtAlgo;
            } else if (currentCtAlgo != null && !ctAlgo.equals(currentCtAlgo)) {
                // ctAlgo must be the same for all the recipients
                throw new JweException(JweException.Error.INVALID_CONTENT_ALGORITHM);
            }
            
            JweHeaders perRecipientUnprotectedHeader = perRecipientUnprotectedHeaders.get(i);
            KeyEncryptionProvider keyEncryptionProvider = 
                JweUtils.loadKeyEncryptionProvider(props, m, perRecipientUnprotectedHeader);
            if (keyEncryptionProvider.getAlgorithm() == KeyAlgorithm.DIRECT && propLocs.size() > 1) {
                throw new JweException(JweException.Error.INVALID_JSON_JWE);
            }
            keyProviders.add(keyEncryptionProvider);
            
            keyAlgos.add(perRecipientUnprotectedHeader.getKeyEncryptionAlgorithm());
        }
        if (ctAlgo == null) {
            throw new JweException(JweException.Error.INVALID_CONTENT_ALGORITHM);
        }
        sharedProtectedHeaders.setContentEncryptionAlgorithm(ctAlgo);
        
        List<JweEncryptionProvider> theEncProviders = new LinkedList<>();
        if (keyProviders.size() == 1 && keyProviders.get(0).getAlgorithm() == KeyAlgorithm.DIRECT) {
            JsonWebKey jwk = JwkUtils.loadJsonWebKey(m, propsList.get(0), KeyOperation.ENCRYPT);
            if (jwk != null) {
                ContentEncryptionProvider ctProvider = JweUtils.getContentEncryptionProvider(jwk, ctAlgo);
                JweEncryptionProvider encProvider = new JweEncryption(keyProviders.get(0), ctProvider);
                theEncProviders.add(encProvider);
            }
        } else {
            ContentEncryptionProvider ctProvider = JweUtils.getContentEncryptionProvider(ctAlgo, true);
            for (int i = 0; i < keyProviders.size(); i++) {
                JweEncryptionProvider encProvider = new JweEncryption(keyProviders.get(i), ctProvider);
                theEncProviders.add(encProvider);
            }
        }
        if (keyAlgos.size() == 1) {
            sharedProtectedHeaders.setKeyEncryptionAlgorithm(keyAlgos.iterator().next());
            for (int i = 0; i < perRecipientUnprotectedHeaders.size(); i++) {
                perRecipientUnprotectedHeaders.get(i).removeProperty(JoseConstants.JWE_HEADER_KEY_ENC_ALGORITHM);
            }
        }
        return theEncProviders;
    }
    protected void writeJws(JwsJsonProducer p, OutputStream os)
        throws IOException {
        byte[] bytes = StringUtils.toBytesUTF8(p.getJwsJsonSignedDocument());
        IOUtils.copy(new ByteArrayInputStream(bytes), os);
    }

}
