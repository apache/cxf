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

import org.apache.cxf.workqueue.AutomaticWorkQueueImpl;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

/**
 * 
 */
public class OSGiAutomaticWorkQueue extends AutomaticWorkQueueImpl implements ManagedService {
    
    public OSGiAutomaticWorkQueue(String name) {
        super(name);
    }

    /** {@inheritDoc}*/
    public void updated(Dictionary d) throws ConfigurationException {
        String name = getName();
        String s = (String)d.get("org.apache.cxf.workqueue." + name + ".highWaterMark");
        if (s != null) {
            setHighWaterMark(Integer.parseInt(s));
        }
        s = (String)d.get("org.apache.cxf.workqueue." + name + ".lowWaterMark");
        if (s != null) {
            setLowWaterMark(Integer.parseInt(s));
        }
        s = (String)d.get("org.apache.cxf.workqueue." + name + ".initialSize");
        if (s != null) {
            setInitialSize(Integer.parseInt(s));
        }
        s = (String)d.get("org.apache.cxf.workqueue." + name + ".dequeueTimeout");
        if (s != null) {
            setDequeueTimeout(Long.parseLong(s));
        }
        s = (String)d.get("org.apache.cxf.workqueue." + name + ".queueSize");
        if (s != null) {
            setQueueSize(Integer.parseInt(s));
        } 

    }

}
