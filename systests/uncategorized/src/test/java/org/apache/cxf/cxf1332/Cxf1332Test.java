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

package org.apache.cxf.cxf1332;

import org.apache.cxf.test.AbstractCXFSpringTest;
import org.junit.Test;

/**
 * 
 */
public class Cxf1332Test extends AbstractCXFSpringTest {

    /**
     * @throws Exception
     */
    public Cxf1332Test() throws Exception {
    }

    @Test
    public void tryToSendStringArray() throws Exception {
        Cxf1332 client = getBean(Cxf1332.class, "client");
        String[] a = new String[] {"a", "b", "c"};
        client.hostSendData(a);
        assertArrayEquals(a, Cxf1332Impl.getLastStrings());
    }
    
    /** {@inheritDoc}*/
    @Override
    protected String[] getConfigLocations() {
        return new String[] {"classpath:/org/apache/cxf/cxf1332/beans.xml" };
    }
}
