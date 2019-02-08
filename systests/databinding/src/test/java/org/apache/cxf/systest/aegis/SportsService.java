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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.jws.WebService;

@WebService(targetNamespace = "http://cxf.apache.org/systest/aegis/sports")
public interface SportsService {
    Collection<Team> getTeams();

    String testForMinOccurs0(String a, Integer b, String c);

    AttributeBean getAttributeBean();

    BeanWithCharacter getCharBean();

    class CustomerMap extends HashMap<String, Map<Integer, Integer>> {
        private static final long serialVersionUID = 6235169270166551322L;
    }

    class Pair<T1, T2> {
        private T1 first;
        private T2 second;

        public Pair() {

        }

        public Pair(T1 first, T2 second) {
            this.first = first;
            this.second = second;
        }

        public T1 getFirst() {
            return first;
        }

        public T2 getSecond() {
            return second;
        }

        public void setFirst(T1 first) {
            this.first = first;
        }

        public void setSecond(T2 second) {
            this.second = second;
        }

        public String toString() {
            return "first: " + getFirst() + " second: " + getSecond();
        }
    }


    class SimpleMapResult extends HashMap<String, Integer> {

        private static final long serialVersionUID = -5599483363035948690L;
    }

    Map<String, Map<Integer, Integer>> testComplexMapResult();

    <T> T getGeneric(Collection<T> collection);

    <T1, T2> Pair<T1, T2> getReturnGenericPair(T1 first, T2 second);

    Pair<Integer, String> getReturnQualifiedPair(Integer first, String second);

    <T1, T2> int getGenericPair(Pair<T1, T2> pair);

    int getQualifiedPair(Pair<Integer, String> pair);
}
