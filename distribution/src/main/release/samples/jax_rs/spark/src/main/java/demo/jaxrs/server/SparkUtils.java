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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import javax.ws.rs.WebApplicationException;

import org.apache.cxf.common.util.Base64Utility;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;

import scala.Tuple2;


public final class SparkUtils {

    private SparkUtils() {
    }

    public static JavaPairDStream<String, Integer> createOutputDStream(
        JavaDStream<String> receiverStream, boolean withId) {
        final JavaDStream<String> words =
            receiverStream.flatMap(x -> withId ? splitInputStringWithId(x) : splitInputString(x));

        final JavaPairDStream<String, Integer> pairs = words.mapToPair(s -> {
            return new Tuple2<String, Integer>(s, 1);
        });
        return pairs.reduceByKey((i1, i2) -> {
            return i1 + i2;
        });
    }
    public static Iterator<String> splitInputString(String x) {
        List<String> list = new LinkedList<>();
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
    public static Iterator<String> splitInputStringWithId(String x) {
        int index = x.indexOf(':');
        String jobId = x.substring(0, index);
        x = x.substring(index + 1);

        List<String> list = new LinkedList<>();
        for (String s : Arrays.asList(x.split(" "))) {
            s = s.trim();
            if (s.endsWith(":") || s.endsWith(",") || s.endsWith(";") || s.endsWith(".")) {
                s = s.substring(0, s.length() - 1);
            }
            if (!s.isEmpty()) {
                list.add(jobId + ":" + s);
            }
        }
        return list.iterator();
    }
    public static String getRandomId() {
        byte[] bytes = new byte[10];
        new Random().nextBytes(bytes);
        return Base64Utility.encode(bytes);
    }
    public static List<String> getStringsFromInputStream(InputStream is) {
        return getStringsFromReader(new BufferedReader(new InputStreamReader(is)));
    }
    public static List<String> getStringsFromString(String s) {
        return getStringsFromReader(new BufferedReader(new StringReader(s)));
    }
    public static List<String> getStringsFromReader(BufferedReader reader) {

        List<String> inputStrings = new LinkedList<>();
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
