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
package org.apache.cxf.jaxrs.ext.logging.atom;

import java.lang.reflect.Constructor;

import org.apache.cxf.jaxrs.ext.logging.atom.converter.Converter;
import org.apache.cxf.jaxrs.ext.logging.atom.converter.StandardConverter;
import org.apache.cxf.jaxrs.ext.logging.atom.converter.StandardConverter.Format;
import org.apache.cxf.jaxrs.ext.logging.atom.converter.StandardConverter.Multiplicity;
import org.apache.cxf.jaxrs.ext.logging.atom.converter.StandardConverter.Output;
import org.apache.cxf.jaxrs.ext.logging.atom.deliverer.Deliverer;
import org.apache.cxf.jaxrs.ext.logging.atom.deliverer.RetryingDeliverer;
import org.apache.cxf.jaxrs.ext.logging.atom.deliverer.WebClientDeliverer;

/**
 * Package private interpreter of incomplete input of engine configuration. Used commonly by
 * {@link AtomPushHandler properties file} and {@link AtomPushBean spring} configuration schemes.
 */
// TODO extract 'general rules' of interpretation in handler and bean and put here
final class AtomPushEngineConfigurator {

    private Deliverer deliverer;
    private Converter converter;
    private String delivererClass;
    private String converterClass;
    private String batchSize;
    private String delivererUrl;
    private String retryTimeout;
    private String retryPause;
    private String retryPauseTime;
    private String output;
    private String multiplicity;
    private String format;

    public void setUrl(String url) {
        this.delivererUrl = url;
    }

    public void setRetryTimeout(String retryTimeout) {
        this.retryTimeout = retryTimeout;
    }

    public void setRetryPause(String retryPause) {
        this.retryPause = retryPause;
    }

    public void setRetryPauseTime(String retryPauseTime) {
        this.retryPauseTime = retryPauseTime;
    }

    public void setBatchSize(String batchSize) {
        this.batchSize = batchSize;
    }

    public void setDeliverer(Deliverer deliverer) {
        this.deliverer = deliverer;
    }

    public void setConverter(Converter converter) {
        this.converter = converter;
    }

    public void setDelivererClass(String delivererClass) {
        this.delivererClass = delivererClass;
    }

    public void setConverterClass(String converterClass) {
        this.converterClass = converterClass;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public void setMultiplicity(String multiplicity) {
        this.multiplicity = multiplicity;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public AtomPushEngine createEngine() {
        Deliverer d = deliverer;
        Converter c = converter;
        if (d == null) {
            if (delivererUrl != null) {
                if (delivererClass != null) {
                    d = createDeliverer(delivererClass, delivererUrl);
                } else {
                    d = new WebClientDeliverer(delivererUrl);
                }
            } else {
                throw new IllegalStateException("Either url, deliverer or "
                                                + "deliverer class with url must be setup");
            }
        }
        if (c == null) {
            if (converterClass != null) {
                c = createConverter(converterClass);
            } else {
                Output out = parseEnum(output, Output.FEED);
                Multiplicity mul = parseEnum(multiplicity, Multiplicity.ONE);
                Format form = parseEnum(format, Format.CONTENT);
                c = new StandardConverter(out, mul, form);
            }
        }
        int batch = parseInt(batchSize, 1, 1);
        if (retryPause != null) {
            int timeout = parseInt(retryTimeout, 0, 0);
            int pause = parseInt(retryPauseTime, 1, 30);
            boolean linear = !retryPause.equalsIgnoreCase("exponential");
            d = new RetryingDeliverer(d, timeout, pause, linear);
        }
        AtomPushEngine engine = new AtomPushEngine();
        engine.setDeliverer(d);
        engine.setConverter(c);
        engine.setBatchSize(batch);
        return engine;
    }

    private Deliverer createDeliverer(String clazz, String url) {
        try {
            Constructor<Deliverer> ctor = loadClass(clazz, Deliverer.class).getConstructor(String.class);
            return ctor.newInstance(url);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private Converter createConverter(String clazz) {
        try {
            Constructor<Converter> ctor = loadClass(clazz, Converter.class).getConstructor();
            return ctor.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Class<T> loadClass(String clazz, Class<T> ifaceClass) throws ClassNotFoundException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            return (Class<T>)cl.loadClass(clazz);
        } catch (ClassNotFoundException e) {
            try {
                // clazz could be shorted (stripped package name) retry for interface location
                String pkg = ifaceClass.getPackage().getName();
                String clazz2 = pkg + "." + clazz;
                return (Class<T>)cl.loadClass(clazz2);
            } catch (Exception e1) {
                throw new ClassNotFoundException(e.getMessage() + " or " + e1.getMessage());
            }
        }
    }

    private int parseInt(String property, int defaultValue) {
        try {
            return Integer.parseInt(property);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private int parseInt(String property, int lessThan, int defaultValue) {
        int ret = parseInt(property, defaultValue);
        if (ret < lessThan) {
            ret = defaultValue;
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    private <T extends Enum<T>> T parseEnum(String value, T defaultValue) {
        if (value == null | "".equals(value)) {
            return defaultValue;
        }
        try {
            return (T)Enum.valueOf(defaultValue.getClass(), value.toUpperCase());
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
