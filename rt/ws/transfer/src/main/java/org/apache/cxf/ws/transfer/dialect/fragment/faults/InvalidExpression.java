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
package org.apache.cxf.ws.transfer.dialect.fragment.faults;

import javax.xml.namespace.QName;

import org.apache.cxf.ws.transfer.dialect.fragment.FragmentDialectConstants;

/**
 * Definition of the InvalidExpression SOAPFault.
 */
public class InvalidExpression extends FragmentFault {

    private static final long serialVersionUID = -1920756304737648952L;

    private static final String SUBCODE = "InvalidExpression";

    private static final String REASON = "The specified Language expression is invalid.";

    private static final String DETAIL = "The invalid language expression.";

    public InvalidExpression() {
        super(REASON, DETAIL,
                new QName(FragmentDialectConstants.FRAGMENT_2011_03_IRI, SUBCODE));
    }

}
