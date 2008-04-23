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
package org.apache.cxf.aegis.inheritance.ws2.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.aegis.inheritance.ws2.WS2;
import org.apache.cxf.aegis.inheritance.ws2.common.ParentBean;
import org.apache.cxf.aegis.inheritance.ws2.common.exception.AlreadyExistsException;
import org.apache.cxf.aegis.inheritance.ws2.common.exception.NotFoundException;
import org.apache.cxf.aegis.inheritance.ws2.common.pack1.ContentBean1;
import org.apache.cxf.aegis.inheritance.ws2.common.pack2.ContentBean2;

/**
 * <br/>
 * 
 * @author xfournet
 */
public class WS2Impl implements WS2 {
    private Map<String, ParentBean> map = new HashMap<String, ParentBean>();

    public WS2Impl() {
        ParentBean x = new ParentBean("X", new ContentBean1("data1-X"));
        ParentBean y = new ParentBean("Y", new ContentBean2("data1-Y", "content2-Y"));
        map.put(x.getId(), x);
        map.put(y.getId(), y);
    }

    public synchronized void putParentBean(ParentBean parentBean) throws AlreadyExistsException {
        String id = parentBean.getId();
        if (map.containsKey(id)) {
            throw new AlreadyExistsException(id);
        }
        map.put(id, parentBean);
    }

    public synchronized ParentBean getParentBean(String id) throws NotFoundException {
        ParentBean result = map.get(id);
        if (result == null) {
            throw new NotFoundException(id);
        }

        return result;
    }
}
