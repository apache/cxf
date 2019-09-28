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
 * Definition of the UnknownDialect SOAPFault.
 */
public class UnknownDialect extends WSTransferFault {

    private static final long serialVersionUID = 7139243705016686015L;

    private static final String SUBCODE = "UnknownDialect";

    private static final String REASON = "The specified Dialect IRI is not known.";

    private static final String DETAIL = "The unknown IRI if specified";

    public UnknownDialect() {
        super(REASON, DETAIL,
                new QName(TransferConstants.TRANSFER_2011_03_NAMESPACE, SUBCODE));
    }

}
