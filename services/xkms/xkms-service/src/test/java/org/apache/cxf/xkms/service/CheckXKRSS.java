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
package org.apache.cxf.xkms.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.xkms.handlers.Register;
import org.apache.cxf.xkms.handlers.XKMSConstants;
import org.apache.cxf.xkms.model.extensions.ResultDetails;
import org.apache.cxf.xkms.model.xkms.RecoverRequestType;
import org.apache.cxf.xkms.model.xkms.RecoverResultType;
import org.apache.cxf.xkms.model.xkms.RegisterRequestType;
import org.apache.cxf.xkms.model.xkms.RegisterResultType;
import org.apache.cxf.xkms.model.xkms.ReissueRequestType;
import org.apache.cxf.xkms.model.xkms.ReissueResultType;
import org.apache.cxf.xkms.model.xkms.RequestAbstractType;
import org.apache.cxf.xkms.model.xkms.ResultMajorEnum;
import org.apache.cxf.xkms.model.xkms.ResultMinorEnum;
import org.apache.cxf.xkms.model.xkms.ResultType;
import org.apache.cxf.xkms.model.xkms.RevokeRequestType;
import org.apache.cxf.xkms.model.xkms.RevokeResultType;

import org.junit.Assert;
import org.junit.Test;

public class CheckXKRSS {
    @Test
    public void checkRegisterWithXKRSS() {
        RegisterRequestType request = new RegisterRequestType();
        request.setId("1");
        request.setService(XKMSConstants.XKMS_ENDPOINT_NAME);
        RegisterResultType result = createXKMSService(true).register(request);
        showResult(result);
        assertSuccess(result);
    }

    @Test
    public void checkRegisterWithoutXKRSS() {
        RegisterRequestType request = new RegisterRequestType();
        request.setId("1");
        request.setService(XKMSConstants.XKMS_ENDPOINT_NAME);
        createXKMSService(false).register(request);
        RegisterResultType result = createXKMSService(false).register(request);
        assertNotSupported(result);
    }

    @Test
    public void checkRevokeWithXKRSS() {
        RegisterRequestType request = new RegisterRequestType();
        request.setId("1");
        request.setService(XKMSConstants.XKMS_ENDPOINT_NAME);
        ResultType result = createXKMSService(true).register(request);
        assertSuccess(result);
    }

    @Test
    public void checkRevokeWithoutXKRSS() {
        RevokeRequestType request = new RevokeRequestType();
        request.setId("1");
        request.setService(XKMSConstants.XKMS_ENDPOINT_NAME);
        ResultType result = createXKMSService(false).revoke(request);
        assertNotSupported(result);
    }

    @Test
    public void checkRecoverWithXKRSS() {
        RecoverRequestType request = new RecoverRequestType();
        request.setId("1");
        request.setService(XKMSConstants.XKMS_ENDPOINT_NAME);
        ResultType result = createXKMSService(true).recover(request);
        showResult(result);
        assertSuccess(result);
    }

    @Test
    public void checkRecoverWithoutXKRSS() {
        RecoverRequestType request = new RecoverRequestType();
        request.setId("1");
        request.setService(XKMSConstants.XKMS_ENDPOINT_NAME);
        ResultType result = createXKMSService(false).recover(request);
        assertNotSupported(result);
    }

    private void assertNotSupported(ResultType result) {
        Assert.assertEquals(ResultMajorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_SENDER.value(),
                            result.getResultMajor());
        Assert.assertEquals(ResultMinorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_MESSAGE_NOT_SUPPORTED.value(),
                            result.getResultMinor());
    }

    private void assertSuccess(ResultType result) {
        Assert.assertEquals(ResultMajorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_SUCCESS.value(),
                            result.getResultMajor());
        Assert.assertNull(result.getResultMinor());
    }

    XKMSService createXKMSService(boolean enableXKRSS) {
        XKMSService xkmsService = new XKMSService();
        List<Register> keyRegisterHandlers = new ArrayList<>();
        keyRegisterHandlers.add(new Register() {

            @Override
            public RevokeResultType revoke(RevokeRequestType request, RevokeResultType response) {
                return response;
            }

            @Override
            public ReissueResultType reissue(ReissueRequestType request, ReissueResultType response) {
                return response;
            }

            @Override
            public RegisterResultType register(RegisterRequestType request, RegisterResultType response) {
                return response;
            }

            @Override
            public boolean canProcess(RequestAbstractType request) {
                return true;
            }

            @Override
            public RecoverResultType recover(RecoverRequestType request, RecoverResultType response) {
                return response;
            }
        });
        xkmsService.setKeyRegisterHandlers(keyRegisterHandlers);
        xkmsService.setEnableXKRSS(enableXKRSS);
        return xkmsService;
    }

    private void showResult(ResultType result) {
        String message = "";
        if (!ResultMajorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_SUCCESS.value().equals(result.getResultMajor())) {
            ResultDetails details = (ResultDetails)result.getMessageExtension().get(0);
            message = details.getDetails();
        }

        System.out.println("Major: " + result.getResultMajor() + ", Minor: " + result.getResultMinor()
                           + ", Message: " + message);

    }
}
