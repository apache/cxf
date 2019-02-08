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

package org.apache.cxf.systest.aegis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 *
 */
public class SportsServiceImpl implements SportsService {

    /** {@inheritDoc}*/
    public Collection<Team> getTeams() {
        List<Team> teams = new ArrayList<>();
        teams.add(new Team("Patriots", "New England"));
        return teams;
    }

    public String testForMinOccurs0(String a, Integer b, String c) {
        return a + b + c;
    }

    public AttributeBean getAttributeBean() {
        return new AttributeBean();
    }

    public BeanWithCharacter getCharBean() {
        return new BeanWithCharacter();
    }


    public Map<String, Map<Integer, Integer>> testComplexMapResult() {
        CustomerMap result
            = new CustomerMap();
        Map<Integer, Integer> map1 = new HashMap<>();
        map1.put(1, 3);
        result.put("key1", map1);
        return result;

    }


    public <T> T getGeneric(Collection<T> collection) {
        Iterator<T> iter = collection.iterator();

        T ret = null;
        if (iter.hasNext()) {
            ret = iter.next();
        }
        return ret;
    }


    public <T1, T2> Pair<T1, T2> getReturnGenericPair(T1 first, T2 second) {
        return new Pair<T1, T2>(first, second);
    }


    public Pair<Integer, String> getReturnQualifiedPair(Integer first, String second) {
        return new Pair<Integer, String>(first, second);
    }


    public <T1, T2> int getGenericPair(Pair<T1, T2> pair) {
        return (Integer) pair.getFirst();
    }


    public int getQualifiedPair(Pair<Integer, String> pair) {
        return pair.getFirst();
    }

}
