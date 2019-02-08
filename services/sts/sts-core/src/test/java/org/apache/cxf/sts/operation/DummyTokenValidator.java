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

package org.apache.cxf.sts.operation;

import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.ReceivedToken.STATE;
import org.apache.cxf.sts.token.validator.TokenValidator;
import org.apache.cxf.sts.token.validator.TokenValidatorParameters;
import org.apache.cxf.sts.token.validator.TokenValidatorResponse;
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;

/**
 * A Dummy TokenValidator for use in the unit tests. It validates the status of a
 * dummy BinarySecurityToken by checking the token value.
 */
public class DummyTokenValidator implements TokenValidator {

    public static final String TOKEN_TYPE =
        "http://dummy-token-type.com/dummy";

    public boolean canHandleToken(ReceivedToken validateTarget) {
        Object token = validateTarget.getToken();
        return (token instanceof BinarySecurityTokenType)
            && TOKEN_TYPE.equals(((BinarySecurityTokenType)token).getValueType());
    }

    public boolean canHandleToken(ReceivedToken validateTarget, String realm) {
        return canHandleToken(validateTarget);
    }

    public TokenValidatorResponse validateToken(TokenValidatorParameters tokenParameters) {
        TokenValidatorResponse response = new TokenValidatorResponse();
        ReceivedToken validateTarget = tokenParameters.getToken();
        validateTarget.setState(STATE.INVALID);
        response.setToken(validateTarget);

        if (validateTarget != null && validateTarget.isBinarySecurityToken()) {
            BinarySecurityTokenType binarySecurity =
                (BinarySecurityTokenType)validateTarget.getToken();
            if ("12345678".equals(binarySecurity.getValue())) {
                validateTarget.setState(STATE.VALID);
            }
        }

        return response;
    }


}
