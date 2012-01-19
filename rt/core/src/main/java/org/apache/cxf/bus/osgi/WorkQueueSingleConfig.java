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
package org.apache.cxf.bus.osgi;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.workqueue.AutomaticWorkQueueImpl;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

/**
 * Use the ManagedWorkQueueList config style instead
 */
@Deprecated
public class WorkQueueSingleConfig implements ManagedService {
    public static final String SERVICE_PID = "org.apache.cxf.workqueue";
    public static final String PROPERTY_PREFIX = "org.apache.cxf.workqueue";
    ManagedWorkQueueList workQueueList;
    
    public WorkQueueSingleConfig(ManagedWorkQueueList workQueueList) {
        this.workQueueList = workQueueList;
    }

    public void updated(@SuppressWarnings("rawtypes") Dictionary properties) throws ConfigurationException {
        if (properties == null) {
            return;
        }
        Dictionary<String, String> p = CastUtils.cast(properties);
        String names = (String)properties.get(PROPERTY_PREFIX + ".names");
        String[] nameAr = names.split(",");
        for (String name : nameAr) {
            updateQueue(name.trim(), p);
        }
    }

    private void updateQueue(String name, Dictionary<String, String> properties) 
        throws ConfigurationException {
        if (properties == null) {
            return;
        }
        Dictionary<String, String> queueProperties = new Hashtable<String, String>();
        Enumeration<?> it = properties.keys();
        while (it.hasMoreElements()) {
            String key = (String)it.nextElement();
            String prefix = PROPERTY_PREFIX + "." + name + ".";
            if (key.startsWith(prefix)) {
                String newKey = (String)key.substring(prefix.length());
                queueProperties.put(newKey, (String)properties.get(key));
            }
        }
        queueProperties.put(AutomaticWorkQueueImpl.PROPERTY_NAME, name);
        workQueueList.updated(name, queueProperties);
    }

    
}
