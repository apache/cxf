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

package demo.jaxws.client;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cxf.common.spi.GeneratedClassClassLoaderCapture;
import org.apache.cxf.common.util.StringUtils;

public class DumpingClassLoaderCapturer implements GeneratedClassClassLoaderCapture {
    private final Map<String, byte[]> classes = new ConcurrentHashMap<>();
    
    public void dumpTo(File file) throws IOException {
        if (!file.exists()) 
            Files.createDirectories(file.toPath());{
        }
        
        if (!file.isDirectory()) {
            throw new IllegalArgumentException("The dump location does not exist or is not a directory: " + file);
        }
        
        for (Map.Entry<String, byte[]> entry: classes.entrySet()) {
            final Path path = file.toPath().resolve(StringUtils.periodToSlashes(entry.getKey()) + ".class");
            Files.createDirectories(path.getParent());
            
            try (OutputStream out = Files.newOutputStream(path, StandardOpenOption.CREATE)) {
                out.write(entry.getValue());
            }
        }
    }

    @Override
    public void capture(String className, byte[] bytes) {
        classes.putIfAbsent(className, bytes);
    }
}
