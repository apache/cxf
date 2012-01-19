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

package org.apache.cxf.jaxb;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.Duration;

import org.apache.cxf.common.logging.LogUtils;

/**
 * Utility class to construct javax.xml.datatype.Duration objects.
 *
 */
public final class DatatypeFactory {
    
    public static final Duration PT0S;
    private static final Logger LOG = LogUtils.getL7dLogger(DatatypeFactory.class, "CommonUtilityMessages");
   

    static {
        PT0S = createDuration("PT0S");
    }

    /**
     * prevents instantiation
     *
     */
    private DatatypeFactory() {
        
    }

    public static Duration createDuration(String s) {
        try {
            return javax.xml.datatype.DatatypeFactory.newInstance().newDuration(s);
        } catch (DatatypeConfigurationException ex) {
            LogUtils.log(LOG, Level.SEVERE, "DATATYPE_FACTORY_INSTANTIATION_EXC", ex);
        }
        return null;
    }
}
