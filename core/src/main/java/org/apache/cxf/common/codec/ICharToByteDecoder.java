package org.apache.cxf.common.codec;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * @author Mark A. Ziesemer
 * 	<a href="http://www.ziesemer.com.">&lt;www.ziesemer.com&gt;</a>
 */
public interface ICharToByteDecoder extends ICoder<CharBuffer, ByteBuffer>{
	// Below not currently supported by javac: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6294779
	// public ICharToByteDecoder clone() throws CloneNotSupportedException;
}
