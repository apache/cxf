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

package org.apache.cxf.systest.beanincreationexception;

import org.apache.cxf.test.AbstractCXFSpringTest;
import org.apache.cxf.testutil.common.TestUtil;

import org.junit.Test;

/**
 *
 */
public class BeanInCreationExceptionTest extends AbstractCXFSpringTest {
    static String port = TestUtil.getPortNumber("springport");

    @Override
    protected String[] getConfigLocations() {
        return new String[] {"classpath:/org/apache/cxf/systest/beanincreationexception/beans.xml"};
    }

    @Test
    public void testBeanInCreationExceptionTest() throws Exception {
        //CXF-3805
        //if it gets in here, creation of the context worked.
    }
}
