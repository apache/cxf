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

package org.apache.cxf.binding.corba.types;

import org.apache.cxf.binding.corba.utils.CorbaObjectReferenceHelper;
import org.apache.cxf.binding.corba.utils.CorbaUtils;

import org.omg.CORBA.ORB;


public class CorbaObjectReferenceListener extends AbstractCorbaTypeListener {

    private final CorbaObjectReferenceHandler value;
    private final ORB orb;

    public CorbaObjectReferenceListener(CorbaObjectHandler handler,
                                        ORB orbRef) {
        super(handler);
        orb = orbRef;
        value = (CorbaObjectReferenceHandler) handler;
        value.setReference(null);
    }

    public void processCharacters(String text) {
        //REVISIT, just checking the address for now.
        if ((currentElement != null) && (currentElement.getLocalPart().equals("Address"))) {
            org.omg.CORBA.Object ref = null;

            if (text.equals(CorbaObjectReferenceHelper.ADDRESSING_NAMESPACE_URI + "/anonymous")) {
                throw new RuntimeException("Anonymous endpoint reference types not supported");
            }

            try {
                ref = CorbaUtils.importObjectReference(orb, text);
            } catch (org.omg.CORBA.BAD_PARAM ex) {
                throw new RuntimeException("Unable to resolve CORBA object with address " + text);
            }
            value.setReference(ref);
        }
    }
}
