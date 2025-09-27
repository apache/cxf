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

package org.apache.cxf.jaxrs.ext.search.hibernate;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.usertype.UserType;

/**
 * @author Steve Ebersole
 */
class StaticUserTypeSupport<T> implements UserType<T> {
    private final BasicJavaType<T> javaType;
    private final JdbcType jdbcType;
    private final BasicValueConverter<T, Object> valueConverter;

    private final ValueExtractor<Object> jdbcValueExtractor;
    private final ValueBinder<Object> jdbcValueBinder;

    StaticUserTypeSupport(BasicJavaType<T> javaType, JdbcType jdbcType) {
        this(javaType, jdbcType, javaType.getMutabilityPlan());
    }

    StaticUserTypeSupport(BasicJavaType<T> javaType, JdbcType jdbcType, MutabilityPlan<T> mutabilityPlan) {
        this(javaType, jdbcType, mutabilityPlan, null);
    }

    StaticUserTypeSupport(BasicJavaType<T> javaType, JdbcType jdbcType, BasicValueConverter<T, Object> valueConverter) {
        this(javaType, jdbcType, javaType.getMutabilityPlan(), valueConverter);
    }

    StaticUserTypeSupport(BasicJavaType<T> javaType, JdbcType jdbcType, MutabilityPlan<T> mutabilityPlan,
            BasicValueConverter<T, Object> valueConverter) {
        this.javaType = javaType;
        this.jdbcType = jdbcType;
        this.valueConverter = valueConverter;

        //noinspection unchecked
        this.jdbcValueExtractor = jdbcType.getExtractor((JavaType<Object>) javaType);
        //noinspection unchecked
        this.jdbcValueBinder = jdbcType.getBinder((JavaType<Object>) javaType);
    }

    @Override
    public int getSqlType() {
        return jdbcType.getDdlTypeCode();
    }

    @Override
    public Class<T> returnedClass() {
        return javaType.getJavaTypeClass();
    }

    @Override
    public boolean equals(T x, T y) throws HibernateException {
        return javaType.areEqual(x, y);
    }

    @Override
    public int hashCode(T x) throws HibernateException {
        return javaType.extractHashCode(x);
    }

    @Override
    public T nullSafeGet(ResultSet rs, int position, WrapperOptions options) throws SQLException {
        final Object extracted = jdbcValueExtractor.extract(rs, position, options);

        if (valueConverter != null) {
            return valueConverter.toDomainValue(extracted);
        }

        //noinspection unchecked
        return (T) extracted;
    }

    @Override
    public void nullSafeSet(PreparedStatement st, T value, int index, WrapperOptions options) throws SQLException {
        final Object valueToBind;
        if (valueConverter != null) {
            valueToBind = valueConverter.toRelationalValue(value);
        } else {
            valueToBind = value;
        }

        jdbcValueBinder.bind(st, valueToBind, index, options);
    }

    @Override
    public T deepCopy(T value) throws HibernateException {
        return javaType.getMutabilityPlan().deepCopy(value);
    }

    @Override
    public boolean isMutable() {
        return javaType.getMutabilityPlan().isMutable();
    }

    @Override
    public Serializable disassemble(T value) throws HibernateException {
        return javaType.getMutabilityPlan().disassemble(value, null);
    }

    @Override
    public T assemble(Serializable cached, Object owner) throws HibernateException {
        return javaType.getMutabilityPlan().assemble(cached, null);
    }
}
