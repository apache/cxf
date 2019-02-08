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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.workqueue.AutomaticWorkQueueImpl;
import org.apache.cxf.workqueue.WorkQueueManager;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.util.tracker.ServiceTracker;

/**
 * List of work queues that can be managed using the OSGi configuration admin service
 */
public class ManagedWorkQueueList implements ManagedServiceFactory, PropertyChangeListener {
    public static final String FACTORY_PID = "org.apache.cxf.workqueues";
    private static final Logger LOG = LogUtils.getL7dLogger(ManagedWorkQueueList.class);

    private Map<String, AutomaticWorkQueueImpl> queues =
        new ConcurrentHashMap<>(4, 0.75f, 2);
    private ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> configAdminTracker;

    public String getName() {
        return FACTORY_PID;
    }

    public void updated(String pid, Dictionary<String, ?> props)
        throws ConfigurationException {
        if (pid == null) {
            return;
        }
        Dictionary<String, String> properties = CastUtils.cast(props);
        String queueName = properties.get(AutomaticWorkQueueImpl.PROPERTY_NAME);
        if (queues.containsKey(queueName)) {
            queues.get(queueName).update(properties);
        } else {
            AutomaticWorkQueueImpl wq = new AutomaticWorkQueueImpl(queueName);
            wq.setShared(true);
            wq.update(properties);
            wq.addChangeListener(this);
            queues.put(pid, wq);
        }
    }

    public void deleted(String pid) {
        queues.remove(pid);
    }

    /*
     * On property changes of queue settings we update the config admin service pid of the queue
     */
    public void propertyChange(PropertyChangeEvent evt) {
        try {
            AutomaticWorkQueueImpl queue = (AutomaticWorkQueueImpl)evt.getSource();
            ConfigurationAdmin configurationAdmin = configAdminTracker.getService();
            if (configurationAdmin != null) {
                Configuration selectedConfig = findConfigForQueueName(queue, configurationAdmin);
                if (selectedConfig != null) {
                    Dictionary<String, String> properties = queue.getProperties();
                    selectedConfig.update(properties);
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, e.getMessage(), e);
        }
    }

    private Configuration findConfigForQueueName(AutomaticWorkQueueImpl queue,
                                                 ConfigurationAdmin configurationAdmin) throws Exception {
        Configuration selectedConfig = null;
        String filter = "(service.factoryPid=" + ManagedWorkQueueList.FACTORY_PID + ")";
        Configuration[] configs = configurationAdmin.listConfigurations(filter);
        for (Configuration configuration : configs) {
            Dictionary<String, Object> props = configuration.getProperties();
            String name = (String)props.get(AutomaticWorkQueueImpl.PROPERTY_NAME);
            if (queue.getName().equals(name)) {
                selectedConfig = configuration;
            }
        }
        return selectedConfig;
    }

    public void addAllToWorkQueueManager(WorkQueueManager manager) {
        if (manager != null) {
            for (AutomaticWorkQueueImpl wq : queues.values()) {
                if (manager.getNamedWorkQueue(wq.getName()) == null) {
                    manager.addNamedWorkQueue(wq.getName(), wq);
                }
            }
        }
    }

    public void setConfigAdminTracker(ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> configAdminTracker) {
        this.configAdminTracker = configAdminTracker;
    }

    public void shutDown() {
        for (AutomaticWorkQueueImpl wq : queues.values()) {
            wq.setShared(false);
            wq.shutdown(true);
        }
        queues.clear();
    }
}