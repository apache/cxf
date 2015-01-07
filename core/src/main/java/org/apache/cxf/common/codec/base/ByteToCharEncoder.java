package org.apache.cxf.common.codec.base;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;

import org.apache.cxf.common.codec.IByteToCharEncoder;

/**
 * @author Mark A. Ziesemer
 * 	<a href="http://www.ziesemer.com.">&lt;www.ziesemer.com&gt;</a>
 * @see java.nio.charset.CharsetDecoder
 */
public abstract class ByteToCharEncoder<T extends ByteToCharEncoder<T>>
		extends BaseCoder<ByteBuffer, CharBuffer, T>
		implements IByteToCharEncoder{

	private static final long serialVersionUID = 1L;

	protected ByteToCharEncoder(float minInPerOut, float averageCharsPerByte, float maxCharsPerByte){
		super(minInPerOut, averageCharsPerByte, maxCharsPerByte);
	}

	@Override
	protected CharBuffer allocate(int capacity){
		return CharBuffer.allocate(capacity);
	}

	@Override
	protected CharBuffer put(CharBuffer base, CharBuffer put){
		return base.put(put);
	}

	/**
	 * Convenience method for {@link #code(java.nio.Buffer)}.
	 * Does not handle all desired variations by design, e.g. only encoding a portion of the input.
	 */
	public String encodeToString(byte[] in) throws CharacterCodingException{
		CharBuffer out = code(ByteBuffer.wrap(in));
		return out.toString();
	}

}
