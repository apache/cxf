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

package org.apache.cxf.sts.token.canceller;

import org.apache.cxf.sts.request.ReceivedToken;


/**
 * An interface that can cancel a security token.
 */

public interface TokenCanceller {

    /**
     * boolean for enabling/disabling verification of proof of possession.
     */
    void setVerifyProofOfPossession(boolean verifyProofOfPossession);

    /**
     * Return true if this TokenCanceller implementation is able to cancel a token
     * that corresponds to the given token.
     */
    boolean canHandleToken(ReceivedToken cancelTarget);

    /**
     * Cancel a token given a TokenCancellerParameters
     */
    TokenCancellerResponse cancelToken(TokenCancellerParameters tokenParameters);

}
