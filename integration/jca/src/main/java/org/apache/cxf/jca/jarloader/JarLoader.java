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
package org.apache.cxf.jca.jarloader;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.cxf.helpers.CastUtils;


/**
 * load jars to memory from an archive
 */
public final class JarLoader {
    // map of all the archives we now about
    // this keys to this map are full distinguished names of the fromat
    // someOuterArchive!/someInnerArchive!/....
    // the values of the archives map are maps of entry names to bytes
    static Map<String, Object> archives = new HashMap<String, Object>();
    static final int CHUNK_SIZE = 4096;
    static final int MAX_CHUNK_SIZE = CHUNK_SIZE * 16;

    private JarLoader() {
    }

    public static byte[] getBytesFromInputStream(InputStream is) throws IOException {
        return getBytesFromInputStream(is, -1);
    }

    public static synchronized Map getJarContents(String path) throws MalformedURLException, IOException {
        if (!archives.containsKey(path)) {
            loadArchive(path);
        }

        return (Map)archives.get(path);
    }

    private static void loadArchive(String path) throws MalformedURLException, IOException {
        List<String> nameComponents = tokenizePathComponents(path);

        for (int i = 0; i < nameComponents.size(); i++) {
            if (!archives.containsKey(buildPartialName(nameComponents, i + 1))) {
                readArchive(buildPartialName(nameComponents, i + 1));

                if (i != 0) {
                    // not the root archive so there is a parentMap with a reference to the
                    // entry as raw byte array.
                    // This byte array has now been exploded into a Map so the raw bytes are
                    // no longer needed, replace the entry with the exploded Map
                    //
                    Map<String, Object> parentMap = 
                        CastUtils.cast((Map)archives.get(buildPartialName(nameComponents, i)));
                    Map archiveMap = 
                        (Map)archives.get(buildPartialName(nameComponents, i + 1));

                    parentMap.put(nameComponents.get(i), archiveMap);
                }
            }
        }
    }

    private static List<String> tokenizePathComponents(String path) {
        List<String> tokens = new LinkedList<String>();
        String tmpPath = new String(path);

        while (tmpPath.length() > 0) {
            if (tmpPath.indexOf("!/") == -1) {
                tokens.add(tmpPath);
                tmpPath = "";
            } else {
                tokens.add(tmpPath.substring(0, tmpPath.indexOf("!/")));
                tmpPath = tmpPath.substring(tmpPath.indexOf("!/") + 2);
            }
        }

        return tokens;
    }

    private static String buildPartialName(List<String> nameComponents, int size) {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < size; i++) {
            sb.append(nameComponents.get(i));
            sb.append("!/");
        }

        return sb.toString();
    }

    private static String getRootArchiveName(String name) {
        int index = name.indexOf("file:");

        if (index > -1) {
            name = name.substring(index);
        }

        if (name.indexOf("!/") != -1) {
            return name.substring(0, name.indexOf("!/"));
        } else {
            return name;
        }
    }

    private static void readArchive(String name) throws MalformedURLException, IOException {
        List<String> nameComponents = tokenizePathComponents(name);
        Map<String, Object> map = null;

        if (nameComponents.size() == 1) {            
            map = readZipStream((new URL(getRootArchiveName(name))).openStream());
        } else {
            Map parentMap = (Map)archives.get(buildPartialName(nameComponents, nameComponents.size() - 1));
            byte bytes[] = (byte[])(parentMap.get(nameComponents.get(nameComponents.size() - 1)));
            
            if (null == bytes) {
                // unexpected, classpath entry in error, referenced jar is not in the archive
                throw new IOException(
                        "Enclosing archive " + buildPartialName(nameComponents, nameComponents.size() - 1)
                        + " has no entry named:" + nameComponents.get(nameComponents.size() - 1)
                        + ", error in archive classpath");
            }

            map = readZipStream(new ByteArrayInputStream(bytes));
        }

        archives.put(name, map);
    }

    private static Map<String, Object> readZipStream(InputStream is) throws IOException {
        ZipInputStream zis = new ZipInputStream(is);
        Map<String, Object> map = new HashMap<String, Object>();

        for (ZipEntry ze = zis.getNextEntry(); ze != null; ze = zis.getNextEntry()) {
            if (ze.isDirectory()) {
                map.put(ze.getName(), ze.getName());               
            } else {                
                byte bytes[] = getBytesFromInputStream(zis, ze.getSize());
                map.put(ze.getName(), bytes);
            }
        }

        return map;
    }

    private static byte[] getBytesFromInputStream(InputStream is, long size) throws IOException {
               
        byte chunk[] = new byte[((size > CHUNK_SIZE) && (size < MAX_CHUNK_SIZE)) ? (int)size : CHUNK_SIZE];
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        for (int i = is.read(chunk, 0, chunk.length); i != -1; i = is.read(chunk, 0, chunk.length)) {
            baos.write(chunk, 0, i);           
        }
         
        return baos.toByteArray();
    }
}
