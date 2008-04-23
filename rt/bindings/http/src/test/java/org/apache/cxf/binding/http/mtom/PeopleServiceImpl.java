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
package org.apache.cxf.binding.http.mtom;

import java.util.ArrayList;
import java.util.List;

import javax.jws.WebService;

import org.apache.cxf.person.People;
import org.apache.cxf.person.Person;

@WebService(endpointInterface = "org.apache.cxf.binding.http.mtom.PeopleService")
public class PeopleServiceImpl implements PeopleService {
    List<Person> people = new ArrayList<Person>();

    public PeopleServiceImpl() {

    }

    public void addPerson(Person p) {
        people.add(p);
    }

    public People getPeople() {
        People p = new People();
        p.getPerson().addAll(people);
        return p;
    }

    public Person getPerson(String name) {
        // TODO Auto-generated method stub
        return null;
    }

}
