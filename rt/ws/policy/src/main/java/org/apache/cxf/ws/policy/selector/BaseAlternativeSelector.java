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

package org.apache.cxf.ws.policy.selector;


import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.ws.policy.AlternativeSelector;
import org.apache.neethi.Assertion;
import org.apache.neethi.builders.PrimitiveAssertion;


/**
 *
 */
public abstract class BaseAlternativeSelector implements AlternativeSelector {

    protected boolean isCompatibleWithRequest(List<Assertion> alternative,
                                   List<List<Assertion>> request) {
        if (request == null) {
            return true;
        }
        for (List<Assertion> r : request) {
            if (isCompatible(alternative, r)) {
                return true;
            }
        }
        return false;
    }

    protected boolean isCompatible(List<Assertion> alternative, List<Assertion> r) {
        List<QName> rNames = new ArrayList<>(r.size());
        for (Assertion ra : r) {
            rNames.add(ra.getName());
        }

        for (Assertion a : alternative) {
            for (Assertion ra : r) {
                if (a.equals(ra)) {
                    rNames.remove(ra.getName());
                    break;
                }
                // Workaround until Neethi assertions implementations do not override equals():
                // objects in lists can be different instances
                if ((a instanceof PrimitiveAssertion) && (ra instanceof PrimitiveAssertion)
                    && ((PrimitiveAssertion) a).equal(ra)) {
                    rNames.remove(ra.getName());
                    break;
                }
            }
        }
        return rNames.isEmpty();
    }
}
