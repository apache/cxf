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

import java.io.InputStream;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import org.apache.cxf.xkms.model.xkms.KeyBindingEnum;
import org.apache.cxf.xkms.model.xkms.ReasonEnum;
import org.apache.cxf.xkms.model.xkms.StatusType;
import org.apache.cxf.xkms.model.xkms.ValidateRequestType;

import org.junit.Assert;
import org.junit.Test;

public class DateValidatorTest extends BasicValidationTest {

    @Test
    public void validateDateOK() throws JAXBException {
        StatusType result = processRequest("/validateRequestOK.xml");
        Assert.assertEquals(KeyBindingEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_VALID, result.getStatusValue());
        Assert.assertFalse(result.getValidReason().isEmpty());
        Assert.assertEquals(ReasonEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_VALIDITY_INTERVAL.value(), result.getValidReason()
                .get(0));
    }

    @Test
    public void validateDateExpired() throws JAXBException {
        StatusType result = processRequest("/validateRequestExpired.xml");
        Assert.assertEquals(result.getStatusValue(), KeyBindingEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_INVALID);
        Assert.assertFalse(result.getInvalidReason().isEmpty());
        Assert.assertEquals(ReasonEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_VALIDITY_INTERVAL.value(), result
                .getInvalidReason().get(0));
    }

    private StatusType processRequest(String path) throws JAXBException {
        InputStream is = this.getClass().getResourceAsStream(path);
        @SuppressWarnings("unchecked")
        JAXBElement<ValidateRequestType> request = (JAXBElement<ValidateRequestType>) unmarshaller.unmarshal(is);
        DateValidator validator = new DateValidator();
        return validator.validate(request.getValue());

    }

}
