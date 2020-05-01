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

package org.apache.cxf.xkms.x509.validator;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPath;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertStore;
import java.security.cert.CertStoreParameters;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.xkms.handlers.Validator;
import org.apache.cxf.xkms.model.xkms.KeyBindingEnum;
import org.apache.cxf.xkms.model.xkms.ReasonEnum;
import org.apache.cxf.xkms.model.xkms.StatusType;
import org.apache.cxf.xkms.model.xkms.ValidateRequestType;
import org.apache.cxf.xkms.x509.repo.CertificateRepo;

public class TrustedAuthorityValidator implements Validator {

    private static final Logger LOG = LogUtils.getL7dLogger(TrustedAuthorityValidator.class);

    final CertificateRepo certRepo;
    boolean enableRevocation;

    public TrustedAuthorityValidator(CertificateRepo certRepo) {
        this.certRepo = certRepo;
    }

    /**
     * Checks if a certificate chain is signed by a trusted authority.
     *
     * @param certificates to check
     * @return the validity state of the certificate
     */
    boolean isCertificateChainValid(List<X509Certificate> certificates) {
        X509Certificate targetCert = certificates.get(0);
        X509CertSelector selector = new X509CertSelector();
        selector.setCertificate(targetCert);
        try {
            List<X509Certificate> intermediateCerts = certRepo.getCaCerts();
            List<X509Certificate> trustedAuthorityCerts = certRepo.getTrustedCaCerts();
            Set<TrustAnchor> trustAnchors = asTrustAnchors(trustedAuthorityCerts);
            CertStoreParameters intermediateParams = new CollectionCertStoreParameters(intermediateCerts);
            CertStoreParameters certificateParams = new CollectionCertStoreParameters(certificates);
            PKIXBuilderParameters pkixParams = new PKIXBuilderParameters(trustAnchors, selector);
            pkixParams.addCertStore(CertStore.getInstance("Collection", intermediateParams));
            pkixParams.addCertStore(CertStore.getInstance("Collection", certificateParams));
            pkixParams.setRevocationEnabled(false);

            CertPathBuilder builder = CertPathBuilder.getInstance("PKIX");
            CertPath certPath = builder.build(pkixParams).getCertPath();

            // Now validate the CertPath (including CRL checking)
            pkixParams.setRevocationEnabled(enableRevocation);
            if (enableRevocation) {
                List<X509CRL> crls = certRepo.getCRLs();
                if (!crls.isEmpty()) {
                    CertStoreParameters crlParams = new CollectionCertStoreParameters(crls);
                    pkixParams.addCertStore(CertStore.getInstance("Collection", crlParams));
                }
            }

            CertPathValidator validator = CertPathValidator.getInstance("PKIX");
            validator.validate(certPath, pkixParams);

        } catch (InvalidAlgorithmParameterException e) {
            LOG.log(Level.WARNING,
                    "Invalid algorithm parameter by certificate chain validation. "
                        + "It is likely that issuer certificates are not found in XKMS trusted storage. "
                        + e.getMessage(), e);
            return false;
        } catch (NoSuchAlgorithmException e) {
            LOG.log(Level.WARNING, "Unknown algorithm by trust chain validation: " + e.getMessage(), e);
            return false;
        } catch (CertPathBuilderException e) {
            LOG.log(Level.WARNING, "Cannot build certification path: " + e.getMessage(), e);
            return false;
        } catch (CertPathValidatorException e) {
            LOG.log(Level.WARNING, "Cannot vaidate certification path: " + e.getMessage(), e);
            return false;
        }
        return true;
    }

    private Set<TrustAnchor> asTrustAnchors(List<X509Certificate> trustedAuthorityCerts) {
        return trustedAuthorityCerts.stream()
                .map(cert -> new TrustAnchor(cert, null))
                .collect(Collectors.toSet());
    }

    @Override
    public StatusType validate(ValidateRequestType request) {
        StatusType status = new StatusType();
        List<X509Certificate> certificates = ValidateRequestParser.parse(request);
        if (certificates == null || certificates.isEmpty()) {
            status.setStatusValue(KeyBindingEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_INDETERMINATE);
            status.getIndeterminateReason().add("http://www.cxf.apache.org/2002/03/xkms#RequestNotSupported");
        }
        if (isCertificateChainValid(certificates)) {
            status.getValidReason().add(ReasonEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_ISSUER_TRUST.value());
            status.setStatusValue(KeyBindingEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_VALID);
        } else {
            status.getInvalidReason().add(ReasonEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_ISSUER_TRUST.value());
            status.setStatusValue(KeyBindingEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_INVALID);
        }
        return status;
    }

    public boolean isEnableRevocation() {
        return enableRevocation;
    }

    public void setEnableRevocation(boolean enableRevocation) {
        this.enableRevocation = enableRevocation;
    }

}
