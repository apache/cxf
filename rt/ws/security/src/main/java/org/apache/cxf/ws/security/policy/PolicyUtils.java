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
package org.apache.cxf.ws.security.policy;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import javax.xml.namespace.QName;

import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.AbstractBinding;

/**
 * Some common functionality that can be shared for working with policies
 */
public final class PolicyUtils {

    private PolicyUtils() {
        // complete
    }

    public static Collection<AssertionInfo> getAllAssertionsByLocalname(
        AssertionInfoMap aim, String localname
    ) {
        Collection<AssertionInfo> sp11Ais = aim.get(new QName(SP11Constants.SP_NS, localname));
        Collection<AssertionInfo> sp12Ais = aim.get(new QName(SP12Constants.SP_NS, localname));

        if ((sp11Ais != null && !sp11Ais.isEmpty()) || (sp12Ais != null && !sp12Ais.isEmpty())) {
            Collection<AssertionInfo> ais = new HashSet<>();
            if (sp11Ais != null) {
                ais.addAll(sp11Ais);
            }
            if (sp12Ais != null) {
                ais.addAll(sp12Ais);
            }
            return ais;
        }

        return Collections.emptySet();
    }

    public static boolean assertPolicy(AssertionInfoMap aim, QName name) {
        Collection<AssertionInfo> ais = aim.getAssertionInfo(name);
        if (ais != null && !ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                ai.setAsserted(true);
            }
            return true;
        }
        return false;
    }

    public static boolean assertPolicy(AssertionInfoMap aim, String localname) {
        Collection<AssertionInfo> ais = getAllAssertionsByLocalname(aim, localname);
        if (!ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                ai.setAsserted(true);
            }
            return true;
        }
        return false;
    }

    public static AssertionInfo getFirstAssertionByLocalname(
        AssertionInfoMap aim, String localname
    ) {
        Collection<AssertionInfo> sp11Ais = aim.get(new QName(SP11Constants.SP_NS, localname));
        if (sp11Ais != null && !sp11Ais.isEmpty()) {
            return sp11Ais.iterator().next();
        }

        Collection<AssertionInfo> sp12Ais = aim.get(new QName(SP12Constants.SP_NS, localname));
        if (sp12Ais != null && !sp12Ais.isEmpty()) {
            return sp12Ais.iterator().next();
        }

        return null;
    }

    public static boolean isThereAnAssertionByLocalname(
        AssertionInfoMap aim, String localname
    ) {
        Collection<AssertionInfo> sp11Ais = aim.get(new QName(SP11Constants.SP_NS, localname));
        Collection<AssertionInfo> sp12Ais = aim.get(new QName(SP12Constants.SP_NS, localname));

        return (sp11Ais != null && !sp11Ais.isEmpty()) || (sp12Ais != null && !sp12Ais.isEmpty());
    }

    public static AbstractBinding getSecurityBinding(AssertionInfoMap aim) {

        AssertionInfo asymAis = PolicyUtils.getFirstAssertionByLocalname(aim, SPConstants.ASYMMETRIC_BINDING);
        if (asymAis != null) {
            asymAis.setAsserted(true);
            return (AbstractBinding)asymAis.getAssertion();
        }

        AssertionInfo symAis = PolicyUtils.getFirstAssertionByLocalname(aim, SPConstants.SYMMETRIC_BINDING);
        if (symAis != null) {
            symAis.setAsserted(true);
            return (AbstractBinding)symAis.getAssertion();
        }

        AssertionInfo transAis = PolicyUtils.getFirstAssertionByLocalname(aim, SPConstants.TRANSPORT_BINDING);
        if (transAis != null) {
            transAis.setAsserted(true);
            return (AbstractBinding)transAis.getAssertion();
        }

        return null;
    }

}
