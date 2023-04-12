package org.apache.cxf.jaxrs.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.apache.cxf.jaxrs.provider.ProviderFactory;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.EntityPart.Builder;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

public class EntityPartBuilderImpl implements EntityPart.Builder {

	private final String name;
	private MediaType mediaType;
	private MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
	private String fileName;
	private InputStream content;

	public EntityPartBuilderImpl(String name) {
		this.name = name;
	}

	@Override
	public Builder mediaType(MediaType mediaType) throws IllegalArgumentException {
		this.mediaType = mediaType;
		return this;
	}

	@Override
	public Builder mediaType(String mediaTypeString) throws IllegalArgumentException {
		this.mediaType = MediaType.valueOf(mediaTypeString);
		return this;
	}

	@Override
	public Builder header(String headerName, String... headerValues) throws IllegalArgumentException {
		headers.addAll(headerName, headerValues);
		return this;
	}

	@Override
	public Builder headers(MultivaluedMap<String, String> newHeaders) throws IllegalArgumentException {
		headers.clear();
		headers.putAll(newHeaders);
		return this;
	}

	@Override
	public Builder fileName(String fileName) throws IllegalArgumentException {
		this.fileName = fileName;
		return this;
	}

	@Override
	public Builder content(InputStream content) throws IllegalArgumentException {
		this.content = content;
		return this;
	}

	@Override
	public <T> Builder content(T content, Class<? extends T> type) throws IllegalArgumentException {
		final MediaType mediaTypeForWrite = Objects.requireNonNullElse(mediaType, MediaType.APPLICATION_OCTET_STREAM_TYPE);

		@SuppressWarnings({ "unchecked", "rawtypes" })
		final MessageBodyWriter<T> writer = (MessageBodyWriter) ProviderFactory.getInstance(null).createMessageBodyWriter(type, null, null, mediaTypeForWrite, null);

		@SuppressWarnings({ "unchecked", "rawtypes" })
		final MultivaluedMap<String, Object> headersForWrite = (MultivaluedMap) headers;
		
		try {
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			writer.writeTo(content, type, null, null, mediaType, headersForWrite, out);
			content(new ByteArrayInputStream(out.toByteArray()));
		} catch (WebApplicationException e) {
			throw new IllegalArgumentException(e);
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}

		return this;
	}

	@Override
	public <T> Builder content(T content, GenericType<T> type) throws IllegalArgumentException {
		final MediaType mediaTypeForWrite = Objects.requireNonNullElse(mediaType, MediaType.APPLICATION_OCTET_STREAM_TYPE);

		@SuppressWarnings({ "unchecked", "rawtypes" })
		final MessageBodyWriter<T> writer = (MessageBodyWriter) ProviderFactory.getInstance(null).createMessageBodyWriter(type.getRawType(), type.getType(), null, mediaTypeForWrite, null);

		@SuppressWarnings({ "unchecked", "rawtypes" })
		final MultivaluedMap<String, Object> headersForWrite = (MultivaluedMap) headers;
		
		try {
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			writer.writeTo(content, type.getRawType(), type.getType(), null, mediaType, headersForWrite, out);
			content(new ByteArrayInputStream(out.toByteArray()));
		} catch (WebApplicationException e) {
			throw new IllegalArgumentException(e);
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
		
		return this;
	}

	@Override
	public EntityPart build() throws IllegalStateException, IOException, WebApplicationException {
		return new EntityPartImpl(name, fileName, content, headers, mediaType);
	}
	
}
