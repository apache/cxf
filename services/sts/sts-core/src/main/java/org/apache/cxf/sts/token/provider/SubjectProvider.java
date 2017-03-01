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
package org.apache.cxf.sts.token.provider;

import org.apache.wss4j.common.saml.bean.SubjectBean;

/**
 * An interface that allows a pluggable way of obtaining a SAML SubjectBean used to populate the
 * Subject of a SAML Assertion.
 */
public interface SubjectProvider {

    /**
     * Get a SubjectBean object.
     */
    SubjectBean getSubject(SubjectProviderParameters subjectProviderParameters);

}
