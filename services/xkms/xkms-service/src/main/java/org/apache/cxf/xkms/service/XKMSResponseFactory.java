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

import java.util.UUID;

import org.apache.cxf.xkms.model.xkms.RequestAbstractType;
import org.apache.cxf.xkms.model.xkms.ResultMajorEnum;
import org.apache.cxf.xkms.model.xkms.ResultType;

public final class XKMSResponseFactory {

    private XKMSResponseFactory() {
    }

    /**
     * @param request Request to generate response for
     * @return Returns response for provided request, with SUCCESS as default major result.
     */
    public static ResultType createResponse(RequestAbstractType request) {
        return createResponse(request, new ResultType());
    }

    /**
     * The following activities are performed:
     *
     * 1) generation and assigning of unique response ID
     * 2) copying of request id
     * 3) copying of service name
     * 4) copying of opaqueClientData
     * 5) setting major result to success
     *
     * @param request Request to be used for response
     * @param response Response to be enriched
     * @return Returns enriched response
     */
    public static <T extends ResultType> T createResponse(RequestAbstractType request, T response) {
        response.setId(generateUniqueID());

        copyRequestId(request, response);
        copyServiceName(request, response);
        copyOpaqueClientData(request, response);

        response.setResultMajor(ResultMajorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_SUCCESS.value());

        return response;
    }

    /**
     * Copies OpaqueClientData from request to response as per XKMS specification
     * http://www.w3.org/TR/xkms2/#XKMS_2_0_Section_1 [86]
     *
     * @param request Containing OpaqueClientData
     * @param response to be used for inserting OpaqueClientData
     * @return Response unmodified, except for including OpaqueClientData from request
     */
    public static ResultType copyOpaqueClientData(RequestAbstractType request, ResultType response) {
        response.setOpaqueClientData(request.getOpaqueClientData());
        return response;
    }

    /**
     * Copies Request ID to response as per XKMS specification http://www.w3.org/TR/xkms2/#XKMS_2_0_Section_1
     *
     * @param request Containing request ID
     * @param response to be used for inserting request ID
     * @return Response unmodified, except for setting request ID
     */
    public static ResultType copyRequestId(RequestAbstractType request, ResultType response) {
        response.setRequestId(request.getId());
        return response;
    }

    /**
     * Copies service name from request to response.
     *
     * @param request containing service name
     * @param response to be used for inserting service name
     * @return Response unmodified, except for setting service name
     */
    public static ResultType copyServiceName(RequestAbstractType request, ResultType response) {
        response.setService(request.getService());
        return response;
    }

    /**
     * @return Returns generated random UUID
     */
    public static String generateUniqueID() {
        return "I" + UUID.randomUUID().getMostSignificantBits();
    }

}
