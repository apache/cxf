package org.apache.cxf.common.codec.impl;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CoderResult;
import java.util.BitSet;

import org.apache.cxf.common.codec.base.ByteToCharEncoder;
import org.apache.cxf.common.codec.charlist.HexUpperCharList;

/**
 * <p>
 * 	Encodes to &quot;Percent-Encoding&quot; (a.k.a. &quot;URL-Encoding&quot;),
 * 		as specified by <a href="http://tools.ietf.org/html/rfc3986#section-2"
 * 		>http://tools.ietf.org/html/rfc3986#section-2</a>.
 * </p>
 * @author Mark A. Ziesemer
 * 	<a href="http://www.ziesemer.com.">&lt;www.ziesemer.com&gt;</a>
 */
public class URLEncoder
		extends ByteToCharEncoder<URLEncoder>{

	private static final long serialVersionUID = 1L;

	/**
	 * <p><a href="http://tools.ietf.org/html/rfc3986#section-2.3"
	 * 		>http://tools.ietf.org/html/rfc3986#section-2.3</a>.</p>
	 */
	private static final BitSet UNRESERVED = new BitSet(0x100);

	/**
	 * <p>Per <a href="http://tools.ietf.org/html/rfc3986#section-2.1"
	 * 		>http://tools.ietf.org/html/rfc3986#section-2.1</a>, upper-case should be
	 * 		used for encoding (though both lower- and upper-case should be decoded).</p>
	 */
	private static final char[] HEX_CHARS = HexUpperCharList.build();

	static{
		// http://tools.ietf.org/html/rfc3986#section-2.3
		for(int i = 'a'; i <= 'z'; i++){
			UNRESERVED.set(i);
		}
		for(int i = 'A'; i <= 'Z'; i++){
			UNRESERVED.set(i);
		}
		for(int i = '0'; i <= '9'; i++){
			UNRESERVED.set(i);
		}

		UNRESERVED.set('-');
		UNRESERVED.set('_');
		UNRESERVED.set('.');
		UNRESERVED.set('*');
		// Will be replaced with '+'.
		UNRESERVED.set(' ');
	}

	public URLEncoder(){
		// Any byte may require 1-3 characters to represent.
		// Using the median of 2 as the mean.
		super(1, 2, 3);
	}

	@Override
	protected CoderResult codingLoop(final ByteBuffer in, final CharBuffer out, final boolean endOfInput){
		while(in.hasRemaining()){
			if(out.remaining() < maxOutPerIn){
				return CoderResult.OVERFLOW;
			}

			int b = in.get() & 0xFF;
			if(UNRESERVED.get(b)){
				if(b == ' '){
					b = '+';
				}
				out.put((char)b);
			}else{
				out.put('%');
				out.put(HEX_CHARS[(b >> 4) & 0xF]);
				out.put(HEX_CHARS[b & 0xF]);
			}
		}
		return CoderResult.UNDERFLOW;
	}

}
