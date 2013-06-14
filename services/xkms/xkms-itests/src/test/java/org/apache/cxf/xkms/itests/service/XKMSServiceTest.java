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
import java.util.UUID;

import org.apache.cxf.xkms.itests.BasicIntegrationTest;
import org.apache.cxf.xkms.model.xkms.PrototypeKeyBindingType;
import org.apache.cxf.xkms.model.xkms.RegisterRequestType;
import org.apache.cxf.xkms.model.xkms.RegisterResultType;
import org.apache.cxf.xkms.model.xkms.ResultMajorEnum;
import org.apache.cxf.xkms.model.xkms.ResultMinorEnum;
import org.apache.cxf.xkms.model.xmldsig.KeyInfoType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;

@RunWith(PaxExam.class)
public class XKMSServiceTest extends BasicIntegrationTest {

    @Test
    public void testEmptyRegister() throws URISyntaxException, Exception {
        RegisterRequestType request = new RegisterRequestType();
        request.setId(UUID.randomUUID().toString());
        RegisterResultType result = xkmsService.register(request);
        Assert.assertEquals(ResultMajorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_SENDER.value(),
                            result.getResultMajor());
        Assert.assertEquals(ResultMinorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_FAILURE.value(),
                            result.getResultMinor());
    }

    @Test
    public void testRegisterWithoutKey() throws URISyntaxException, Exception {
        RegisterRequestType request = new RegisterRequestType();
        PrototypeKeyBindingType binding = new PrototypeKeyBindingType();
        KeyInfoType keyInfo = new KeyInfoType();
        binding.setKeyInfo(keyInfo);
        request.setPrototypeKeyBinding(binding);
        request.setId(UUID.randomUUID().toString());
        RegisterResultType result = xkmsService.register(request);
        Assert.assertEquals(ResultMajorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_SENDER.value(),
                            result.getResultMajor());
        Assert.assertEquals(ResultMinorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_FAILURE.value(),
                            result.getResultMinor());
    }
    
}
