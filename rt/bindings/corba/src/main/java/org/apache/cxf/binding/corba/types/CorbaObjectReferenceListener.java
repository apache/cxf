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

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.cxf.binding.corba.CorbaBindingException;
import org.apache.cxf.binding.corba.utils.CorbaObjectReferenceHelper;
import org.apache.cxf.binding.corba.utils.CorbaUtils;
import org.apache.cxf.common.util.SystemPropertyAction;
import org.omg.CORBA.ORB;


public class CorbaObjectReferenceListener extends AbstractCorbaTypeListener {

    /**
     * The URL schemes that the address of an object reference in a message may use, as a comma-separated
     * list. The default is "IOR,corbaloc". The address is resolved as soon as the reference is marshalled.
     * IOR and corbaloc addresses only describe the object, but a corbaname address makes the ORB contact the
     * naming service it names, and a file address reads a local file. So corbaname and file should only be
     * allowed if the addresses can be trusted. It is set as a system property.
     */
    public static final String ALLOWED_ADDRESS_SCHEMES = "org.apache.cxf.corba.objectReferenceAddressSchemes";

    private static final Set<String> ALLOWED_SCHEMES =
        Arrays.stream(SystemPropertyAction.getProperty(ALLOWED_ADDRESS_SCHEMES, "IOR,corbaloc").split(","))
            .map(scheme -> scheme.trim().toLowerCase(Locale.ENGLISH))
            .filter(scheme -> !scheme.isEmpty())
            .collect(Collectors.toSet());

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
        if (currentElement != null && "Address".equals(currentElement.getLocalPart())) {
            final org.omg.CORBA.Object ref;

            if (text.equals(CorbaObjectReferenceHelper.ADDRESSING_NAMESPACE_URI + "/anonymous")) {
                throw new RuntimeException("Anonymous endpoint reference types not supported");
            }

            checkAddressScheme(text);
            try {
                ref = CorbaUtils.importObjectReference(orb, text);
            } catch (org.omg.CORBA.BAD_PARAM ex) {
                throw new RuntimeException("Unable to resolve CORBA object with address " + text);
            }
            value.setReference(ref);
        }
    }

    private static void checkAddressScheme(String address) {
        String trimmed = address.trim();
        int index = trimmed.indexOf(':');
        String scheme = index > 0 ? trimmed.substring(0, index).toLowerCase(Locale.ENGLISH) : "";
        if (!ALLOWED_SCHEMES.contains(scheme)) {
            // don't include the address, so that the error doesn't reveal anything about it
            throw new CorbaBindingException("The object reference address scheme \"" + scheme
                + "\" is not allowed, see " + ALLOWED_ADDRESS_SCHEMES);
        }
    }
}
