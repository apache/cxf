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

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.DestroyFailedException;
import javax.security.auth.callback.CallbackHandler;

import org.w3c.dom.Document;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSPasswordCallback;

@Path("metadata")
public class MetadataService extends AbstractSSOSpHandler {
    protected static final Logger LOG = LogUtils.getL7dLogger(MetadataService.class);
    protected static final ResourceBundle BUNDLE = BundleUtils.getBundle(MetadataService.class);

    private String serviceAddress;
    private String assertionConsumerServiceAddress;
    private String logoutServiceAddress;
    private boolean addEndpointAddressToContext;

    @GET
    @Produces("text/xml")
    public Document getMetadata() {
        try {
            MetadataWriter metadataWriter = new MetadataWriter();

            Crypto crypto = getSignatureCrypto();
            if (crypto == null) {
                LOG.fine("No crypto instance of properties file configured for signature");
                throw ExceptionUtils.toInternalServerErrorException(null, null);
            }
            String signatureUser = getSignatureUsername();
            if (signatureUser == null) {
                LOG.fine("No user configured for signature");
                throw ExceptionUtils.toInternalServerErrorException(null, null);
            }
            CallbackHandler callbackHandler = getCallbackHandler();
            if (callbackHandler == null) {
                LOG.fine("No CallbackHandler configured to supply a password for signature");
                throw ExceptionUtils.toInternalServerErrorException(null, null);
            }

            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
            cryptoType.setAlias(signatureUser);
            X509Certificate[] issuerCerts = crypto.getX509Certificates(cryptoType);
            if (issuerCerts == null) {
                throw new Exception(
                    "No issuer certs were found to sign the request using name: " + signatureUser
                );
            }

            // Get the password
            WSPasswordCallback[] cb = {new WSPasswordCallback(signatureUser, WSPasswordCallback.SIGNATURE)};
            callbackHandler.handle(cb);
            String password = cb[0].getPassword();

            // Get the private key
            PrivateKey privateKey = crypto.getPrivateKey(signatureUser, password);

            if (addEndpointAddressToContext) {
                Message message = JAXRSUtils.getCurrentMessage();
                String rawPath = (String)message.get("http.base.path");
                return metadataWriter.getMetaData(rawPath + serviceAddress,
                                                  rawPath + assertionConsumerServiceAddress,
                                                  rawPath + logoutServiceAddress,
                                                  privateKey, issuerCerts[0],
                                                  true);
            }
            Document metadata = metadataWriter.getMetaData(serviceAddress, assertionConsumerServiceAddress,
                                              logoutServiceAddress,
                                              privateKey, issuerCerts[0],
                                              true);

            // Clean the private key from memory when we're done
            try {
                privateKey.destroy();
            } catch (DestroyFailedException ex) {
                // ignore
            }

            return metadata;
        } catch (Exception ex) {
            LOG.log(Level.FINE, ex.getMessage(), ex);
            throw ExceptionUtils.toInternalServerErrorException(ex, null);
        }
    }


    protected void reportError(String code) {
        org.apache.cxf.common.i18n.Message errorMsg =
            new org.apache.cxf.common.i18n.Message(code, BUNDLE);
        LOG.warning(errorMsg.toString());
    }

    public String getServiceAddress() {
        return serviceAddress;
    }

    public void setServiceAddress(String serviceAddress) {
        this.serviceAddress = serviceAddress;
    }

    public String getLogoutServiceAddress() {
        return logoutServiceAddress;
    }

    public void setLogoutServiceAddress(String logoutServiceAddress) {
        this.logoutServiceAddress = logoutServiceAddress;
    }

    public void setAddEndpointAddressToContext(boolean add) {
        addEndpointAddressToContext = add;
    }


    public String getAssertionConsumerServiceAddress() {
        return assertionConsumerServiceAddress;
    }


    public void setAssertionConsumerServiceAddress(String assertionConsumerServiceAddress) {
        this.assertionConsumerServiceAddress = assertionConsumerServiceAddress;
    }
}
