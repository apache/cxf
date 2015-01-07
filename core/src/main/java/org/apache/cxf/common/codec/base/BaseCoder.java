package org.apache.cxf.common.codec.base;

import java.nio.Buffer;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CoderMalfunctionError;
import java.nio.charset.CoderResult;

import org.apache.cxf.common.codec.ICoder;

/**
 * @param <IN> The type of {@link Buffer} being read from.
 * @param <OUT> The type of {@link Buffer} being written to.
 * @param <T> The concrete type that is extending this class.
 * 	Used to provide automatic covariant return types for
 * 		{@link #config(ICoder)}, {@link #clone()}, and {@link #reset()}.
 * @author Mark A. Ziesemer
 * 	<a href="http://www.ziesemer.com.">&lt;www.ziesemer.com&gt;</a>
 */
public abstract class BaseCoder<IN extends Buffer, OUT extends Buffer, T extends BaseCoder<IN, OUT, T>>
		implements ICoder<IN, OUT>{

	private static final long serialVersionUID = 1L;

	protected float minInPerOut;
	protected float averageOutPerIn;
	protected float maxOutPerIn;

	protected transient CodingStates state = CodingStates.CONFIG;

	protected BaseCoder(float minInPerOut, float averageOutPerIn, float maxOutPerIn){
		this.minInPerOut = minInPerOut;
		this.averageOutPerIn = averageOutPerIn;
		this.maxOutPerIn = maxOutPerIn;
	}

	@SuppressWarnings("unchecked")
	public T config(ICoder<IN, OUT> base){
		checkConfigAllowed();
		if(!getClass().isInstance(base)){
			throw new IllegalArgumentException(base.getClass().getName()
				+ " not an instance of " + getClass().getName());
		}
		configImpl((T)base);
		return (T)this;
	}

	/**
	 * <p>Hook for implementations to configure themselves from another instance.
	 * 	The base implementation does nothing.</p>
	 * @param base The instance to copy the configuration from.
	 */
	protected void configImpl(T base){
		// Hook; nothing to do by default.
	}

	@SuppressWarnings("unchecked")
	@Override
	public T clone() throws CloneNotSupportedException{
		T base = (T)super.clone();
		base.config(this);
		return base;
	}

	/**
	 * <p>Hook called once after the first coding operation is started.
	 * 	The base implementation does nothing.</p>
	 */
	protected void init(){
		// Hook.
	}

	protected <TA> TA checkNullArgument(TA o){
		if(o == null){
			throw new IllegalArgumentException("Argument must not be null.");
		}
		return o;
	}

	public float getMinInPerOut(){
		return minInPerOut;
	}

	public float getAverageOutPerIn(){
		return averageOutPerIn;
	}

	public float getMaxOutPerIn(){
		return maxOutPerIn;
	}

	@SuppressWarnings("unchecked")
	public T reset(){
		resetImpl();
		if(state == CodingStates.CONFIG){
			init();
		}
		state = CodingStates.RESET;
		return (T)this;
	}

	/**
	 * <p>Hook called from {@link #reset()}.
	 * 	The base implementation does nothing.</p>
	 */
	protected void resetImpl(){
		// Hook.
	}

	public OUT code(final IN in) throws CharacterCodingException{
		int n = (int)Math.ceil(in.remaining() * averageOutPerIn);
		OUT out = allocate(n);

		if(in.remaining() > 0){
			while(true){
				CoderResult cr;
				if(in.hasRemaining()){
					cr = code(in, out, true);
				}else{
					cr = CoderResult.UNDERFLOW;
				}
				if(cr.isUnderflow()){
					cr = flush(out);
				}
				if(cr.isUnderflow()){
					break;
				}
				if(cr.isOverflow()){
					out.flip();
					n = (n << 1) | 1; // Ensure progress; n might be 0!
					out = put(allocate(n), out);
				}else{
					cr.throwException();
				}
			}
		}
		reset();
		out.flip();
		return out;
	}

	protected abstract OUT allocate(int capacity);

	protected abstract OUT put(OUT base, OUT put);

	public CoderResult code(final IN in, final OUT out, final boolean endOfInput){
		CodingStates newState = endOfInput ? CodingStates.END : CodingStates.CODING;
		if(!(state.ordinal() <= CodingStates.CODING.ordinal())
				&& !(endOfInput && (state == CodingStates.END))){
			throwIllegalStateException(state, newState);
		}else if(state == CodingStates.CONFIG){
			init();
		}
		state = newState;

		CoderResult cr;
		try{
			cr = codingLoop(in, out, endOfInput);
		}catch(BufferUnderflowException x){
			throw new CoderMalfunctionError(x);
		}catch(BufferOverflowException x){
			throw new CoderMalfunctionError(x);
		}

		if(cr.isOverflow()){
			return cr;
		}

		if(cr.isUnderflow()){
			if(endOfInput && in.hasRemaining()){
				return CoderResult.malformedForLength(in.remaining());
			}
			return cr;
		}

		if(cr.isError()){
			return cr;
		}

		throw new CoderMalfunctionError(new Exception("Unexpected CoderResult."));
	}

	protected abstract CoderResult codingLoop(IN in, OUT out, boolean endOfInput);

	public CoderResult flush(final OUT out){
		if(state == CodingStates.END){
			CoderResult cr = implFlush(out);
			state = CodingStates.FLUSHED;
			return cr;
		}

		if(state != CodingStates.FLUSHED){
			throwIllegalStateException(state, CodingStates.FLUSHED);
		}

		return CoderResult.UNDERFLOW; // Already flushed
	}

	/**
	 * @see java.nio.charset.CharsetEncoder#implFlush(java.nio.ByteBuffer)
	 * @see java.nio.charset.CharsetDecoder#implFlush(java.nio.CharBuffer)
	 */
	protected CoderResult implFlush(final OUT out){
		return CoderResult.UNDERFLOW;
	}

	protected void checkConfigAllowed(){
		if(state.ordinal() > CodingStates.RESET.ordinal()){
			throw new IllegalStateException("Configuration not allowed unless reset.");
		}
		state = CodingStates.CONFIG;
	}

	protected void throwIllegalStateException(CodingStates from, CodingStates to){
		throw new IllegalStateException("Current state = " + from
			+ ", new state = " + to);
	}
}
