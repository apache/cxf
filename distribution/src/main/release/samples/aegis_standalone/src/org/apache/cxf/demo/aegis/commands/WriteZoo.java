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

import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;


import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.AegisWriter;
import org.apache.cxf.aegis.type.Type;
import org.apache.cxf.demo.aegis.types.Animal;
import org.apache.cxf.demo.aegis.types.Zoo;

import javanet.staxutils.IndentingXMLStreamWriter;

/**
 * 
 */
public final class WriteZoo {
    private XMLOutputFactory outputFactory;
    private String outputPathname;

    private WriteZoo() {
        outputFactory = XMLOutputFactory.newInstance();
    }

    private void go() throws Exception {
        AegisContext context;

        context = new AegisContext();
        context.setWriteXsiTypes(true);
        Set<Class<?>> rootClasses = new HashSet<Class<?>>();
        rootClasses.add(Zoo.class);
        context.setRootClasses(rootClasses);
        context.initialize();
        AegisWriter<XMLStreamWriter> writer = context.createXMLStreamWriter();
        FileOutputStream output = new FileOutputStream(outputPathname);
        XMLStreamWriter xmlWriter = outputFactory.createXMLStreamWriter(output);
        IndentingXMLStreamWriter indentWriter = new IndentingXMLStreamWriter(xmlWriter);

        Zoo zoo = populateZoo();
        Type aegisType = context.getTypeMapping().getType(zoo.getClass());
        writer.write(zoo, new QName("urn:aegis:demo", "zoo"), false, indentWriter, aegisType);
        xmlWriter.close();
        output.close();
    }

    private Zoo populateZoo() {
        Zoo zoo = new Zoo();
        zoo.setFounder("Noah");
        zoo.setName("The Original Zoo");
        Map<String, Animal> animals = new HashMap<String, Animal>();
        Animal a = new Animal();
        a.setName("lion");
        animals.put("lion", a);
        a = new Animal();
        a.setName("tiger");
        animals.put("tiger", a);
        a = new Animal();
        a.setName("bear");
        animals.put("bear", a);
        zoo.setAnimals(animals);
        return zoo;
    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        WriteZoo wz = new WriteZoo();
        wz.outputPathname = args[0];
        wz.go();
    }

}
