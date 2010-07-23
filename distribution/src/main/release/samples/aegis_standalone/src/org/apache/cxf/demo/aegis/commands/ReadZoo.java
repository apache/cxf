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

package org.apache.cxf.demo.aegis.commands;

import java.io.FileInputStream;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.AegisReader;
import org.apache.cxf.demo.aegis.types.Animal;
import org.apache.cxf.demo.aegis.types.Zoo;

/**
 * 
 */
public final class ReadZoo {

    private XMLInputFactory inputFactory;
    private String inputPathname;

    private ReadZoo() {
        inputFactory = XMLInputFactory.newInstance();
    }

    private void go() throws Exception {
        AegisContext context;

        context = new AegisContext();
        Set<Type> rootClasses = new HashSet<Type>();
        rootClasses.add(Zoo.class);
        context.setRootClasses(rootClasses);
        context.initialize();
        AegisReader<XMLStreamReader> reader = context.createXMLStreamReader();
        FileInputStream input = new FileInputStream(inputPathname);
        XMLStreamReader xmlReader = inputFactory.createXMLStreamReader(input);
        Zoo zoo = (Zoo)reader.read(xmlReader);
        System.out.println("Name " + zoo.getName());
        System.out.println("Founder " + zoo.getFounder());
        for (Map.Entry<String, Animal> e : zoo.getAnimals().entrySet()) {
            System.out.println(e.getKey() + " -> " + e.getValue().getName());
        }
    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        ReadZoo rz = new ReadZoo();
        rz.inputPathname = args[0];
        rz.go();
    }
}
