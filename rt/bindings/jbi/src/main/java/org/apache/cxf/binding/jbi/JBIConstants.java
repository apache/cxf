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
package org.apache.cxf.binding.jbi;

import javax.xml.namespace.QName;


public final class JBIConstants {

    public static final String NS_JBI_BINDING = "http://cxf.apache.org/bindings/jbi";

    public static final String NS_JBI_WRAPPER = "http://java.sun.com/xml/ns/jbi/wsdl-11-wrapper";
    
    public static final QName JBI_WRAPPER_MESSAGE = new QName(NS_JBI_WRAPPER, "message");

    public static final QName JBI_WRAPPER_PART = new QName(NS_JBI_WRAPPER, "part");

    private JBIConstants() {
        //utility class
    }
}