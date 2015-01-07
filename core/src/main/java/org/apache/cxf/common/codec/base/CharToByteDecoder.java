package org.apache.cxf.common.codec.base;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;

import org.apache.cxf.common.codec.ICharToByteDecoder;

/**
 * @author Mark A. Ziesemer
 * 	<a href="http://www.ziesemer.com.">&lt;www.ziesemer.com&gt;</a>
 * @see java.nio.charset.CharsetEncoder
 */
public abstract class CharToByteDecoder<T extends CharToByteDecoder<T>>
		extends BaseCoder<CharBuffer, ByteBuffer, T>
		implements ICharToByteDecoder{

	private static final long serialVersionUID = 1L;

	protected CharToByteDecoder(float minInPerOut, float averageBytesPerChar, float maxBytesPerChar){
		super(minInPerOut, averageBytesPerChar, maxBytesPerChar);
	}

	@Override
	protected ByteBuffer allocate(int capacity){
		return ByteBuffer.allocate(capacity);
	}

	@Override
	protected ByteBuffer put(ByteBuffer base, ByteBuffer put){
		return base.put(put);
	}

	/**
	 * Convenience method for {@link #code(java.nio.Buffer)}.
	 * Does not handle all desired variations by design, e.g. only decoding a portion of the input.
	 * (In this case, pass-in the result of {@link CharSequence#subSequence(int, int)} instead.)
	 */
	public byte[] decodeToBytes(CharSequence in) throws CharacterCodingException{
		ByteBuffer out = code(CharBuffer.wrap(in));
		byte[] result = new byte[out.remaining()];
		out.get(result);
		return result;
	}

}
