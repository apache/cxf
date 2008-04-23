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
package org.apache.cxf.aegis.inheritance.ws1;

import java.util.Arrays;

/**
 * <br/>
 * 
 * @author xfournet
 */
public class ResultBean {
    private BeanA[] result1;
    private RootBean[] result2;

    public BeanA[] getResult1() {
        return result1;
    }

    public void setResult1(BeanA[] result1) {
        this.result1 = result1;
    }

    public RootBean[] getResult2() {
        return result2;
    }

    public void setResult2(RootBean[] result2) {
        this.result2 = result2;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ResultBean that = (ResultBean)o;

        if (!Arrays.equals(result1, that.result1)) {
            return false;
        }
        if (!Arrays.equals(result2, that.result2)) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        return 0;
    }
}
