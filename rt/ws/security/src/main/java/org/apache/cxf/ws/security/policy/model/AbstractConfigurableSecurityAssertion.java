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
package org.apache.cxf.ws.security.policy.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.neethi.Assertion;

public abstract class AbstractConfigurableSecurityAssertion extends AbstractSecurityAssertion {

    protected List<Assertion> configurations;
    
    public AbstractConfigurableSecurityAssertion(SPConstants version) {
        super(version);
    }

    public void addConfiguration(Assertion assertion) {
        if (configurations == null) {
            configurations = new ArrayList<Assertion>();
        }
        configurations.add(assertion);
    }

    public List<Assertion> getConfigurations() {
        return configurations;
    }

    public Assertion getDefaultAssertion() {
        if (configurations != null) {
            return configurations.get(0);
        }
        return null;
    }

}
