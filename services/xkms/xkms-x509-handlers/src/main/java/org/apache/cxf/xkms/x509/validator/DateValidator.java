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

import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.xkms.handlers.Validator;
import org.apache.cxf.xkms.model.xkms.KeyBindingEnum;
import org.apache.cxf.xkms.model.xkms.ReasonEnum;
import org.apache.cxf.xkms.model.xkms.StatusType;
import org.apache.cxf.xkms.model.xkms.ValidateRequestType;

public class DateValidator implements Validator {

    private static final Logger LOG = LogUtils.getL7dLogger(DateValidator.class);

    /**
     * Checks if a certificate is within its validity period.
     *
     * @param certificate to check
     * @return the validity state of the certificate
     */
    public boolean isCertificateValid(X509Certificate certificate) {
        Instant now = Instant.now();

        try {
            certificate.checkValidity(Date.from(now));
        } catch (CertificateNotYetValidException | CertificateExpiredException e) {
            return false;
        }
        /*
         * TODO: clarify use of KeyUsage with customer if (null == certificate.getKeyUsage()) { return false; }
         * boolean[] keyUsage = certificate.getKeyUsage(); if (!keyUsage[KeyUsage.digitalSignature.ordinal()] ||
         * !keyUsage[KeyUsage.dataEncipherment.ordinal()] || keyUsage[KeyUsage.encipherOnly.ordinal()] ||
         * keyUsage[KeyUsage.decipherOnly.ordinal()]) { return false; }
         */
        return true;
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
            status.getValidReason().add(ReasonEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_VALIDITY_INTERVAL.value());
            status.setStatusValue(KeyBindingEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_VALID);
        } else {
            status.getInvalidReason().add(ReasonEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_VALIDITY_INTERVAL.value());
            status.setStatusValue(KeyBindingEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_INVALID);
        }
        return status;
    }

    public boolean isCertificateChainValid(List<X509Certificate> certificates) {
        if (certificates == null) {
            return false;
        }

        for (X509Certificate x509Certificate : certificates) {
            if (!isCertificateValid(x509Certificate)) {
                LOG.info("Certificate is expired: " + x509Certificate.getSubjectX500Principal());
                return false;
            }
        }
        return true;
    }

}
