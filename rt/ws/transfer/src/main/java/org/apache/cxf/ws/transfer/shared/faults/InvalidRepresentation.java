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

package org.apache.cxf.ws.transfer.shared.faults;

import javax.xml.namespace.QName;

import org.apache.cxf.ws.transfer.shared.TransferConstants;

/**
 * Definition of the InvalidRepresentation SOAPFault.
 */
public class InvalidRepresentation extends WSTransferFault {

    private static final long serialVersionUID = 330259919571428100L;

    private static final String SUBCODE = "InvalidRepresentation";

    private static final String REASON = "The supplied representation is invalid";

    public InvalidRepresentation() {
        super(REASON, null,
                new QName(TransferConstants.TRANSFER_2011_03_NAMESPACE, SUBCODE));
    }

}
