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
package demo.jaxrs.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.apache.cxf.jaxrs.ext.search.tika.TikaContentExtractor;
import org.apache.cxf.jaxrs.ext.search.tika.TikaContentExtractor.TikaContent;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkException;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.receiver.Receiver;

import scala.Tuple2;


@Path("/")
public class StreamingService {
    private static final Map<String, MediaType> MEDIA_TYPE_TABLE;
    static {
        MEDIA_TYPE_TABLE = new HashMap<String, MediaType>();
        MEDIA_TYPE_TABLE.put("pdf", MediaType.valueOf("application/pdf"));
        MEDIA_TYPE_TABLE.put("odt", MediaType.valueOf("application/vnd.oasis.opendocument.text"));
        MEDIA_TYPE_TABLE.put("odp", MediaType.valueOf("application/vnd.oasis.opendocument.presentation"));
    }
    private Executor executor = new ThreadPoolExecutor(5, 5, 0, TimeUnit.SECONDS,
                                                       new ArrayBlockingQueue<Runnable>(10));
    public StreamingService() {
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
        processStream(async, new StringListReceiver(getStringsFromString(tikaContent.getContent())));
    }
    
    @POST
    @Path("/stream")
    @Consumes("text/plain")
    @Produces("text/plain")
    public void processSimpleStream(@Suspended AsyncResponse async, InputStream is) {
        processStream(async, new StringListReceiver(getStringsFromInputStream(is)));
    }

    private void processStream(AsyncResponse async, Receiver<String> receiver) {
        try {
            SparkConf sparkConf = new SparkConf().setMaster("local[*]")
                .setAppName("JAX-RS Spark Connect " + getRandomId());
            JavaStreamingContext jssc = new JavaStreamingContext(sparkConf, Durations.seconds(1)); 
            
            SparkStreamingOutput streamOut = new SparkStreamingOutput(jssc);
            SparkStreamingListener sparkListener =  new SparkStreamingListener(streamOut);
            jssc.addStreamingListener(sparkListener);
            
            JavaReceiverInputDStream<String> receiverStream = jssc.receiverStream(receiver);
            JavaPairDStream<String, Integer> wordCounts = createOutputDStream(receiverStream);
            wordCounts.foreachRDD(new OutputFunction(streamOut));
            jssc.start();
                                                    
            executor.execute(new SparkJob(async, sparkListener));
        } catch (Exception ex) {
            // the compiler does not allow to catch SparkException directly
            if (ex instanceof SparkException) {
                async.cancel(60);
            } else {
                async.resume(new WebApplicationException(ex));
            }
        }
    }
    
    private static JavaPairDStream<String, Integer> createOutputDStream(
            JavaReceiverInputDStream<String> receiverStream) {
        final JavaDStream<String> words = 
            receiverStream.flatMap(x -> splitInputString(x));
            
        final JavaPairDStream<String, Integer> pairs = words.mapToPair(s -> {
                    return new Tuple2<String, Integer>(s, 1);
                });
        return pairs.reduceByKey((i1, i2) -> {
                    return i1 + i2;
                });
    }
    private static Iterator<String> splitInputString(String x) {
        List<String> list = new LinkedList<String>();
        for (String s : Arrays.asList(x.split(" "))) {
            s = s.trim();
            if (s.endsWith(":") || s.endsWith(",") || s.endsWith(";") || s.endsWith(".")) {
                s = s.substring(0, s.length() - 1);
            }
            if (!s.isEmpty()) {
                list.add(s);
            }
        }
        return list.iterator();
    }
    private static class OutputFunction implements VoidFunction<JavaPairRDD<String, Integer>> {
        private static final long serialVersionUID = 1L;
        private SparkStreamingOutput streamOut;
        OutputFunction(SparkStreamingOutput streamOut) {
            this.streamOut = streamOut;
        }
        @Override
        public void call(JavaPairRDD<String, Integer> rdd) {
            for (Map.Entry<String, Integer> entry : rdd.collectAsMap().entrySet()) {
                String value = entry.getKey() + " : " + entry.getValue() + "\n";
                streamOut.addResponseEntry(value);
            }
        }
        
    }
    private static String getRandomId() {
        byte[] bytes = new byte[10];
        new Random().nextBytes(bytes);
        return Base64Utility.encode(bytes);
    }
    private List<String> getStringsFromInputStream(InputStream is) {
        return getStringsFromReader(new BufferedReader(new InputStreamReader(is)));
    }
    private List<String> getStringsFromString(String s) {
        return getStringsFromReader(new BufferedReader(new StringReader(s)));
    }
    private List<String> getStringsFromReader(BufferedReader reader) {
        
        List<String> inputStrings = new LinkedList<String>();
        String userInput = null;
        try {
            while ((userInput = reader.readLine()) != null) {
                inputStrings.add(userInput);
            }
        } catch (IOException ex) {
            throw new WebApplicationException(ex);
        }
        return inputStrings;
    }
}
