package org.apache.cxf.common.codec.impl;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CoderResult;

import org.apache.cxf.common.codec.base.CharToByteDecoder;

/**
 * <p>
 * 	Decodes from &quot;Percent-Encoding&quot; (a.k.a. &quot;URL-Encoding&quot;),
 * 		as specified by <a href="http://tools.ietf.org/html/rfc3986#section-2"
 * 		>http://tools.ietf.org/html/rfc3986#section-2</a>.
 * </p>
 * @author Mark A. Ziesemer
 * 	<a href="http://www.ziesemer.com.">&lt;www.ziesemer.com&gt;</a>
 */
public class URLDecoder
		extends CharToByteDecoder<URLDecoder>{

	private static final long serialVersionUID = 1L;

	public URLDecoder(){
		// Any byte may require 1-3 characters to represent.
		// Using the median of 2 as the mean for average characters per byte.
		// Reciprocal for average bytes per character is 1 / 2, or 0.5.
		super(3, 0.5f, 1);
	}

	@Override
	protected CoderResult codingLoop(final CharBuffer in, final ByteBuffer out, final boolean endOfInput){
		while(in.hasRemaining()){
			if(out.remaining() < maxOutPerIn){
				return CoderResult.OVERFLOW;
			}

			int mark = in.position();
			final char c = in.get();
			if(c == '%'){
				if(in.remaining() >= 2){
					int x, y;
					if((x = Character.digit(in.get(), 0x10)) == -1
							|| (y = Character.digit(in.get(), 0x10)) == -1){
						int malLength = in.position() - mark;
						in.position(mark);
						return CoderResult.malformedForLength(malLength);
					}
					out.put((byte)((x << 4) + y));
				}else{
					// Push back the '%', and wait for more.
					in.position(in.position() - 1);
					return CoderResult.UNDERFLOW;
				}
			}else if(c == '+'){
				out.put((byte)' ');
			}else{
				out.put((byte)c);
			}
		}
		return CoderResult.UNDERFLOW;
	}

}
