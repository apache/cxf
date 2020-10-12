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

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.xkms.handlers.Validator;
import org.apache.cxf.xkms.handlers.XKMSConstants;
import org.apache.cxf.xkms.model.xkms.KeyBindingEnum;
import org.apache.cxf.xkms.model.xkms.KeyUsageEnum;
import org.apache.cxf.xkms.model.xkms.StatusType;
import org.apache.cxf.xkms.model.xkms.ValidateRequestType;
import org.apache.cxf.xkms.x509.repo.CertificateRepo;

public class DirectTrustValidator implements Validator {

    private static final Logger LOG = LogUtils.getL7dLogger(DirectTrustValidator.class);

    private final CertificateRepo certRepo;

    public DirectTrustValidator(CertificateRepo certRepo) {
        this.certRepo = certRepo;
    }

    /**
     * Checks if a certificate is located in XKMS storage.
     *
     * @param certificate to check
     * @return true if certificate is found
     */
    public boolean isCertificateInRepo(X509Certificate certificate) {
        X509Certificate findCert = certRepo.findBySubjectDn(certificate.getSubjectDN().getName());
        return findCert != null;
    }

    @Override
    public StatusType validate(ValidateRequestType request) {
        StatusType status = new StatusType();

        if (request.getQueryKeyBinding() != null) {
            List<KeyUsageEnum> keyUsages = request.getQueryKeyBinding().getKeyUsage();
            if (keyUsages.contains(KeyUsageEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_SIGNATURE)) {
                List<X509Certificate> certificates = ValidateRequestParser.parse(request);
                if (certificates == null || certificates.isEmpty()) {
                    status.setStatusValue(KeyBindingEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_INDETERMINATE);
                    status.getIndeterminateReason().add("http://www.cxf.apache.org/2002/03/xkms#RequestNotSupported");
                    return status;
                }
                for (X509Certificate certificate : certificates) {
                    if (!isCertificateInRepo(certificate)) {
                        LOG.warning("Certificate is not found in XKMS repo and is not directly trusted: "
                                    + certificate.getSubjectDN().getName());
                        status.getInvalidReason().add(XKMSConstants.DIRECT_TRUST_VALIDATION);
                        status.setStatusValue(KeyBindingEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_INVALID);
                        return status;
                    }
                }
                status.getValidReason().add(XKMSConstants.DIRECT_TRUST_VALIDATION);
            }
        }

        status.setStatusValue(KeyBindingEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_VALID);

        return status;
    }
}
