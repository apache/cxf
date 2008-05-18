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
package org.apache.cxf.aegis.type.java5.map;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class StudentServiceImpl implements StudentService {
    
    private Map<Long, Student> studentMap;
    
    public StudentServiceImpl() {
        studentMap = new HashMap<Long, Student>();
        studentMap.put(Long.valueOf(1), new Student("Student1", 1));
        studentMap.put(Long.valueOf(100), new Student("Student100", 100));
        studentMap.put(Long.valueOf(-1), new Student("StudentNegative", -1));
    }
    
    public Student findStudent(Long id) {
        return studentMap.get(id);
    }

    public List<Student> getStudents(Map<String, String> filters) {
        List<Student> returnValue = new LinkedList<Student>();
        for (Map.Entry<Long, Student> e : studentMap.entrySet()) {
            if (filters.containsKey(e.getValue())) {
                returnValue.add(e.getValue());
            }
            
        }
        return returnValue;
    }

    public List<Student> getStudentsByIds(List<String> ids) {
        List<Student> returnValue = new LinkedList<Student>();
        for (String id : ids) {
            Long longId = Long.decode(id);
            Student s = studentMap.get(longId);
            returnValue.add(s);
        }
        return returnValue;
    }

    public Map<Long, Student> getStudentsMap() {
        return studentMap;
    }

    public Map<String, ?> getWildcardMap() {
        Map<String, String> m = new HashMap<String, String>();
        m.put("keystring", "valuestring");
        return m;
    }

}
