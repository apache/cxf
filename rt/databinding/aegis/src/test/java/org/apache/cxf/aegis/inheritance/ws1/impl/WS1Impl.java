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
package org.apache.cxf.aegis.inheritance.ws1.impl;

import java.util.Map;

import org.apache.cxf.aegis.inheritance.ws1.BeanA;
import org.apache.cxf.aegis.inheritance.ws1.BeanB;
import org.apache.cxf.aegis.inheritance.ws1.BeanC;
import org.apache.cxf.aegis.inheritance.ws1.ResultBean;
import org.apache.cxf.aegis.inheritance.ws1.RootBean;
import org.apache.cxf.aegis.inheritance.ws1.WS1;
import org.apache.cxf.aegis.inheritance.ws1.WS1Exception;
import org.apache.cxf.aegis.inheritance.ws1.WS1ExtendedException;
import org.apache.cxf.aegis.services.SimpleBean;

/**
 * <br/>
 * 
 * @author xfournet
 */
public class WS1Impl implements WS1 {
    public BeanA getBeanA() {
        BeanA a = new BeanA();
        a.setPropA("valueA");
        return a;
    }

    public BeanB getBeanB() {
        BeanB b = new BeanB();
        b.setPropA("valueA");
        b.setPropB("valueB");
        return b;
    }

    // not exported to interface to "hide" BeanC from interface introspection
    public BeanC getBeanC() {
        BeanC c = new BeanC();
        c.setPropA("valueA");
        c.setPropB("valueB");
        c.setPropC("valueC");
        return c;
    }

    public BeanA getBean(String id) {
        if ("b".equalsIgnoreCase(id)) {
            return getBeanB();
        } else if ("c".equalsIgnoreCase(id)) {
            return getBeanC();
        } else if ("a".equalsIgnoreCase(id)) {
            return getBeanA();
        } else {
            return null;
        }
    }

    public BeanA[] listBeans() {
        BeanA[] result = new BeanA[4];

        result[0] = getBean("b");
        result[1] = null;
        result[2] = getBean("a");
        result[3] = getBean("c");

        return result;
    }

    public RootBean getRootBean(String id) {
        RootBean rootBean = new RootBean();
        rootBean.setId(id);
        rootBean.setChild(getBean(id));

        return rootBean;
    }

    public RootBean[] listRootBeans() {
        RootBean[] result = new RootBean[4];

        result[0] = getRootBean("b");
        result[1] = null;
        result[2] = getRootBean("a");
        result[3] = getRootBean("c");

        return result;
    }

    public ResultBean getResultBean() {
        ResultBean resultBean = new ResultBean();
        resultBean.setResult1(listBeans());
        resultBean.setResult2(listRootBeans());

        return resultBean;
    }

    public Map echoMap(Map beans) {
        return beans;
    }

    public void throwException(boolean extendedOne) throws WS1Exception {
        if (extendedOne) {
            throw new WS1ExtendedException("WS1 extended exception",
                                             20, 30,
                                             new SimpleBean());
        } else {
            throw new WS1Exception("WS1 base exception", 10);
        }
    }

    public Map echoRawMap(Map rawMap) {
        return rawMap;
    }
}
