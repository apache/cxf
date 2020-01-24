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

import java.util.UUID;

import org.apache.cxf.xkms.handlers.XKMSConstants;
import org.apache.cxf.xkms.itests.BasicIntegrationTest;
import org.apache.cxf.xkms.model.extensions.ResultDetails;
import org.apache.cxf.xkms.model.xkms.RegisterRequestType;
import org.apache.cxf.xkms.model.xkms.RegisterResultType;
import org.apache.cxf.xkms.model.xkms.ResultMajorEnum;
import org.apache.cxf.xkms.model.xkms.ResultMinorEnum;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.PaxExam;

import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

@RunWith(PaxExam.class)
public class XKRSSDisableTest extends BasicIntegrationTest {

    @Configuration
    public Option[] getConfig() {
        return OptionUtils.combine(
            super.getConfig(),
            editConfigurationFilePut("etc/org.apache.cxf.xkms.cfg", "xkms.enableXKRSS", "false")
        );
    }

    @Test
    public void testRegisterShouldBeDisabled() {
        RegisterRequestType request = new RegisterRequestType();
        request.setService(XKMSConstants.XKMS_ENDPOINT_NAME);
        request.setId(UUID.randomUUID().toString());
        RegisterResultType result = xkmsService.register(request);
        Assert.assertEquals(ResultMajorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_SENDER.value(),
                            result.getResultMajor());
        Assert.assertEquals(ResultMinorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_MESSAGE_NOT_SUPPORTED.value(),
                            result.getResultMinor());
        ResultDetails message = (ResultDetails)result.getMessageExtension().get(0);
        Assert.assertEquals("XKRSS Operations are disabled", message.getDetails());
    }

}
