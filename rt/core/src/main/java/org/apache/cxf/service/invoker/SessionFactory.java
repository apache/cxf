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

package org.apache.cxf.service.invoker;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.service.Service;

/**
 * Creates a new instance for each session.
 * 
 * This may have restrictions on what the bean can look like.   For example, 
 * some session implementations require the beans to be Serializable
 */
public class SessionFactory implements Factory {
    Factory factory;
    public SessionFactory(Class<?> svcClass) {
        this(new PerRequestFactory(svcClass));
    }
    public SessionFactory(Factory f) {
        factory = f;
    }

    /** {@inheritDoc}*/
    public Object create(Exchange e) throws Throwable {
        Service serv = e.get(Service.class);
        Object o = null;
        synchronized (serv) {
            o = e.getSession().get(serv.getName());
            if (o == null) {
                o = factory.create(e);
                e.getSession().put(serv.getName(), o);
            }
        }
        return o;
    }

    /** {@inheritDoc}*/
    public void release(Exchange e, Object o) {
        //nothing
    }

}
