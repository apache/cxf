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

package org.apache.cxf.jca.cxf;

import java.util.logging.Logger;

import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkListener;

import org.apache.cxf.common.logging.LogUtils;

/**
 * 
 */
public class CXFWorkAdapter implements WorkListener {
    
    public static final long DEFAULT_START_TIME_OUT = 1 * 60 * 1000; // 1 minute

    private static final Logger LOG = LogUtils.getL7dLogger(CXFWorkAdapter.class);
    
    public void workAccepted(WorkEvent e) {
        LOG.fine("workAccepted: [" + e.getWork() + "], source is [" + e.getSource() + "]");
    }

    
    public void workCompleted(WorkEvent e) {
        LOG.fine("workCompleted: [" + e.getWork() + "], source is [" + e.getSource() + "]");
    }

    
    public void workRejected(WorkEvent e) {
        LOG.severe("workRejected: [" + e.getWork() + "], source is [" + e.getSource() + "]");
        LOG.severe("root cause is:" + e.getException().getMessage());
        
        e.getException().printStackTrace();
    }

    
    public void workStarted(WorkEvent e) {
        LOG.fine("workStarted: [" + e.getWork() + "], source is [" + e.getSource() + "]");
    }

}
