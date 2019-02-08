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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.xkms.exception.ExceptionMapper;
import org.apache.cxf.xkms.handlers.Locator;
import org.apache.cxf.xkms.handlers.Register;
import org.apache.cxf.xkms.handlers.Validator;
import org.apache.cxf.xkms.handlers.XKMSConstants;
import org.apache.cxf.xkms.model.xkms.CompoundRequestType;
import org.apache.cxf.xkms.model.xkms.CompoundResultType;
import org.apache.cxf.xkms.model.xkms.KeyBindingAbstractType;
import org.apache.cxf.xkms.model.xkms.KeyBindingEnum;
import org.apache.cxf.xkms.model.xkms.KeyBindingType;
import org.apache.cxf.xkms.model.xkms.KeyUsageEnum;
import org.apache.cxf.xkms.model.xkms.LocateRequestType;
import org.apache.cxf.xkms.model.xkms.LocateResultType;
import org.apache.cxf.xkms.model.xkms.MessageAbstractType;
import org.apache.cxf.xkms.model.xkms.PendingRequestType;
import org.apache.cxf.xkms.model.xkms.RecoverRequestType;
import org.apache.cxf.xkms.model.xkms.RecoverResultType;
import org.apache.cxf.xkms.model.xkms.RegisterRequestType;
import org.apache.cxf.xkms.model.xkms.RegisterResultType;
import org.apache.cxf.xkms.model.xkms.ReissueRequestType;
import org.apache.cxf.xkms.model.xkms.ReissueResultType;
import org.apache.cxf.xkms.model.xkms.ResultMinorEnum;
import org.apache.cxf.xkms.model.xkms.ResultType;
import org.apache.cxf.xkms.model.xkms.RevokeRequestType;
import org.apache.cxf.xkms.model.xkms.RevokeResultType;
import org.apache.cxf.xkms.model.xkms.StatusRequestType;
import org.apache.cxf.xkms.model.xkms.StatusResultType;
import org.apache.cxf.xkms.model.xkms.StatusType;
import org.apache.cxf.xkms.model.xkms.UnverifiedKeyBindingType;
import org.apache.cxf.xkms.model.xkms.ValidateRequestType;
import org.apache.cxf.xkms.model.xkms.ValidateResultType;
import org.w3._2002._03.xkms_wsdl.XKMSPortType;

public class XKMSService implements XKMSPortType {

    protected static final Logger LOG = LogUtils.getL7dLogger(XKMSService.class);

    private String serviceName = XKMSConstants.XKMS_ENDPOINT_NAME;
    private List<Locator> locators = new ArrayList<>();
    private List<Validator> validators = new ArrayList<>();
    private List<Register> keyRegisterHandlers = new ArrayList<>();
    private boolean enableXKRSS = true;
    private boolean logExceptions;

    @Override
    public ReissueResultType reissue(ReissueRequestType request) {
        ReissueResultType response = XKMSResponseFactory.createResponse(request, new ReissueResultType());
        try {
            assertXKRSSAllowed();
            validateRequest(request);
            for (Register handler : keyRegisterHandlers) {
                if (handler.canProcess(request)) {
                    return handler.reissue(request, response);
                }
            }
            throw new UnsupportedOperationException("Service was unable to handle your request");
        } catch (Exception e) {
            return handleException("reissue", e, response);
        }
    }


    @Override
    public CompoundResultType compound(CompoundRequestType request) {
        validateRequest(request);

        RuntimeException ex = new UnsupportedOperationException("XKMS compound request is currently not supported");
        CompoundResultType response = XKMSResponseFactory.createResponse(request, new CompoundResultType());
        return handleException("compound", ex, response);
    }

    @Override
    public RegisterResultType register(RegisterRequestType request) {
        RegisterResultType response = XKMSResponseFactory.createResponse(request, new RegisterResultType());
        try {
            assertXKRSSAllowed();
            validateRequest(request);

            for (Register handler : keyRegisterHandlers) {
                if (handler.canProcess(request)) {
                    return handler.register(request, response);
                }
            }
            throw new UnsupportedOperationException("Service was unable to handle your request");
        } catch (Exception e) {
            return handleException("register", e, response);
        }
    }

    @Override
    public ResultType pending(PendingRequestType request) {
        validateRequest(request);

        return ExceptionMapper.toResponse(new UnsupportedOperationException("XKMS request is currently not supported"),
                XKMSResponseFactory.createResponse(request, new ResultType()));
    }

    @Override
    public RevokeResultType revoke(RevokeRequestType request) {
        RevokeResultType response = XKMSResponseFactory.createResponse(request, new RevokeResultType());
        try {
            assertXKRSSAllowed();
            validateRequest(request);
            for (Register handler : keyRegisterHandlers) {
                if (handler.canProcess(request)) {
                    return handler.revoke(request, response);
                }
            }
            throw new UnsupportedOperationException("Service was unable to handle your request");
        } catch (Exception e) {
            return handleException("revoke", e, response);
        }
    }

    @Override
    public LocateResultType locate(LocateRequestType request) {
        LocateResultType response = XKMSResponseFactory.createResponse(request, new LocateResultType());
        try {
            validateRequest(request);
            // Search
            for (Locator locator : locators) {
                UnverifiedKeyBindingType keyBinding = locator.locate(request);
                if (keyBinding != null) {
                    response.getUnverifiedKeyBinding().add(keyBinding);
                    return response;
                }
            }
            // No matches found
            response.setResultMinor(ResultMinorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_NO_MATCH.value());
            return response;
        } catch (Exception e) {
            return handleException("locate", e, response);
        }
    }

