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

package org.apache.cxf.tools.corba.common;

import javax.xml.namespace.QName;

public interface ReferenceConstants {

    String WSADDRESSING_NAMESPACE = "http://www.w3.org/2005/08/addressing";
    String WSADDRESSING_PREFIX = "wsa";
    String WSADDRESSING_LOCATION = "http://www.w3.org/2005/08/addressing/ws-addr.xsd";
    String WSADDRESSING_LOCAL_NAME = "EndpointReferenceType";
    QName  WSADDRESSING_TYPE = new QName(WSADDRESSING_NAMESPACE, WSADDRESSING_LOCAL_NAME);

    String REFERENCE_NAMESPACE = "http://schemas.iona.com/references";
    String REFERENCE_LOCAL_NAME = "Reference";
    QName  REFERENCE_TYPE = new QName(REFERENCE_NAMESPACE, REFERENCE_LOCAL_NAME);
    
    String getValue(String value);
}
