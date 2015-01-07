package org.apache.cxf.common.codec;

import java.io.Serializable;
import java.nio.Buffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CoderResult;

/**
 * <p>
 * 	Base API for high-performance encoding and decoding between various {@link Buffer}s.
 * 	Supports conversions between {@link java.nio.ByteBuffer}s and
 * 		{@link java.nio.CharBuffer}s through the {@link IByteToCharEncoder} and
 * 		{@link ICharToByteDecoder} child interfaces, e.g. URL/Percent and Base64 codecs.
 * </p>
 * <p>
 * 	This API is similar in design to {@link java.nio.charset.CharsetEncoder} and
 * 		{@link java.nio.charset.CharsetDecoder}.
 * </p>
 * @param <IN> The type of {@link Buffer} being read from.
 * @param <OUT> The type of {@link Buffer} being written to.
 * @author Mark A. Ziesemer
 * 	<a href="http://www.ziesemer.com.">&lt;www.ziesemer.com&gt;</a>
 */
public interface ICoder<IN extends Buffer, OUT extends Buffer>
		extends Cloneable, Serializable{

	/**
	 * <p>Configures this instances based upon another.
	 * 	The passed-in parameter must be an instance of the current implementation,
	 * 		or an {@link IllegalArgumentException} will be thrown.</p>
	 * @param base The instance to copy the configuration from.
	 * @return The current instance, supporting the builder pattern.
	 * @throws IllegalArgumentException If the passed-in parameter is not a
	 * 	class-compatible instance of the current implementation.
	 */
	ICoder<IN, OUT> config(ICoder<IN, OUT> base);

	/**
	 * <p>Another means for obtaining a configured instance copy from another.
	 * 	Typically utilizes {@link #config(ICoder)}.</p>
	 * @return A copy of the current instance with the same configuration.
	 * @throws CloneNotSupportedException
	 */
	ICoder<IN, OUT> clone() throws CloneNotSupportedException;

	/**
	 * <p>Heuristic value of the minimum number of input units required to produce
	 * 		one or more output units.</p>
	 * <p>Similar to {@link #getAverageOutPerIn()} and {@link #getMaxOutPerIn()}.</p>
	 */
	float getMinInPerOut();

	/**
	 * <p>Heuristic value of the average number of output units generated per input unit.</p>
	 * <p>Similar to {@link java.nio.charset.CharsetEncoder#averageBytesPerChar()}
	 * 		and {@link java.nio.charset.CharsetDecoder#averageCharsPerByte()}.</p>
	 */
	float getAverageOutPerIn();

	/**
	 * <p>Heuristic value of the maximum number of output units generated per input unit.</p>
	 * <p>Similar to {@link java.nio.charset.CharsetEncoder#maxBytesPerChar()}
	 * 		and {@link java.nio.charset.CharsetDecoder#maxCharsPerByte()}.</p>
	 */
	float getMaxOutPerIn();
	
	/**
	 * <p>Resets the coder, allowing it to be reused against new input and output,
	 * 	while keeping the same configuration.</p>
	 */
	ICoder<IN, OUT> reset();
	
	/**
	 * <p>Convenience method that encodes the passed-in input {@link Buffer} to a
	 * 		returned output {@link Buffer}.</p>
	 * <p>Functions similar to
	 * 	{@link java.nio.charset.CharsetEncoder#encode(java.nio.CharBuffer)} and
	 * 	{@link java.nio.charset.CharsetDecoder#decode(java.nio.ByteBuffer)}.</p>
	 * @param in The input {@link Buffer} to read from.
	 * @return The output {@link Buffer} written to.
	 * @throws CharacterCodingException
	 */
	OUT code(IN in) throws CharacterCodingException;

	/**
	 * <p>Encodes as many units as possible from the given input buffer,
	 * 		writing the results to the given output buffer.</p>
	 * <p>Functions similar to
	 * 	{@link java.nio.charset.CharsetEncoder#encode(java.nio.CharBuffer, java.nio.ByteBuffer, boolean)} and
	 * 	{@link java.nio.charset.CharsetDecoder#decode(java.nio.ByteBuffer, java.nio.CharBuffer, boolean)}.</p>
	 * @param in The input {@link Buffer} to read from.
	 * @param out The output {@link Buffer} to write to.
	 * @param endOfInput <code>true</code> if, and only if, the invoker can provide no
	 * 	additional input characters beyond those in the given buffer.
	 */
	CoderResult code(IN in, OUT out, boolean endOfInput);

	/**
	 * <p>Writes any final output after the entire input has been processed.
	 * 	Should be called after {@link #code(Buffer, Buffer, boolean)} is
	 * 		called for the final time.</p>
	 * <p>Functions similar to
	 * 	{@link java.nio.charset.CharsetEncoder#flush(java.nio.ByteBuffer)} and
	 * 	{@link java.nio.charset.CharsetDecoder#flush(java.nio.CharBuffer)}.</p>
	 * @param out The output {@link Buffer} to write to.
	 */
	CoderResult flush(OUT out);

}
