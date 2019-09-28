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
package demo.jaxrs.server.socket;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.ext.Oneway;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.apache.cxf.jaxrs.ext.search.tika.TikaContentExtractor;
import org.apache.cxf.jaxrs.ext.search.tika.TikaContentExtractor.TikaContent;

import demo.jaxrs.server.SparkUtils;


@Path("/")
public class StreamingService {
    private static final Map<String, MediaType> MEDIA_TYPE_TABLE;
    static {
        MEDIA_TYPE_TABLE = new HashMap<>();
        MEDIA_TYPE_TABLE.put("pdf", MediaType.valueOf("application/pdf"));
        MEDIA_TYPE_TABLE.put("odt", MediaType.valueOf("application/vnd.oasis.opendocument.text"));
        MEDIA_TYPE_TABLE.put("odp", MediaType.valueOf("application/vnd.oasis.opendocument.presentation"));
    }
    private Executor executor = new ThreadPoolExecutor(5, 5, 0, TimeUnit.SECONDS,
                                                       new ArrayBlockingQueue<Runnable>(10));
    private Map<String, BlockingQueue<String>> sparkResponses = new ConcurrentHashMap<>();
    private PrintStream sparkOutputStream;

    public StreamingService(BufferedReader sparkInputStream, PrintStream sparkOutputStream) {
        this.sparkOutputStream = sparkOutputStream;
        executor.execute(new SparkResultJob(sparkResponses, sparkInputStream));
    }

    @POST
    @Path("/multipart")
    @Consumes("multipart/form-data")
    @Produces("text/plain")
    public void processMultipartStream(@Suspended AsyncResponse async,
                                       @Multipart("file") Attachment att) {
        MediaType mediaType = att.getContentType();
        if (mediaType == null) {
            String fileName = att.getContentDisposition().getFilename();
            if (fileName != null) {
                int extDot = fileName.lastIndexOf('.');
                if (extDot > 0) {
                    mediaType = MEDIA_TYPE_TABLE.get(fileName.substring(extDot + 1));
                }
            }
        }

        TikaContentExtractor tika = new TikaContentExtractor();
        TikaContent tikaContent = tika.extract(att.getObject(InputStream.class),
                                               mediaType);
        processStream(async, SparkUtils.getStringsFromString(tikaContent.getContent()));
    }

    @POST
    @Path("/stream")
    @Consumes("text/plain")
    @Produces("text/plain")
    public void processSimpleStream(@Suspended AsyncResponse async, InputStream is) {
        processStream(async, SparkUtils.getStringsFromInputStream(is));
    }

    private void processStream(AsyncResponse async, List<String> inputStrings) {
        executor.execute(
            new SparkJob(async, sparkResponses, sparkOutputStream, inputStrings));
    }

    @POST
    @Path("/streamOneWay")
    @Consumes("text/plain")
    @Oneway
    public void processSimpleStreamOneWay(InputStream is) {
        for (String s : SparkUtils.getStringsFromInputStream(is)) {
            sparkOutputStream.println("oneway:" + s);
        }
    }
}
