/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.cxf.jaxrs.provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.junit.Test;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class GenericArgumentComparatorTest {

    /**
     * The type parameters of each relate and have a clear inheritance order
     */
    @Test
    public void simpleCase() {
        class Shape {
        }
        class Circle extends Shape {
        }
        abstract class ShapeReader implements MessageBodyReader<Shape> {
        }
        abstract class CircleReader implements MessageBodyReader<Circle> {
        }
        abstract class ObjectReader implements MessageBodyReader<Object> {
        }

        final List<Class<?>> classes = classes(ShapeReader.class, CircleReader.class, ObjectReader.class);

        assertOrder(MessageBodyReader.class, classes, "CircleReader\n"
                + "ShapeReader\n"
                + "ObjectReader");
    }

    /**
     * The parent implements the interface and the subclass defines the type
     */
    @Test
    public void subclassDefinedParameter() {
        class Shape {
        }
        class Circle extends Shape {
        }
        abstract class Resolver<T> implements ContextResolver<T> {
        }
        abstract class ShapeResolver extends Resolver<Shape> {
        }
        abstract class CircleResolver extends Resolver<Circle> {
        }
        abstract class ObjectResolver extends Resolver<Object> {
        }

        final List<Class<?>> classes = classes(ShapeResolver.class, CircleResolver.class, ObjectResolver.class);

        assertOrder(ContextResolver.class, classes, "CircleResolver\n"
                + "ShapeResolver\n"
                + "ObjectResolver");
    }


    /**
     * The type parameters of one hasn't actually been defined
     */
    @Test
    public void testUnspecifiedType() {
        class Shape {
        }
        class Circle extends Shape {
        }
        abstract class ShapeReader implements MessageBodyReader<Shape> {
        }
        abstract class CircleReader implements MessageBodyReader<Circle> {
        }
        abstract class UnknownReader<T> implements MessageBodyReader<T> {
        }

        final List<Class<?>> classes = classes(ShapeReader.class, CircleReader.class, UnknownReader.class);

        assertOrder(MessageBodyReader.class, classes, "CircleReader\n"
                + "ShapeReader\n"
                + "UnknownReader");
    }

    /**
     * The type parameters have a variable with bounds
     */
    @Test
    public void boundedTypeVariable() {
        class Shape {
        }
        class Circle extends Shape {
        }
        abstract class ShapeReader<T extends Shape> implements MessageBodyReader<T> {
        }
        abstract class CircleReader<T extends Circle> implements MessageBodyReader<T> {
        }
        abstract class ObjectReader implements MessageBodyReader<Object> {
        }

        final List<Class<?>> classes = classes(ShapeReader.class, CircleReader.class, ObjectReader.class);

        assertOrder(MessageBodyReader.class, classes, "CircleReader\n"
                + "ShapeReader\n"
                + "ObjectReader");
    }

    /**
     * The type parameters of each have no relationship
     */
    @Test
    public void noInheritance() {
        class Red {
        }
        class Green {
        }
        class Blue {
        }

        abstract class RedReader implements MessageBodyReader<Red> {
        }
        abstract class GreenReader implements MessageBodyReader<Green> {
        }
        abstract class BlueReader implements MessageBodyReader<Blue> {
        }

        final List<Class<?>> classListA = asList(RedReader.class, GreenReader.class, BlueReader.class);
        assertOrder(MessageBodyReader.class, classListA, "RedReader\n"
                + "GreenReader\n"
                + "BlueReader");

        final List<Class<?>> classListB = asList(GreenReader.class, RedReader.class, GreenReader.class, BlueReader.class);
        assertOrder(MessageBodyReader.class, classListB, "GreenReader\n"
                + "RedReader\n"
                + "GreenReader\n"
                + "BlueReader");
    }

    /**
     * One item is not a MessageBodyReader and should sort to the bottom
     */
    @Test
    public void oneTypeIsNotMessageBodyReader() {

        class Shape {
        }
        abstract class ShapeReader implements MessageBodyReader<Shape> {
        }
        abstract class CircleReader {
        }
        abstract class ObjectReader implements MessageBodyReader<Object> {
        }

        final List<Class<?>> classes = classes(ShapeReader.class, CircleReader.class, ObjectReader.class);

        assertOrder(MessageBodyReader.class, classes, "ShapeReader\n"
                + "ObjectReader\n"
                + "CircleReader");
    }

    /**
     * All items are equal in the first type parameter and can only be distinguished
     * by their nested parameter types
     */
    @Test
    public void nestedTypeArgument() throws Exception {
        class Shape {
        }
        class Circle extends Shape {
        }
        abstract class ShapeReader implements MessageBodyReader<Consumer<Shape>> {
        }
        abstract class CircleReader implements MessageBodyReader<Consumer<Circle>> {
        }
        abstract class ObjectReader implements MessageBodyReader<Consumer<Object>> {
        }

        final List<Class<?>> classes = classes(ShapeReader.class, CircleReader.class, ObjectReader.class);

        assertOrder(MessageBodyReader.class, classes, "CircleReader\n"
                + "ShapeReader\n"
                + "ObjectReader");
    }

    /**
     * All items are equal in the first type parameter and can only be distinguished
     * by their nested parameter types.  Also the type is defined by the subclasses.
     */
    @Test
    public void nestedTypeArgumentSubclass() throws Exception {
        class Shape {
        }
        class Circle extends Shape {
        }

        abstract class Writer<T> implements MessageBodyWriter<Consumer<T>> {
        }
        abstract class ShapeWriter extends Writer<Shape> {
        }
        abstract class CircleWriter extends Writer<Circle> {
        }
        abstract class ObjectWriter extends Writer<Object> {
        }

        final List<Class<?>> classes = classes(ShapeWriter.class, CircleWriter.class, ObjectWriter.class);

        assertOrder(MessageBodyWriter.class, classes, "CircleWriter\n"
                + "ShapeWriter\n"
                + "ObjectWriter");
    }

    /**
     * Don't just grab the first generics we see, they must map directly or
     * indirectly back to the interface we are interested in
     */
    @Test
    public void unrelatedGenericsAreIgnored() throws Exception {
        class Color {
        }
        class Red extends Color {
        }
        class Crimson extends Red {
        }

        class Shape {
        }
        class Circle extends Shape {
        }

        abstract class Writer<T> implements MessageBodyWriter<Consumer<T>> {
        }
        abstract class ShapeWriter<T> extends Writer<Shape>  implements ContextResolver<T> {
        }
        abstract class CircleWriter extends Writer<Circle> {
        }
        abstract class ObjectWriter extends Writer<Object> {
        }

        abstract class SpecialCircleWriter extends CircleWriter implements ContextResolver<Color> {
        }
        abstract class SpecialShapeWriter extends ShapeWriter<Red> {
        }
        abstract class SpecialObjectWriter extends ObjectWriter implements ContextResolver<Crimson> {
        }

        final List<Class<?>> classes = classes(SpecialObjectWriter.class, SpecialCircleWriter.class, SpecialShapeWriter.class);

        assertOrder(MessageBodyWriter.class, classes, "SpecialCircleWriter\n"
                + "SpecialShapeWriter\n"
                + "SpecialObjectWriter");
    }

    public static List<Class<?>> classes(final Class<?>... classes) {
        final List<Class<?>> list = new ArrayList<Class<?>>(asList(classes));
        Collections.shuffle(list);
        return list;
    }

    public static void assertOrder(final Class<?> genericInterface,
                                   final List<Class<?>> classes,
                                   final String expected) {
        final GenericArgumentComparator comparator = new GenericArgumentComparator(genericInterface);
        classes.sort(comparator);
        final String order = classes.stream()
                .map(Class::getSimpleName)
                .reduce((s, s2) -> s + "\n" + s2)
                .get();

        assertEquals(expected, order);
    }

}
