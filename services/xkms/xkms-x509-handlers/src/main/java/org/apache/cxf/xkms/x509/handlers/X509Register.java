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
package org.apache.cxf.xkms.x509.handlers;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.JAXBElement;
import org.apache.cxf.xkms.handlers.Register;
import org.apache.cxf.xkms.model.xkms.KeyBindingEnum;
import org.apache.cxf.xkms.model.xkms.KeyBindingType;
import org.apache.cxf.xkms.model.xkms.PrototypeKeyBindingType;
import org.apache.cxf.xkms.model.xkms.RecoverRequestType;
import org.apache.cxf.xkms.model.xkms.RecoverResultType;
import org.apache.cxf.xkms.model.xkms.RegisterRequestType;
import org.apache.cxf.xkms.model.xkms.RegisterResultType;
import org.apache.cxf.xkms.model.xkms.ReissueRequestType;
import org.apache.cxf.xkms.model.xkms.ReissueResultType;
import org.apache.cxf.xkms.model.xkms.RequestAbstractType;
import org.apache.cxf.xkms.model.xkms.RespondWithEnum;
import org.apache.cxf.xkms.model.xkms.RevokeRequestType;
import org.apache.cxf.xkms.model.xkms.RevokeResultType;
import org.apache.cxf.xkms.model.xkms.StatusType;
import org.apache.cxf.xkms.model.xkms.UseKeyWithType;
import org.apache.cxf.xkms.model.xmldsig.KeyInfoType;
import org.apache.cxf.xkms.model.xmldsig.X509DataType;
import org.apache.cxf.xkms.x509.repo.CertificateRepo;
import org.apache.cxf.xkms.x509.utils.X509Utils;

public class X509Register implements Register {

    protected final CertificateFactory certFactory;
    private CertificateRepo certRepo;

    public X509Register(CertificateRepo certRepo) throws CertificateException {
        this.certRepo = certRepo;
        certFactory = CertificateFactory.getInstance("X.509");
    }

    @Override
    public boolean canProcess(RequestAbstractType request) {
        if (request instanceof RecoverRequestType) {
            return false;
        }
        List<String> respondWithList = request.getRespondWith();
        if ((respondWithList != null) && !(respondWithList.isEmpty())) {
            return respondWithList.contains((Object)RespondWithEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_X_509_CERT);
        }
        // Default handler
        return true;
    }

    @Override
    public RegisterResultType register(RegisterRequestType request, RegisterResultType response) {
        try {
            PrototypeKeyBindingType binding = request.getPrototypeKeyBinding();
            X509Utils.assertElementNotNull(binding, PrototypeKeyBindingType.class);
            KeyInfoType keyInfo = binding.getKeyInfo();
            X509Utils.assertElementNotNull(binding, KeyInfoType.class);
            List<UseKeyWithType> useKeyWithList = binding.getUseKeyWith();
            if (useKeyWithList == null || useKeyWithList.size() != 1) {
                throw new IllegalArgumentException("Exactly one useKeyWith element is supported");
                //TODO standard requires support for multiple useKeyWith attributes
            }
            UseKeyWithType useKeyWith = useKeyWithList.get(0);
            List<X509Certificate> certList = getCertsFromKeyInfo(keyInfo);
            if (certList.size() != 1) {
                throw new IllegalArgumentException("Must provide one X509Certificate");
            }
            X509Certificate cert = certList.get(0);
            certRepo.saveCertificate(cert, useKeyWith);

            KeyBindingType responseBinding = prepareResponseBinding(binding);
            response.getKeyBinding().add(responseBinding);
            return response;
        } catch (CertificateException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private KeyBindingType prepareResponseBinding(PrototypeKeyBindingType binding) {
        KeyBindingType responseBinding = new KeyBindingType();
        responseBinding.setKeyInfo(binding.getKeyInfo());
        StatusType status = new StatusType();
        status.setStatusValue(KeyBindingEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_VALID);
        responseBinding.setStatus(status);
        return responseBinding;
    }

    @Override
    public ReissueResultType reissue(ReissueRequestType request, ReissueResultType response) {
        throw new UnsupportedOperationException("This service does not support reissue");
    }

    @Override
    public RevokeResultType revoke(RevokeRequestType request, RevokeResultType response) {
        throw new UnsupportedOperationException("This service does not support revoke");
    }

    private List<X509Certificate> getCertsFromKeyInfo(KeyInfoType keyInfo) throws CertificateException {
        List<X509Certificate> certList = new ArrayList<>();
        for (Object key : keyInfo.getContent()) {
            if (key instanceof JAXBElement) {
                Object value = ((JAXBElement<?>) key).getValue();
                if (value instanceof X509DataType) {
                    X509DataType x509Data = (X509DataType) value;
                    List<Object> data = x509Data.getX509IssuerSerialOrX509SKIOrX509SubjectName();
                    for (Object certO : data) {
                        JAXBElement<?> certO2 = (JAXBElement<?>) certO;
                        if (certO2.getDeclaredType() == byte[].class) {
                            byte[] certContent = (byte[]) certO2.getValue();
                            X509Certificate cert = (X509Certificate) certFactory
                                    .generateCertificate(new ByteArrayInputStream(certContent));
                            certList.add(cert);
                        }
                    }
                }
            }

        }
        return certList;
    }

    @Override
    public RecoverResultType recover(RecoverRequestType request, RecoverResultType response) {
        throw new UnsupportedOperationException("Recover is currently not supported");
    }

}