    @Override
    public RecoverResultType recover(RecoverRequestType request) {
        RecoverResultType response = XKMSResponseFactory.createResponse(request, new RecoverResultType());
        try {
            assertXKRSSAllowed();
            validateRequest(request);
            for (Register handler : keyRegisterHandlers) {
                if (handler.canProcess(request)) {
                    return handler.recover(request, response);
                }
            }
            throw new UnsupportedOperationException("Service was unable to handle your request");
        } catch (Exception e) {
            return handleException("recover", e, response);
        }
    }

    @Override
    public StatusResultType status(StatusRequestType request) {
        validateRequest(request);

        return ExceptionMapper.toResponse(new UnsupportedOperationException("XKMS request is currently not supported"),
                XKMSResponseFactory.createResponse(request, new StatusResultType()));
    }

    @Override
    public ValidateResultType validate(ValidateRequestType request) {
        ValidateResultType response = XKMSResponseFactory.createResponse(request, new ValidateResultType());
        try {
            validateRequest(request);

            // Create basic response
            KeyBindingType binding = createKeyBinding(response);

            // Validate request
            for (Validator validator : validators) {
                StatusType status = validator.validate(request);
                addValidationReasons(binding, status);
            }

            resolveValidationStatus(binding);
            return response;
        } catch (Exception e) {
            return handleException("recover", e, response);
        }
    }

    private <T extends ResultType> T handleException(String method, Exception e, T response) {
        if (logExceptions) {
            LOG.log(Level.SEVERE, "Error during " + method + ": " + e.getMessage(), e);
        }
        return ExceptionMapper.toResponse(e, response);
    }

    /**
     * Performs basic validations on request message to ensure XKMS standard is applied correctly.
     *
     * The following validations are performed: 1) Check if a request ID is set 2) Check if service name equals this
     * XKMS service instance
     *
     * @param request XKMS request
     */
    private void validateRequest(MessageAbstractType request) {
        // Check if ID is set
        if (request.getId() == null || request.getId().isEmpty()) {
            throw new IllegalArgumentException("Message Id is not set");
        }
        // Check if Service matches this instance
        if (!serviceName.equals(request.getService())) {
            throw new IllegalArgumentException("Service " + request.getService()
                                               + " is not responsible to process request");
        }
    }

    // TODO refactoring into factory class?
    public static KeyBindingType createKeyBinding(ValidateResultType result) {
        KeyBindingType binding = new KeyBindingType();
        binding.setId(XKMSResponseFactory.generateUniqueID());
        result.getKeyBinding().add(binding);

        StatusType status = new StatusType();
        binding.setStatus(status);

        return binding;
    }

    private void addValidationReasons(KeyBindingType binding, StatusType status) {
        StatusType resultStatus = binding.getStatus();
        resultStatus.getValidReason().addAll(status.getValidReason());
        resultStatus.getInvalidReason().addAll(status.getInvalidReason());
        resultStatus.getIndeterminateReason().addAll(status.getIndeterminateReason());
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setLocators(List<Locator> locators) {
        this.locators = locators;
    }

    public void setValidators(List<Validator> validators) {
        this.validators = validators;
    }

    public void setKeyRegisterHandlers(List<Register> keyRegisterHandlers) {
        this.keyRegisterHandlers = keyRegisterHandlers;
    }

    /**
     * http://www.w3.org/TR/xkms2/#XKMS_2_0_Section_4_1 [206]
     *
     * If no (or indeterminate) reasons are present total status is INDETERMINATE.
     * If no invalid and indeterminate reasons are present status is VALID.
     * If invalid reasons are present status is INVALID.
     *
     * @param binding KeyBinding to check validation reasons for
     */
    private void resolveValidationStatus(KeyBindingType binding) {
        StatusType status = binding.getStatus();
        status.setStatusValue(KeyBindingEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_INDETERMINATE);
        if (!status.getValidReason().isEmpty() && status.getIndeterminateReason().isEmpty()) {
            status.setStatusValue(KeyBindingEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_VALID);
        }
        if (!status.getInvalidReason().isEmpty()) {
            status.setStatusValue(KeyBindingEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_INVALID);
            // Only return invalid reasons
            status.getValidReason().clear();
        }
    }

    /**
     * Sets encryption, signature and exchange as key usage for provided keyBinding.
     *
     * @param keyBinding KeyBinding to set KeyUsage within
     */
    protected void setKeyUsageAll(KeyBindingAbstractType keyBinding) {
        keyBinding.getKeyUsage().add(KeyUsageEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_ENCRYPTION);
        keyBinding.getKeyUsage().add(KeyUsageEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_SIGNATURE);
        keyBinding.getKeyUsage().add(KeyUsageEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_EXCHANGE);
    }

    public void setEnableXKRSS(boolean enableXKRSS) {
        this.enableXKRSS = enableXKRSS;
        LOG.info("enableXKRSS:" + enableXKRSS);
    }

    public void setLogExceptions(boolean logExceptions) {
        this.logExceptions = logExceptions;
    }

    private void assertXKRSSAllowed() {
        if (!enableXKRSS) {
            throw new UnsupportedOperationException("XKRSS Operations are disabled");
        }
    }
}

