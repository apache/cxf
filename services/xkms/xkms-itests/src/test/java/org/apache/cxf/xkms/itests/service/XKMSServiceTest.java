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
package org.apache.cxf.xkms.itests.service;

import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;

import org.apache.cxf.xkms.handlers.Applications;
import org.apache.cxf.xkms.handlers.XKMSConstants;
import org.apache.cxf.xkms.itests.BasicIntegrationTest;
import org.apache.cxf.xkms.model.xkms.LocateRequestType;
import org.apache.cxf.xkms.model.xkms.LocateResultType;
import org.apache.cxf.xkms.model.xkms.MessageAbstractType;
import org.apache.cxf.xkms.model.xkms.PrototypeKeyBindingType;
import org.apache.cxf.xkms.model.xkms.QueryKeyBindingType;
import org.apache.cxf.xkms.model.xkms.RegisterRequestType;
import org.apache.cxf.xkms.model.xkms.RegisterResultType;
import org.apache.cxf.xkms.model.xkms.ResultMajorEnum;
import org.apache.cxf.xkms.model.xkms.ResultMinorEnum;
import org.apache.cxf.xkms.model.xkms.UnverifiedKeyBindingType;
import org.apache.cxf.xkms.model.xkms.UseKeyWithType;
import org.apache.cxf.xkms.model.xmldsig.KeyInfoType;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;

@RunWith(PaxExam.class)
public class XKMSServiceTest extends BasicIntegrationTest {
    private static final org.apache.cxf.xkms.model.xkms.ObjectFactory XKMS_OF =
        new org.apache.cxf.xkms.model.xkms.ObjectFactory();

    @Test
    public void testLocatePKIX() throws URISyntaxException, Exception {
        LocateRequestType request = XKMS_OF.createLocateRequestType();
        setGenericRequestParams(request);
        QueryKeyBindingType queryKeyBindingType = XKMS_OF.createQueryKeyBindingType();

        UseKeyWithType useKeyWithType = XKMS_OF.createUseKeyWithType();
        useKeyWithType.setIdentifier("CN=Dave, OU=Apache, O=CXF, L=CGN, ST=NRW, C=DE");
        useKeyWithType.setApplication(Applications.PKIX.getUri());

        locateCertificate(request, queryKeyBindingType, useKeyWithType);
    }

    @Test
    public void testLocateByEndpoint() throws URISyntaxException, Exception {
        LocateRequestType request = XKMS_OF.createLocateRequestType();
        setGenericRequestParams(request);
        QueryKeyBindingType queryKeyBindingType = XKMS_OF.createQueryKeyBindingType();

        UseKeyWithType useKeyWithType = XKMS_OF.createUseKeyWithType();
        useKeyWithType.setIdentifier("http://localhost:8080/services/TestService");
        useKeyWithType.setApplication(Applications.SERVICE_ENDPOINT.getUri());

        locateCertificate(request, queryKeyBindingType, useKeyWithType);
    }

    private void locateCertificate(LocateRequestType request,
                                   QueryKeyBindingType queryKeyBindingType,
                                   UseKeyWithType useKeyWithType) {
        queryKeyBindingType.getUseKeyWith().add(useKeyWithType);

        request.setQueryKeyBinding(queryKeyBindingType);
        LocateResultType result = xkmsService.locate(request);
        assertSuccess(result);
        List<UnverifiedKeyBindingType> keyBinding = result.getUnverifiedKeyBinding();
        Assert.assertEquals(1, keyBinding.size());
        KeyInfoType keyInfo = keyBinding.get(0).getKeyInfo();
        Assert.assertNotNull(keyInfo);
    }

    private void setGenericRequestParams(MessageAbstractType request) {
        request.setService(XKMSConstants.XKMS_ENDPOINT_NAME);
        request.setId(UUID.randomUUID().toString());
    }

    @Test
    public void testEmptyRegister() throws URISyntaxException, Exception {
        RegisterRequestType request = new RegisterRequestType();
        setGenericRequestParams(request);
        RegisterResultType result = xkmsService.register(request);
        Assert.assertEquals(ResultMajorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_SENDER.value(),
                            result.getResultMajor());
        Assert.assertEquals(ResultMinorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_FAILURE.value(),
                            result.getResultMinor());
    }

    @Test
    public void testRegisterWithoutKey() throws URISyntaxException, Exception {
        RegisterRequestType request = new RegisterRequestType();
        setGenericRequestParams(request);
        PrototypeKeyBindingType binding = new PrototypeKeyBindingType();
        KeyInfoType keyInfo = new KeyInfoType();
        binding.setKeyInfo(keyInfo);
        request.setPrototypeKeyBinding(binding);
        RegisterResultType result = xkmsService.register(request);
        Assert.assertEquals(ResultMajorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_SENDER.value(),
                            result.getResultMajor());
        Assert.assertEquals(ResultMinorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_FAILURE.value(),
                            result.getResultMinor());
    }

}
