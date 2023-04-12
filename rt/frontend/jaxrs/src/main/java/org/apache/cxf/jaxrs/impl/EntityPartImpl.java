package org.apache.cxf.jaxrs.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.apache.cxf.jaxrs.provider.ProviderFactory;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;

public class EntityPartImpl implements EntityPart {

	private final String name;
	private final String fileName;
	private final InputStream content;
	private final MultivaluedMap<String, String> headers;
	private final MediaType mediaType;

	EntityPartImpl(String name, String fileName, InputStream content, MultivaluedMap<String, String> headers, MediaType mediaType) {
		this.name = name;
		this.fileName = fileName;
		this.content = content;
		this.headers = headers;
		this.mediaType = mediaType;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Optional<String> getFileName() {
		return Optional.ofNullable(fileName);
	}

	@Override
	public InputStream getContent() {
		return content;
	}

	@Override
	public <T> T getContent(Class<T> type) throws IllegalArgumentException, IllegalStateException, IOException, WebApplicationException {
		@SuppressWarnings({ "unchecked", "rawtypes" })
		final MessageBodyReader<T> reader = (MessageBodyReader) ProviderFactory.getInstance(null).createMessageBodyReader(type, null, null, mediaType, null);

		return reader.readFrom(type, null, null, mediaType, headers, content);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getContent(GenericType<T> type) throws IllegalArgumentException, IllegalStateException, IOException, WebApplicationException {
		@SuppressWarnings("rawtypes")
		final MessageBodyReader<T> reader = (MessageBodyReader) ProviderFactory.getInstance(null).createMessageBodyReader(type.getRawType(), type.getType(), null, mediaType, null);

		return reader.readFrom((Class<T>) type.getRawType(), type.getType(), null, mediaType, headers, content);
	}

	@Override
	public MultivaluedMap<String, String> getHeaders() {
		return headers;
	}

	@Override
	public MediaType getMediaType() {
		return mediaType;
	}
	
}
