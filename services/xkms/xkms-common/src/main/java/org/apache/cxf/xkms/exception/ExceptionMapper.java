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

package org.apache.cxf.xkms.exception;

import org.apache.cxf.xkms.model.extensions.ResultDetails;
import org.apache.cxf.xkms.model.xkms.ResultMajorEnum;
import org.apache.cxf.xkms.model.xkms.ResultMinorEnum;
import org.apache.cxf.xkms.model.xkms.ResultType;

public final class ExceptionMapper {

    private ExceptionMapper() {
    }

    public static <T extends ResultType> T toResponse(Exception e, T result) {
        if (e instanceof XKMSException) {
            XKMSException xkmsEx = (XKMSException)e;
            initResultType(xkmsEx.getMessage(), xkmsEx.getResultMajor(), xkmsEx.getResultMinor(), result);
        } else if (e instanceof UnsupportedOperationException) {
            initResultType(e.getMessage(),
                                    ResultMajorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_SENDER,
                                    ResultMinorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_MESSAGE_NOT_SUPPORTED,
                                    result);
        } else if (e instanceof IllegalArgumentException) {
            initResultType(e.getMessage(),
                                    ResultMajorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_SENDER,
                                    ResultMinorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_FAILURE,
                                    result);
        } else {
            initResultType(e.getMessage(),
                                    ResultMajorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_RECEIVER,
                                    ResultMinorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_FAILURE,
                                    result);
        }
        return result;
    }

    public static XKMSException fromResponse(ResultType result) {
        ResultMajorEnum major = null;
        if ((result.getResultMajor() != null) && !result.getResultMajor().isEmpty()) {
            major = ResultMajorEnum.fromValue(result.getResultMajor());
        }
        if (major == ResultMajorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_SUCCESS) {
            return null;
        }
        ResultMinorEnum minor = null;
        if ((result.getResultMinor() != null) && !result.getResultMinor().isEmpty()) {
            minor = ResultMinorEnum.fromValue(result.getResultMinor());
        }

        String message = null;
        if (!result.getMessageExtension().isEmpty()) {
            message = ((ResultDetails) result.getMessageExtension().get(0)).getDetails();
        }
        return new XKMSException(major, minor, message);
    }

    private static <T extends ResultType> void initResultType(String message, ResultMajorEnum majorCode,
                                             ResultMinorEnum minorCode, T result) {
        result.setResultMajor((majorCode != null)
            ? majorCode.value() : ResultMajorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_RECEIVER.value());
        result.setResultMinor((minorCode != null)
            ? minorCode.value() : ResultMinorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_FAILURE.value());
        if (message != null) {
            ResultDetails resultDetails = new ResultDetails();
            resultDetails.setDetails(message);
            result.getMessageExtension().add(resultDetails);
        }
    }
}
