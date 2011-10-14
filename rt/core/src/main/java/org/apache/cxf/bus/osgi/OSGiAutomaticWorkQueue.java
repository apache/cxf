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

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;


import org.apache.cxf.workqueue.AutomaticWorkQueueImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

/**
 * 
 */
public class OSGiAutomaticWorkQueue extends AutomaticWorkQueueImpl {
    static class WorkQueueList implements ManagedService {
        Map<String, OSGiAutomaticWorkQueue> queues 
            = new ConcurrentHashMap<String, OSGiAutomaticWorkQueue>();
        ServiceRegistration registration;
        Configuration config;
        Properties current = new Properties();

        
        public synchronized void register(BundleContext ctx, Configuration c) {
            Properties props = new Properties();
            props.put(Constants.SERVICE_PID, "org.apache.cxf.workqueues");  

            registration = ctx.registerService(ManagedService.class.getName(),  
                                               this, props);
            
            this.config = c;
        }
        
        public synchronized void updateProperty(String key, String val) {
            if (val != null) {
                current.put(key, val);
            } else {
                current.remove(key);
            }
            try {
                config.update(current);
            } catch (IOException e) {
                //ignore
            }
        }
        public synchronized void updated(Dictionary d) throws ConfigurationException {
            current.clear();
            if (d == null) {
                return;
            }
            Enumeration e = d.keys();
            while (e.hasMoreElements()) {
                String k = (String)e.nextElement();
                current.put(k, d.get(k));
            }
            String s = (String)d.get("org.apache.cxf.workqueue.names");
            if (s != null) {
                String s2[] = s.split(",");
                for (String name : s2) {
                    name = name.trim();
                    if (queues.containsKey(name)) {
                        queues.get(name).updated(d);
                    } else {
                        OSGiAutomaticWorkQueue wq = new OSGiAutomaticWorkQueue(name, this);
                        wq.updated(d);
                        wq.setShared(true);
                        queues.put(name, wq);
                    }
                }
            }
            if (registration != null) {
                registration.setProperties(d);
            }
        }
    };
    final WorkQueueList qlist;
    
    public OSGiAutomaticWorkQueue(String name, WorkQueueList ql) {
        super(name);
        qlist = ql;
    }

    
    public void setHighWaterMark(int hwm) {
        super.setHighWaterMark(hwm);
        qlist.updateProperty("org.apache.cxf.workqueue." + getName() + ".highWaterMark",
                             Integer.toString(getHighWaterMark()));
    }

    public void setLowWaterMark(int lwm) {
        super.setLowWaterMark(lwm);
        qlist.updateProperty("org.apache.cxf.workqueue." + getName() + ".lowWaterMark",
                             Integer.toString(getLowWaterMark()));
    }

    public void setInitialSize(int initialSize) {
        super.setInitialSize(initialSize);
        qlist.updateProperty("org.apache.cxf.workqueue." + getName() + ".initialSize",
                             Integer.toString(initialSize));
    }

    public void setQueueSize(int size) {
        super.setQueueSize(size);
        qlist.updateProperty("org.apache.cxf.workqueue." + getName() + ".queueSize",
                             Integer.toString(size));
    }

    public void setDequeueTimeout(long l) {
        super.setDequeueTimeout(l);
        qlist.updateProperty("org.apache.cxf.workqueue." + getName() + ".dequeueTimeout",
                             Long.toString(l));
    }

    /** {@inheritDoc}*/
    public void updated(Dictionary d) throws ConfigurationException {
        String name = getName();
        String s = (String)d.get("org.apache.cxf.workqueue." + name + ".highWaterMark");
        if (s != null) {
            super.setHighWaterMark(Integer.parseInt(s));
        }
        s = (String)d.get("org.apache.cxf.workqueue." + name + ".lowWaterMark");
        if (s != null) {
            super.setLowWaterMark(Integer.parseInt(s));
        }
        s = (String)d.get("org.apache.cxf.workqueue." + name + ".initialSize");
        if (s != null) {
            super.setInitialSize(Integer.parseInt(s));
        }
        s = (String)d.get("org.apache.cxf.workqueue." + name + ".dequeueTimeout");
        if (s != null) {
            super.setDequeueTimeout(Long.parseLong(s));
        }
        s = (String)d.get("org.apache.cxf.workqueue." + name + ".queueSize");
        if (s != null) {
            super.setQueueSize(Integer.parseInt(s));
        } 

    }

}
