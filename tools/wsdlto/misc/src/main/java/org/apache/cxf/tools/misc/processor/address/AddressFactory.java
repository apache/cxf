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
package org.apache.cxf.tools.misc.processor.address;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.util.NameUtil;

public final class AddressFactory {
    private static final Logger LOG = LogUtils.getL7dLogger(AddressFactory.class);
    
    private static final String PREFIX = "org.apache.cxf.tools.misc.processor.address";
    private static final AddressFactory INSTANCE = new AddressFactory();

    private final Map<String, Address> addresses = new HashMap<String, Address>();
    
    private AddressFactory() {
    }

    public static AddressFactory getInstance() {
        return INSTANCE;
    }
    
    public Address getAddresser(final String name) {
        Address address = addresses.get(name);
        if (address != null) {
            return address;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(PREFIX);
        sb.append(".");
        sb.append(NameUtil.capitalize(name));
        sb.append("Address");
        try {
            address = (Address) Class.forName(sb.toString()).newInstance();
            addresses.put(name, address);
            LOG.log(Level.INFO, "FOUND_ADDRESSER", sb);
            return address;
        } catch (Exception e) {
            Message msg = new Message("FOUND_NO_ADDRESSER", LOG, sb);
            throw new ToolException(msg);
        } 
    }
}
