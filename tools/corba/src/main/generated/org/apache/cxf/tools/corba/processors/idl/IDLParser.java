// $ANTLR 2.7.4: "idl.g" -> "IDLParser.java"$
// Generated
package org.apache.cxf.tools.corba.processors.idl;

import java.io.*;
import java.util.Vector;
import java.util.Hashtable;
 
import antlr.TokenBuffer;
import antlr.TokenStreamException;
import antlr.TokenStreamIOException;
import antlr.ANTLRException;
import antlr.LLkParser;
import antlr.Token;
import antlr.TokenStream;
import antlr.RecognitionException;
import antlr.NoViableAltException;
import antlr.MismatchedTokenException;
import antlr.SemanticException;
import antlr.ParserSharedInputState;
import antlr.collections.impl.BitSet;
import antlr.collections.AST;
import java.util.Hashtable;
import antlr.ASTFactory;
import antlr.ASTPair;
import antlr.collections.impl.ASTArray;

@SuppressWarnings({"all", "PMD"})
public final class IDLParser extends antlr.LLkParser       implements IDLTokenTypes
 {

protected IDLParser(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
  buildTokenTypeASTClassMap();
  astFactory = new ASTFactory(getTokenTypeToASTClassMap());
}

public IDLParser(TokenBuffer tokenBuf) {
  this(tokenBuf,4);
}

protected IDLParser(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
  buildTokenTypeASTClassMap();
  astFactory = new ASTFactory(getTokenTypeToASTClassMap());
}

public IDLParser(TokenStream lexer) {
  this(lexer,4);
}

public IDLParser(ParserSharedInputState state) {
  super(state,4);
  tokenNames = _tokenNames;
  buildTokenTypeASTClassMap();
  astFactory = new ASTFactory(getTokenTypeToASTClassMap());
}

	public void specification() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST specification_AST = null;
		
		try {      // for error handling
			{
			_loop3:
			do {
				if ((LA(1)==LITERAL_import)) {
					import_dcl();
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop3;
				}
				
			} while (true);
			}
			{
			int _cnt5=0;
			_loop5:
			do {
				if ((_tokenSet_0.member(LA(1)))) {
					definition();
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					if ( _cnt5>=1 ) { break _loop5; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt5++;
			} while (true);
			}
			specification_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_1);
			} else {
			  throw ex;
			}
		}
		returnAST = specification_AST;
	}
	
	public final void import_dcl() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST import_dcl_AST = null;
		
		try {      // for error handling
			AST tmp1_AST = null;
			tmp1_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp1_AST);
			match(LITERAL_import);
			imported_scope();
			astFactory.addASTChild(currentAST, returnAST);
			match(SEMI);
			import_dcl_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_2);
			} else {
			  throw ex;
			}
		}
		returnAST = import_dcl_AST;
	}
	
	public final void definition() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST definition_AST = null;
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case LITERAL_typedef:
			case LITERAL_native:
			case LITERAL_struct:
			case LITERAL_union:
			case LITERAL_enum:
			{
				type_dcl();
				astFactory.addASTChild(currentAST, returnAST);
				match(SEMI);
				break;
			}
			case LITERAL_const:
			{
				const_dcl();
				astFactory.addASTChild(currentAST, returnAST);
				match(SEMI);
				break;
			}
			case LITERAL_exception:
			{
				except_dcl();
				astFactory.addASTChild(currentAST, returnAST);
				match(SEMI);
				break;
			}
			case LITERAL_module:
			{
				module();
				astFactory.addASTChild(currentAST, returnAST);
				match(SEMI);
				break;
			}
			case LITERAL_typeid:
			{
				type_id_dcl();
				astFactory.addASTChild(currentAST, returnAST);
				match(SEMI);
				break;
			}
			case LITERAL_typeprefix:
			{
				type_prefix_dcl();
				astFactory.addASTChild(currentAST, returnAST);
				match(SEMI);
				break;
			}
			case LITERAL_component:
			{
				component();
				astFactory.addASTChild(currentAST, returnAST);
				match(SEMI);
				break;
			}
			case LITERAL_home:
			{
				home_dcl();
				astFactory.addASTChild(currentAST, returnAST);
				match(SEMI);
				break;
			}
			default:
				boolean synPredMatched10 = false;
				if ((((LA(1) >= LITERAL_abstract && LA(1) <= LITERAL_interface)) && (LA(2)==LITERAL_interface||LA(2)==IDENT) && (_tokenSet_3.member(LA(3))) && (_tokenSet_4.member(LA(4))))) {
					int _m10 = mark();
					synPredMatched10 = true;
					inputState.guessing++;
					try {
						{
						{
						switch ( LA(1)) {
						case LITERAL_abstract:
						{
							match(LITERAL_abstract);
							break;
						}
						case LITERAL_local:
						{
							match(LITERAL_local);
							break;
						}
						case LITERAL_interface:
						{
							break;
						}
						default:
						{
							throw new NoViableAltException(LT(1), getFilename());
						}
						}
						}
						match(LITERAL_interface);
						}
					}
					catch (RecognitionException pe) {
						synPredMatched10 = false;
					}
					rewind(_m10);
					inputState.guessing--;
				}
				if ( synPredMatched10 ) {
					interf();
					astFactory.addASTChild(currentAST, returnAST);
					match(SEMI);
				}
				else {
					boolean synPredMatched13 = false;
					if (((LA(1)==LITERAL_abstract||LA(1)==LITERAL_custom||LA(1)==LITERAL_valuetype) && (LA(2)==LITERAL_valuetype||LA(2)==IDENT) && (_tokenSet_5.member(LA(3))) && (_tokenSet_6.member(LA(4))))) {
						int _m13 = mark();
						synPredMatched13 = true;
						inputState.guessing++;
						try {
							{
							{
							switch ( LA(1)) {
							case LITERAL_abstract:
							{
								match(LITERAL_abstract);
								break;
							}
							case LITERAL_custom:
							{
								match(LITERAL_custom);
								break;
							}
							case LITERAL_valuetype:
							{
								break;
							}
							default:
							{
								throw new NoViableAltException(LT(1), getFilename());
							}
							}
							}
							match(LITERAL_valuetype);
							}
						}
						catch (RecognitionException pe) {
							synPredMatched13 = false;
						}
						rewind(_m13);
						inputState.guessing--;
					}
					if ( synPredMatched13 ) {
						value();
						astFactory.addASTChild(currentAST, returnAST);
						match(SEMI);
					}
					else {
						boolean synPredMatched16 = false;
						if (((LA(1)==LITERAL_abstract||LA(1)==LITERAL_custom||LA(1)==LITERAL_eventtype) && (LA(2)==LITERAL_eventtype||LA(2)==IDENT) && (_tokenSet_7.member(LA(3))) && (_tokenSet_8.member(LA(4))))) {
							int _m16 = mark();
							synPredMatched16 = true;
							inputState.guessing++;
							try {
								{
								{
								switch ( LA(1)) {
								case LITERAL_abstract:
								{
									match(LITERAL_abstract);
									break;
								}
								case LITERAL_custom:
								{
									match(LITERAL_custom);
									break;
								}
								case LITERAL_eventtype:
								{
									break;
								}
								default:
								{
									throw new NoViableAltException(LT(1), getFilename());
								}
								}
								}
								match(LITERAL_eventtype);
								}
							}
							catch (RecognitionException pe) {
								synPredMatched16 = false;
							}
							rewind(_m16);
							inputState.guessing--;
						}
						if ( synPredMatched16 ) {
							event();
							astFactory.addASTChild(currentAST, returnAST);
							match(SEMI);
						}
					else {
						throw new NoViableAltException(LT(1), getFilename());
					}
					}}}
					}
					definition_AST = (AST)currentAST.root;
				}
				catch (RecognitionException ex) {
					if (inputState.guessing==0) {
						reportError(ex);
						consume();
						consumeUntil(_tokenSet_9);
					} else {
					  throw ex;
					}
				}
				returnAST = definition_AST;
			}
			
	public final void type_dcl() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST type_dcl_AST = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case LITERAL_typedef:
			{
				AST tmp14_AST = null;
				tmp14_AST = astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp14_AST);
				match(LITERAL_typedef);
				type_declarator();
				astFactory.addASTChild(currentAST, returnAST);
				type_dcl_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_enum:
			{
				enum_type();
				astFactory.addASTChild(currentAST, returnAST);
				type_dcl_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_native:
			{
				AST tmp15_AST = null;
				tmp15_AST = astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp15_AST);
				match(LITERAL_native);
				simple_declarator();
				astFactory.addASTChild(currentAST, returnAST);
				type_dcl_AST = (AST)currentAST.root;
				break;
			}
			default:
				boolean synPredMatched114 = false;
				if (((LA(1)==LITERAL_struct) && (LA(2)==IDENT) && (LA(3)==LCURLY))) {
					int _m114 = mark();
					synPredMatched114 = true;
					inputState.guessing++;
					try {
						{
						struct_type();
						}
					}
					catch (RecognitionException pe) {
						synPredMatched114 = false;
					}
					rewind(_m114);
					inputState.guessing--;
				}
				if ( synPredMatched114 ) {
					struct_type();
					astFactory.addASTChild(currentAST, returnAST);
					type_dcl_AST = (AST)currentAST.root;
				}
				else {
					boolean synPredMatched116 = false;
					if (((LA(1)==LITERAL_union) && (LA(2)==IDENT) && (LA(3)==LITERAL_switch))) {
						int _m116 = mark();
						synPredMatched116 = true;
						inputState.guessing++;
						try {
							{
							union_type();
							}
						}
						catch (RecognitionException pe) {
							synPredMatched116 = false;
						}
						rewind(_m116);
						inputState.guessing--;
					}
					if ( synPredMatched116 ) {
						union_type();
						astFactory.addASTChild(currentAST, returnAST);
						type_dcl_AST = (AST)currentAST.root;
					}
					else if ((LA(1)==LITERAL_struct||LA(1)==LITERAL_union) && (LA(2)==IDENT) && (LA(3)==SEMI)) {
						constr_forward_decl();
						astFactory.addASTChild(currentAST, returnAST);
						type_dcl_AST = (AST)currentAST.root;
					}
				else {
					throw new NoViableAltException(LT(1), getFilename());
				}
				}}
			}
			catch (RecognitionException ex) {
				if (inputState.guessing==0) {
					reportError(ex);
					consume();
					consumeUntil(_tokenSet_10);
				} else {
				  throw ex;
				}
			}
			returnAST = type_dcl_AST;
		}
		
	public final void const_dcl() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST const_dcl_AST = null;
		
		try {      // for error handling
			AST tmp16_AST = null;
			tmp16_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp16_AST);
			match(LITERAL_const);
			const_type();
			astFactory.addASTChild(currentAST, returnAST);
			identifier();
			astFactory.addASTChild(currentAST, returnAST);
			match(ASSIGN);
			const_exp();
			astFactory.addASTChild(currentAST, returnAST);
			const_dcl_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = const_dcl_AST;
	}
	
	public final void except_dcl() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST except_dcl_AST = null;
		
		try {      // for error handling
			AST tmp18_AST = null;
			tmp18_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp18_AST);
			match(LITERAL_exception);
			identifier();
			astFactory.addASTChild(currentAST, returnAST);
			match(LCURLY);
			opt_member_list();
			astFactory.addASTChild(currentAST, returnAST);
			match(RCURLY);
			except_dcl_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = except_dcl_AST;
	}
	
	public final void interf() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST interf_AST = null;
		
		try {      // for error handling
			{
			if (((LA(1) >= LITERAL_abstract && LA(1) <= LITERAL_interface)) && (LA(2)==LITERAL_interface||LA(2)==IDENT) && (LA(3)==LCURLY||LA(3)==COLON||LA(3)==IDENT) && (_tokenSet_11.member(LA(4)))) {
				interface_dcl();
				astFactory.addASTChild(currentAST, returnAST);
			}
			else if (((LA(1) >= LITERAL_abstract && LA(1) <= LITERAL_interface)) && (LA(2)==LITERAL_interface||LA(2)==IDENT) && (LA(3)==SEMI||LA(3)==IDENT) && (_tokenSet_12.member(LA(4)))) {
				forward_dcl();
				astFactory.addASTChild(currentAST, returnAST);
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
			interf_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = interf_AST;
	}
	
	public final void module() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST module_AST = null;
		AST d_AST = null;
		
		try {      // for error handling
			AST tmp21_AST = null;
			tmp21_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp21_AST);
			match(LITERAL_module);
			identifier();
			astFactory.addASTChild(currentAST, returnAST);
			match(LCURLY);
			definition_list();
			d_AST = (AST)returnAST;
			astFactory.addASTChild(currentAST, returnAST);
			match(RCURLY);
			module_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = module_AST;
	}
	
	public final void value() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST value_AST = null;
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case LITERAL_abstract:
			{
				value_abs_dcl();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case LITERAL_custom:
			{
				value_custom_dcl();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			default:
				if ((LA(1)==LITERAL_valuetype) && (LA(2)==IDENT) && (LA(3)==LCURLY||LA(3)==COLON||LA(3)==LITERAL_supports)) {
					value_dcl();
					astFactory.addASTChild(currentAST, returnAST);
				}
				else if ((LA(1)==LITERAL_valuetype) && (LA(2)==IDENT) && (_tokenSet_13.member(LA(3)))) {
					value_box_dcl();
					astFactory.addASTChild(currentAST, returnAST);
				}
				else if ((LA(1)==LITERAL_valuetype) && (LA(2)==IDENT) && (LA(3)==SEMI)) {
					value_forward_dcl();
					astFactory.addASTChild(currentAST, returnAST);
				}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			value_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = value_AST;
	}
	
	public final void type_id_dcl() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST type_id_dcl_AST = null;
		
		try {      // for error handling
			AST tmp24_AST = null;
			tmp24_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp24_AST);
			match(LITERAL_typeid);
			scoped_name();
			astFactory.addASTChild(currentAST, returnAST);
			string_literal();
			astFactory.addASTChild(currentAST, returnAST);
			type_id_dcl_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = type_id_dcl_AST;
	}
	
	public final void type_prefix_dcl() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST type_prefix_dcl_AST = null;
		
		try {      // for error handling
			AST tmp25_AST = null;
			tmp25_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp25_AST);
			match(LITERAL_typeprefix);
			scoped_name();
			astFactory.addASTChild(currentAST, returnAST);
			string_literal();
			astFactory.addASTChild(currentAST, returnAST);
			type_prefix_dcl_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = type_prefix_dcl_AST;
	}
	
	public final void event() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST event_AST = null;
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case LITERAL_abstract:
			{
				event_abs();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case LITERAL_custom:
			{
				event_custom();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case LITERAL_eventtype:
			{
				event_dcl();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			event_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = event_AST;
	}
	
	public final void component() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST component_AST = null;
		
		try {      // for error handling
			AST tmp26_AST = null;
			tmp26_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp26_AST);
			match(LITERAL_component);
			identifier();
			astFactory.addASTChild(currentAST, returnAST);
			{
			switch ( LA(1)) {
			case LCURLY:
			case COLON:
			case LITERAL_supports:
			{
				component_dcl();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case SEMI:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			component_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = component_AST;
	}
	
	public final void home_dcl() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST home_dcl_AST = null;
		
		try {      // for error handling
			home_header();
			astFactory.addASTChild(currentAST, returnAST);
			home_body();
			astFactory.addASTChild(currentAST, returnAST);
			home_dcl_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = home_dcl_AST;
	}
	
	public final void identifier() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST identifier_AST = null;
		
		try {      // for error handling
			AST tmp27_AST = null;
			tmp27_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp27_AST);
			match(IDENT);
			identifier_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_14);
			} else {
			  throw ex;
			}
		}
		returnAST = identifier_AST;
	}
	
	public final void definition_list() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST definition_list_AST = null;
		
		try {      // for error handling
			{
			int _cnt20=0;
			_loop20:
			do {
				if ((_tokenSet_0.member(LA(1)))) {
					definition();
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					if ( _cnt20>=1 ) { break _loop20; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt20++;
			} while (true);
			}
			definition_list_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_15);
			} else {
			  throw ex;
			}
		}
		returnAST = definition_list_AST;
	}
	
	public final void interface_dcl() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST interface_dcl_AST = null;
		
		try {      // for error handling
			{
			{
			switch ( LA(1)) {
			case LITERAL_abstract:
			{
				AST tmp28_AST = null;
				tmp28_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp28_AST);
				match(LITERAL_abstract);
				break;
			}
			case LITERAL_local:
			{
				AST tmp29_AST = null;
				tmp29_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp29_AST);
				match(LITERAL_local);
				break;
			}
			case LITERAL_interface:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			AST tmp30_AST = null;
			tmp30_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp30_AST);
			match(LITERAL_interface);
			identifier();
			astFactory.addASTChild(currentAST, returnAST);
			{
			switch ( LA(1)) {
			case COLON:
			{
				interface_inheritance_spec();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case LCURLY:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			AST tmp31_AST = null;
			tmp31_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp31_AST);
			match(LCURLY);
			interface_body();
			astFactory.addASTChild(currentAST, returnAST);
			AST tmp32_AST = null;
			tmp32_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp32_AST);
			match(RCURLY);
			}
			interface_dcl_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = interface_dcl_AST;
	}
	
	public final void forward_dcl() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST forward_dcl_AST = null;
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case LITERAL_abstract:
			{
				AST tmp33_AST = null;
				tmp33_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp33_AST);
				match(LITERAL_abstract);
				break;
			}
			case LITERAL_local:
			{
				AST tmp34_AST = null;
				tmp34_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp34_AST);
				match(LITERAL_local);
				break;
			}
			case LITERAL_interface:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			AST tmp35_AST = null;
			tmp35_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp35_AST);
			match(LITERAL_interface);
			identifier();
			astFactory.addASTChild(currentAST, returnAST);
			forward_dcl_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = forward_dcl_AST;
	}
	
	public final void interface_inheritance_spec() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST interface_inheritance_spec_AST = null;
		
		try {      // for error handling
			AST tmp36_AST = null;
			tmp36_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp36_AST);
			match(COLON);
			scoped_name_list();
			astFactory.addASTChild(currentAST, returnAST);
			interface_inheritance_spec_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_16);
			} else {
			  throw ex;
			}
		}
		returnAST = interface_inheritance_spec_AST;
	}
	
	public final void interface_body() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST interface_body_AST = null;
		
		try {      // for error handling
			{
			_loop31:
			do {
				if ((_tokenSet_17.member(LA(1)))) {
					export();
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop31;
				}
				
			} while (true);
			}
			interface_body_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_15);
			} else {
			  throw ex;
			}
		}
		returnAST = interface_body_AST;
	}
	
	public final void export() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST export_AST = null;
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case LITERAL_typedef:
			case LITERAL_native:
			case LITERAL_struct:
			case LITERAL_union:
			case LITERAL_enum:
			{
				type_dcl();
				astFactory.addASTChild(currentAST, returnAST);
				match(SEMI);
				break;
			}
			case LITERAL_const:
			{
				const_dcl();
				astFactory.addASTChild(currentAST, returnAST);
				match(SEMI);
				break;
			}
			case LITERAL_exception:
			{
				except_dcl();
				astFactory.addASTChild(currentAST, returnAST);
				match(SEMI);
				break;
			}
			case LITERAL_readonly:
			case LITERAL_attribute:
			{
				attr_dcl();
				astFactory.addASTChild(currentAST, returnAST);
				match(SEMI);
				break;
			}
			case SCOPEOP:
			case IDENT:
			case LITERAL_float:
			case LITERAL_double:
			case LITERAL_long:
			case LITERAL_short:
			case LITERAL_unsigned:
			case LITERAL_char:
			case LITERAL_wchar:
			case LITERAL_boolean:
			case LITERAL_octet:
			case LITERAL_any:
			case LITERAL_Object:
			case LITERAL_string:
			case LITERAL_wstring:
			case LITERAL_oneway:
			case LITERAL_void:
			case LITERAL_ValueBase:
			{
				op_dcl();
				astFactory.addASTChild(currentAST, returnAST);
				match(SEMI);
				break;
			}
			case LITERAL_typeid:
			{
				type_id_dcl();
				astFactory.addASTChild(currentAST, returnAST);
				match(SEMI);
				break;
			}
			case LITERAL_typeprefix:
			{
				type_prefix_dcl();
				astFactory.addASTChild(currentAST, returnAST);
				match(SEMI);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			export_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_18);
			} else {
			  throw ex;
			}
		}
		returnAST = export_AST;
	}
	
	public final void attr_dcl() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST attr_dcl_AST = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case LITERAL_readonly:
			{
				readonly_attr_spec();
				astFactory.addASTChild(currentAST, returnAST);
				attr_dcl_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_attribute:
			{
				attr_spec();
				astFactory.addASTChild(currentAST, returnAST);
				attr_dcl_AST = (AST)currentAST.root;
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = attr_dcl_AST;
	}
	
	public final void op_dcl() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST op_dcl_AST = null;
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case LITERAL_oneway:
			{
				op_attribute();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case SCOPEOP:
			case IDENT:
			case LITERAL_float:
			case LITERAL_double:
			case LITERAL_long:
			case LITERAL_short:
			case LITERAL_unsigned:
			case LITERAL_char:
			case LITERAL_wchar:
			case LITERAL_boolean:
			case LITERAL_octet:
			case LITERAL_any:
			case LITERAL_Object:
			case LITERAL_string:
			case LITERAL_wstring:
			case LITERAL_void:
			case LITERAL_ValueBase:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			op_type_spec();
			astFactory.addASTChild(currentAST, returnAST);
			AST tmp44_AST = null;
			tmp44_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp44_AST);
			match(IDENT);
			parameter_dcls();
			astFactory.addASTChild(currentAST, returnAST);
			{
			switch ( LA(1)) {
			case LITERAL_raises:
			{
				raises_expr();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case SEMI:
			case LITERAL_context:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			{
			switch ( LA(1)) {
			case LITERAL_context:
			{
				context_expr();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case SEMI:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			op_dcl_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = op_dcl_AST;
	}
	
	public final void scoped_name_list() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST scoped_name_list_AST = null;
		
		try {      // for error handling
			scoped_name();
			astFactory.addASTChild(currentAST, returnAST);
			{
			_loop38:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					scoped_name();
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop38;
				}
				
			} while (true);
			}
			scoped_name_list_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_19);
			} else {
			  throw ex;
			}
		}
		returnAST = scoped_name_list_AST;
	}
	
	public final void interface_name() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST interface_name_AST = null;
		
		try {      // for error handling
			scoped_name();
			astFactory.addASTChild(currentAST, returnAST);
			interface_name_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_20);
			} else {
			  throw ex;
			}
		}
		returnAST = interface_name_AST;
	}
	
	public final void scoped_name() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST scoped_name_AST = null;
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case SCOPEOP:
			{
				AST tmp46_AST = null;
				tmp46_AST = astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp46_AST);
				match(SCOPEOP);
				break;
			}
			case IDENT:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			AST tmp47_AST = null;
			tmp47_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp47_AST);
			match(IDENT);
			{
			_loop42:
			do {
				if ((LA(1)==SCOPEOP)) {
					match(SCOPEOP);
					identifier();
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop42;
				}
				
			} while (true);
			}
			scoped_name_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_21);
			} else {
			  throw ex;
			}
		}
		returnAST = scoped_name_AST;
	}
	
	public final void value_dcl() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST value_dcl_AST = null;
		
		try {      // for error handling
			value_header();
			astFactory.addASTChild(currentAST, returnAST);
			match(LCURLY);
			{
			_loop54:
			do {
				if ((_tokenSet_22.member(LA(1)))) {
					value_element();
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop54;
				}
				
			} while (true);
			}
			match(RCURLY);
			value_dcl_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = value_dcl_AST;
	}
	
	public final void value_abs_dcl() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST value_abs_dcl_AST = null;
		
		try {      // for error handling
			AST tmp51_AST = null;
			tmp51_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp51_AST);
			match(LITERAL_abstract);
			AST tmp52_AST = null;
			tmp52_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp52_AST);
			match(LITERAL_valuetype);
			identifier();
			astFactory.addASTChild(currentAST, returnAST);
			{
			switch ( LA(1)) {
			case LCURLY:
			case COLON:
			case LITERAL_supports:
			{
				value_abs_full_dcl();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case SEMI:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			value_abs_dcl_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = value_abs_dcl_AST;
	}
	
	public final void value_box_dcl() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST value_box_dcl_AST = null;
		
		try {      // for error handling
			AST tmp53_AST = null;
			tmp53_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp53_AST);
			match(LITERAL_valuetype);
			identifier();
			astFactory.addASTChild(currentAST, returnAST);
			type_spec();
			astFactory.addASTChild(currentAST, returnAST);
			value_box_dcl_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = value_box_dcl_AST;
	}
	
	public final void value_custom_dcl() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST value_custom_dcl_AST = null;
		
		try {      // for error handling
			AST tmp54_AST = null;
			tmp54_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp54_AST);
			match(LITERAL_custom);
			value_dcl();
			astFactory.addASTChild(currentAST, returnAST);
			value_custom_dcl_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = value_custom_dcl_AST;
	}
	
	public final void value_forward_dcl() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST value_forward_dcl_AST = null;
		
		try {      // for error handling
			AST tmp55_AST = null;
			tmp55_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp55_AST);
			match(LITERAL_valuetype);
			identifier();
			astFactory.addASTChild(currentAST, returnAST);
			value_forward_dcl_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = value_forward_dcl_AST;
	}
	
	public final void type_spec() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST type_spec_AST = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case SCOPEOP:
			case IDENT:
			case LITERAL_float:
			case LITERAL_double:
			case LITERAL_long:
			case LITERAL_short:
			case LITERAL_unsigned:
			case LITERAL_char:
			case LITERAL_wchar:
			case LITERAL_boolean:
			case LITERAL_octet:
			case LITERAL_any:
			case LITERAL_Object:
			case LITERAL_sequence:
			case LITERAL_string:
			case LITERAL_wstring:
			case LITERAL_fixed:
			case LITERAL_ValueBase:
			{
				simple_type_spec();
				astFactory.addASTChild(currentAST, returnAST);
				type_spec_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_struct:
			case LITERAL_union:
			case LITERAL_enum:
			{
				constr_type_spec();
				astFactory.addASTChild(currentAST, returnAST);
				type_spec_AST = (AST)currentAST.root;
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_23);
			} else {
			  throw ex;
			}
		}
		returnAST = type_spec_AST;
	}
	
	public final void value_abs_full_dcl() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST value_abs_full_dcl_AST = null;
		
		try {      // for error handling
			value_inheritance_spec();
			astFactory.addASTChild(currentAST, returnAST);
			match(LCURLY);
			{
			_loop51:
			do {
				if ((_tokenSet_17.member(LA(1)))) {
					export();
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop51;
				}
				
			} while (true);
			}
			match(RCURLY);
			value_abs_full_dcl_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = value_abs_full_dcl_AST;
	}
	
	public final void value_inheritance_spec() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST value_inheritance_spec_AST = null;
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case COLON:
			{
				value_value_inheritance_spec();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case LCURLY:
			case LITERAL_supports:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			{
			switch ( LA(1)) {
			case LITERAL_supports:
			{
				value_interface_inheritance_spec();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case LCURLY:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			value_inheritance_spec_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_16);
			} else {
			  throw ex;
			}
		}
		returnAST = value_inheritance_spec_AST;
	}
	
	public final void value_header() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST value_header_AST = null;
		
		try {      // for error handling
			AST tmp58_AST = null;
			tmp58_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp58_AST);
			match(LITERAL_valuetype);
			identifier();
			astFactory.addASTChild(currentAST, returnAST);
			value_inheritance_spec();
			astFactory.addASTChild(currentAST, returnAST);
			value_header_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_16);
			} else {
			  throw ex;
			}
		}
		returnAST = value_header_AST;
	}
	
	public final void value_element() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST value_element_AST = null;
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case SCOPEOP:
			case IDENT:
			case LITERAL_const:
			case LITERAL_typedef:
			case LITERAL_native:
			case LITERAL_float:
			case LITERAL_double:
			case LITERAL_long:
			case LITERAL_short:
			case LITERAL_unsigned:
			case LITERAL_char:
			case LITERAL_wchar:
			case LITERAL_boolean:
			case LITERAL_octet:
			case LITERAL_any:
			case LITERAL_Object:
			case LITERAL_struct:
			case LITERAL_union:
			case LITERAL_enum:
			case LITERAL_string:
			case LITERAL_wstring:
			case LITERAL_exception:
			case LITERAL_oneway:
			case LITERAL_void:
			case LITERAL_ValueBase:
			case LITERAL_typeid:
			case LITERAL_typeprefix:
			case LITERAL_readonly:
			case LITERAL_attribute:
			{
				export();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case LITERAL_public:
			case LITERAL_private:
			{
				state_member();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case LITERAL_factory:
			{
				init_dcl();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			value_element_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_24);
			} else {
			  throw ex;
			}
		}
		returnAST = value_element_AST;
	}
	
	public final void value_value_inheritance_spec() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST value_value_inheritance_spec_AST = null;
		
		try {      // for error handling
			AST tmp59_AST = null;
			tmp59_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp59_AST);
			match(COLON);
			{
			switch ( LA(1)) {
			case LITERAL_truncatable:
			{
				AST tmp60_AST = null;
				tmp60_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp60_AST);
				match(LITERAL_truncatable);
				break;
			}
			case SCOPEOP:
			case IDENT:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			value_name();
			astFactory.addASTChild(currentAST, returnAST);
			{
			_loop63:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					value_name();
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop63;
				}
				
			} while (true);
			}
			value_value_inheritance_spec_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_25);
			} else {
			  throw ex;
			}
		}
		returnAST = value_value_inheritance_spec_AST;
	}
	
	public final void value_interface_inheritance_spec() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST value_interface_inheritance_spec_AST = null;
		
		try {      // for error handling
			AST tmp62_AST = null;
			tmp62_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp62_AST);
			match(LITERAL_supports);
			interface_name();
			astFactory.addASTChild(currentAST, returnAST);
			{
			_loop66:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					interface_name();
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop66;
				}
				
			} while (true);
			}
			value_interface_inheritance_spec_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_16);
			} else {
			  throw ex;
			}
		}
		returnAST = value_interface_inheritance_spec_AST;
	}
	
	public final void value_name() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST value_name_AST = null;
		
		try {      // for error handling
			scoped_name();
			astFactory.addASTChild(currentAST, returnAST);
			value_name_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_26);
			} else {
			  throw ex;
			}
		}
		returnAST = value_name_AST;
	}
	
	public final void state_member() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST state_member_AST = null;
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case LITERAL_public:
			{
				AST tmp64_AST = null;
				tmp64_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp64_AST);
				match(LITERAL_public);
				break;
			}
			case LITERAL_private:
			{
				AST tmp65_AST = null;
				tmp65_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp65_AST);
				match(LITERAL_private);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			type_spec();
			astFactory.addASTChild(currentAST, returnAST);
			declarators();
			astFactory.addASTChild(currentAST, returnAST);
			match(SEMI);
			state_member_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_24);
			} else {
			  throw ex;
			}
		}
		returnAST = state_member_AST;
	}
	
	public final void init_dcl() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST init_dcl_AST = null;
		
		try {      // for error handling
			AST tmp67_AST = null;
			tmp67_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp67_AST);
			match(LITERAL_factory);
			identifier();
			astFactory.addASTChild(currentAST, returnAST);
			match(LPAREN);
			{
			switch ( LA(1)) {
			case LITERAL_in:
			{
				init_param_decls();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case RPAREN:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(RPAREN);
			{
			switch ( LA(1)) {
			case LITERAL_raises:
			{
				raises_expr();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case SEMI:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(SEMI);
			init_dcl_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_24);
			} else {
			  throw ex;
			}
		}
		returnAST = init_dcl_AST;
	}
	
	public final void declarators() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST declarators_AST = null;
		
		try {      // for error handling
			declarator();
			astFactory.addASTChild(currentAST, returnAST);
			{
			_loop127:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					declarator();
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop127;
				}
				
			} while (true);
			}
			declarators_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = declarators_AST;
	}
	
	public final void init_param_decls() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST init_param_decls_AST = null;
		
		try {      // for error handling
			init_param_decl();
			astFactory.addASTChild(currentAST, returnAST);
			{
			_loop77:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					init_param_decl();
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop77;
				}
				
			} while (true);
			}
			init_param_decls_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_27);
			} else {
			  throw ex;
			}
		}
		returnAST = init_param_decls_AST;
	}
	
	public final void raises_expr() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST raises_expr_AST = null;
		
		try {      // for error handling
			AST tmp73_AST = null;
			tmp73_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp73_AST);
			match(LITERAL_raises);
			match(LPAREN);
			scoped_name_list();
			astFactory.addASTChild(currentAST, returnAST);
			match(RPAREN);
			raises_expr_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_28);
			} else {
			  throw ex;
			}
		}
		returnAST = raises_expr_AST;
	}
	
	public final void init_param_decl() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST init_param_decl_AST = null;
		
		try {      // for error handling
			init_param_attribute();
			astFactory.addASTChild(currentAST, returnAST);
			param_type_spec();
			astFactory.addASTChild(currentAST, returnAST);
			simple_declarator();
			astFactory.addASTChild(currentAST, returnAST);
			init_param_decl_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_29);
			} else {
			  throw ex;
			}
		}
		returnAST = init_param_decl_AST;
	}
	
	public final void init_param_attribute() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST init_param_attribute_AST = null;
		
		try {      // for error handling
			AST tmp76_AST = null;
			tmp76_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp76_AST);
			match(LITERAL_in);
			init_param_attribute_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_30);
			} else {
			  throw ex;
			}
		}
		returnAST = init_param_attribute_AST;
	}
	
	public final void param_type_spec() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST param_type_spec_AST = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case LITERAL_float:
			case LITERAL_double:
			case LITERAL_long:
			case LITERAL_short:
			case LITERAL_unsigned:
			case LITERAL_char:
			case LITERAL_wchar:
			case LITERAL_boolean:
			case LITERAL_octet:
			case LITERAL_any:
			case LITERAL_Object:
			case LITERAL_ValueBase:
			{
				base_type_spec();
				astFactory.addASTChild(currentAST, returnAST);
				param_type_spec_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_string:
			{
				string_type();
				astFactory.addASTChild(currentAST, returnAST);
				param_type_spec_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_wstring:
			{
				wide_string_type();
				astFactory.addASTChild(currentAST, returnAST);
				param_type_spec_AST = (AST)currentAST.root;
				break;
			}
			case SCOPEOP:
			case IDENT:
			{
				scoped_name();
				astFactory.addASTChild(currentAST, returnAST);
				param_type_spec_AST = (AST)currentAST.root;
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_31);
			} else {
			  throw ex;
			}
		}
		returnAST = param_type_spec_AST;
	}
	
	public final void simple_declarator() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST simple_declarator_AST = null;
		
		try {      // for error handling
			identifier();
			astFactory.addASTChild(currentAST, returnAST);
			simple_declarator_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_32);
			} else {
			  throw ex;
			}
		}
		returnAST = simple_declarator_AST;
	}
	
	public final void const_type() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST const_type_AST = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case LITERAL_char:
			{
				char_type();
				astFactory.addASTChild(currentAST, returnAST);
				const_type_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_wchar:
			{
				wide_char_type();
				astFactory.addASTChild(currentAST, returnAST);
				const_type_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_boolean:
			{
				boolean_type();
				astFactory.addASTChild(currentAST, returnAST);
				const_type_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_string:
			{
				string_type();
				astFactory.addASTChild(currentAST, returnAST);
				const_type_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_wstring:
			{
				wide_string_type();
				astFactory.addASTChild(currentAST, returnAST);
				const_type_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_fixed:
			{
				fixed_pt_const_type();
				astFactory.addASTChild(currentAST, returnAST);
				const_type_AST = (AST)currentAST.root;
				break;
			}
			case SCOPEOP:
			case IDENT:
			{
				scoped_name();
				astFactory.addASTChild(currentAST, returnAST);
				const_type_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_octet:
			{
				octet_type();
				astFactory.addASTChild(currentAST, returnAST);
				const_type_AST = (AST)currentAST.root;
				break;
			}
			default:
				boolean synPredMatched83 = false;
				if ((((LA(1) >= LITERAL_long && LA(1) <= LITERAL_unsigned)) && (LA(2)==IDENT||LA(2)==LITERAL_long||LA(2)==LITERAL_short) && (LA(3)==IDENT||LA(3)==ASSIGN||LA(3)==LITERAL_long) && (_tokenSet_33.member(LA(4))))) {
					int _m83 = mark();
					synPredMatched83 = true;
					inputState.guessing++;
					try {
						{
						integer_type();
						}
					}
					catch (RecognitionException pe) {
						synPredMatched83 = false;
					}
					rewind(_m83);
					inputState.guessing--;
				}
				if ( synPredMatched83 ) {
					integer_type();
					astFactory.addASTChild(currentAST, returnAST);
					const_type_AST = (AST)currentAST.root;
				}
				else if (((LA(1) >= LITERAL_float && LA(1) <= LITERAL_long)) && (LA(2)==IDENT||LA(2)==LITERAL_double) && (LA(3)==IDENT||LA(3)==ASSIGN) && (_tokenSet_33.member(LA(4)))) {
					floating_pt_type();
					astFactory.addASTChild(currentAST, returnAST);
					const_type_AST = (AST)currentAST.root;
				}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_31);
			} else {
			  throw ex;
			}
		}
		returnAST = const_type_AST;
	}
	
	public final void const_exp() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST const_exp_AST = null;
		
		try {      // for error handling
			or_expr();
			astFactory.addASTChild(currentAST, returnAST);
			const_exp_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_34);
			} else {
			  throw ex;
			}
		}
		returnAST = const_exp_AST;
	}
	
	public final void integer_type() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST integer_type_AST = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case LITERAL_long:
			case LITERAL_short:
			{
				signed_int();
				astFactory.addASTChild(currentAST, returnAST);
				integer_type_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_unsigned:
			{
				unsigned_int();
				astFactory.addASTChild(currentAST, returnAST);
				integer_type_AST = (AST)currentAST.root;
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_35);
			} else {
			  throw ex;
			}
		}
		returnAST = integer_type_AST;
	}
	
	public final void char_type() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST char_type_AST = null;
		
		try {      // for error handling
			AST tmp77_AST = null;
			tmp77_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp77_AST);
			match(LITERAL_char);
			char_type_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_35);
			} else {
			  throw ex;
			}
		}
		returnAST = char_type_AST;
	}
	
	public final void wide_char_type() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST wide_char_type_AST = null;
		
		try {      // for error handling
			AST tmp78_AST = null;
			tmp78_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp78_AST);
			match(LITERAL_wchar);
			wide_char_type_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_36);
			} else {
			  throw ex;
			}
		}
		returnAST = wide_char_type_AST;
	}
	
	public final void boolean_type() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST boolean_type_AST = null;
		
		try {      // for error handling
			AST tmp79_AST = null;
			tmp79_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp79_AST);
			match(LITERAL_boolean);
			boolean_type_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_35);
			} else {
			  throw ex;
			}
		}
		returnAST = boolean_type_AST;
	}
	
	public final void floating_pt_type() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST floating_pt_type_AST = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case LITERAL_float:
			{
				AST tmp80_AST = null;
				tmp80_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp80_AST);
				match(LITERAL_float);
				floating_pt_type_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_double:
			{
				AST tmp81_AST = null;
				tmp81_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp81_AST);
				match(LITERAL_double);
				floating_pt_type_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_long:
			{
				AST tmp82_AST = null;
				tmp82_AST = astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp82_AST);
				match(LITERAL_long);
				AST tmp83_AST = null;
				tmp83_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp83_AST);
				match(LITERAL_double);
				floating_pt_type_AST = (AST)currentAST.root;
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_36);
			} else {
			  throw ex;
			}
		}
		returnAST = floating_pt_type_AST;
	}
	
	public final void string_type() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST string_type_AST = null;
		
		try {      // for error handling
			AST tmp84_AST = null;
			tmp84_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp84_AST);
			match(LITERAL_string);
			{
			switch ( LA(1)) {
			case LT:
			{
				match(LT);
				positive_int_const();
				astFactory.addASTChild(currentAST, returnAST);
				match(GT);
				break;
			}
			case SEMI:
			case COMMA:
			case IDENT:
			case GT:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			string_type_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_36);
			} else {
			  throw ex;
			}
		}
		returnAST = string_type_AST;
	}
	
	public final void wide_string_type() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST wide_string_type_AST = null;
		
		try {      // for error handling
			AST tmp87_AST = null;
			tmp87_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp87_AST);
			match(LITERAL_wstring);
			{
			switch ( LA(1)) {
			case LT:
			{
				match(LT);
				positive_int_const();
				astFactory.addASTChild(currentAST, returnAST);
				match(GT);
				break;
			}
			case SEMI:
			case COMMA:
			case IDENT:
			case GT:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			wide_string_type_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_36);
			} else {
			  throw ex;
			}
		}
		returnAST = wide_string_type_AST;
	}
	
	public final void fixed_pt_const_type() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST fixed_pt_const_type_AST = null;
		
		try {      // for error handling
			AST tmp90_AST = null;
			tmp90_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp90_AST);
			match(LITERAL_fixed);
			fixed_pt_const_type_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_31);
			} else {
			  throw ex;
			}
		}
		returnAST = fixed_pt_const_type_AST;
	}
	
	public final void octet_type() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST octet_type_AST = null;
		
		try {      // for error handling
			AST tmp91_AST = null;
			tmp91_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp91_AST);
			match(LITERAL_octet);
			octet_type_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_36);
			} else {
			  throw ex;
			}
		}
		returnAST = octet_type_AST;
	}
	
	public final void or_expr() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST or_expr_AST = null;
		
		try {      // for error handling
			xor_expr();
			astFactory.addASTChild(currentAST, returnAST);
			{
			_loop87:
			do {
				if ((LA(1)==OR)) {
					AST tmp92_AST = null;
					tmp92_AST = astFactory.create(LT(1));
					astFactory.makeASTRoot(currentAST, tmp92_AST);
					match(OR);
					xor_expr();
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop87;
				}
				
			} while (true);
			}
			or_expr_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_34);
			} else {
			  throw ex;
			}
		}
		returnAST = or_expr_AST;
	}
	
	public final void xor_expr() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST xor_expr_AST = null;
		
		try {      // for error handling
			and_expr();
			astFactory.addASTChild(currentAST, returnAST);
			{
			_loop90:
			do {
				if ((LA(1)==XOR)) {
					AST tmp93_AST = null;
					tmp93_AST = astFactory.create(LT(1));
					astFactory.makeASTRoot(currentAST, tmp93_AST);
					match(XOR);
					and_expr();
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop90;
				}
				
			} while (true);
			}
			xor_expr_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_37);
			} else {
			  throw ex;
			}
		}
		returnAST = xor_expr_AST;
	}
	
	public final void and_expr() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST and_expr_AST = null;
		
		try {      // for error handling
			shift_expr();
			astFactory.addASTChild(currentAST, returnAST);
			{
			_loop93:
			do {
				if ((LA(1)==AND)) {
					AST tmp94_AST = null;
					tmp94_AST = astFactory.create(LT(1));
					astFactory.makeASTRoot(currentAST, tmp94_AST);
					match(AND);
					shift_expr();
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop93;
				}
				
			} while (true);
			}
			and_expr_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_38);
			} else {
			  throw ex;
			}
		}
		returnAST = and_expr_AST;
	}
	
	public final void shift_expr() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST shift_expr_AST = null;
		
		try {      // for error handling
			add_expr();
			astFactory.addASTChild(currentAST, returnAST);
			{
			_loop97:
			do {
				if ((LA(1)==LSHIFT||LA(1)==RSHIFT)) {
					{
					switch ( LA(1)) {
					case LSHIFT:
					{
						AST tmp95_AST = null;
						tmp95_AST = astFactory.create(LT(1));
						astFactory.makeASTRoot(currentAST, tmp95_AST);
						match(LSHIFT);
						break;
					}
					case RSHIFT:
					{
						AST tmp96_AST = null;
						tmp96_AST = astFactory.create(LT(1));
						astFactory.makeASTRoot(currentAST, tmp96_AST);
						match(RSHIFT);
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					add_expr();
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop97;
				}
				
			} while (true);
			}
			shift_expr_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_39);
			} else {
			  throw ex;
			}
		}
		returnAST = shift_expr_AST;
	}
	
	public final void add_expr() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST add_expr_AST = null;
		
		try {      // for error handling
			mult_expr();
			astFactory.addASTChild(currentAST, returnAST);
			{
			_loop101:
			do {
				if ((LA(1)==PLUS||LA(1)==MINUS)) {
					{
					switch ( LA(1)) {
					case PLUS:
					{
						AST tmp97_AST = null;
						tmp97_AST = astFactory.create(LT(1));
						astFactory.makeASTRoot(currentAST, tmp97_AST);
						match(PLUS);
						break;
					}
					case MINUS:
					{
						AST tmp98_AST = null;
						tmp98_AST = astFactory.create(LT(1));
						astFactory.makeASTRoot(currentAST, tmp98_AST);
						match(MINUS);
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					mult_expr();
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop101;
				}
				
			} while (true);
			}
			add_expr_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_40);
			} else {
			  throw ex;
			}
		}
		returnAST = add_expr_AST;
	}
	
	public final void mult_expr() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST mult_expr_AST = null;
		
		try {      // for error handling
			unary_expr();
			astFactory.addASTChild(currentAST, returnAST);
			{
			_loop105:
			do {
				if (((LA(1) >= STAR && LA(1) <= MOD))) {
					{
					switch ( LA(1)) {
					case STAR:
					{
						AST tmp99_AST = null;
						tmp99_AST = astFactory.create(LT(1));
						astFactory.makeASTRoot(currentAST, tmp99_AST);
						match(STAR);
						break;
					}
					case DIV:
					{
						AST tmp100_AST = null;
						tmp100_AST = astFactory.create(LT(1));
						astFactory.makeASTRoot(currentAST, tmp100_AST);
						match(DIV);
						break;
					}
					case MOD:
					{
						AST tmp101_AST = null;
						tmp101_AST = astFactory.create(LT(1));
						astFactory.makeASTRoot(currentAST, tmp101_AST);
						match(MOD);
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					unary_expr();
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop105;
				}
				
			} while (true);
			}
			mult_expr_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_41);
			} else {
			  throw ex;
			}
		}
		returnAST = mult_expr_AST;
	}
	
	public final void unary_expr() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST unary_expr_AST = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case PLUS:
			case MINUS:
			case TILDE:
			{
				{
				switch ( LA(1)) {
				case MINUS:
				{
					AST tmp102_AST = null;
					tmp102_AST = astFactory.create(LT(1));
					astFactory.makeASTRoot(currentAST, tmp102_AST);
					match(MINUS);
					break;
				}
				case PLUS:
				{
					AST tmp103_AST = null;
					tmp103_AST = astFactory.create(LT(1));
					astFactory.makeASTRoot(currentAST, tmp103_AST);
					match(PLUS);
					break;
				}
				case TILDE:
				{
					AST tmp104_AST = null;
					tmp104_AST = astFactory.create(LT(1));
					astFactory.makeASTRoot(currentAST, tmp104_AST);
					match(TILDE);
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				primary_expr();
				astFactory.addASTChild(currentAST, returnAST);
				unary_expr_AST = (AST)currentAST.root;
				break;
			}
			case SCOPEOP:
			case IDENT:
			case LPAREN:
			case LITERAL_TRUE:
			case LITERAL_FALSE:
			case INT:
			case OCTAL:
			case HEX:
			case STRING_LITERAL:
			case WIDE_STRING_LITERAL:
			case CHAR_LITERAL:
			case WIDE_CHAR_LITERAL:
			case FIXED:
			case FLOAT:
			{
				primary_expr();
				astFactory.addASTChild(currentAST, returnAST);
				unary_expr_AST = (AST)currentAST.root;
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_42);
			} else {
			  throw ex;
			}
		}
		returnAST = unary_expr_AST;
	}
	
	public final void primary_expr() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST primary_expr_AST = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case SCOPEOP:
			case IDENT:
			{
				scoped_name();
				astFactory.addASTChild(currentAST, returnAST);
				primary_expr_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_TRUE:
			case LITERAL_FALSE:
			case INT:
			case OCTAL:
			case HEX:
			case STRING_LITERAL:
			case WIDE_STRING_LITERAL:
			case CHAR_LITERAL:
			case WIDE_CHAR_LITERAL:
			case FIXED:
			case FLOAT:
			{
				literal();
				astFactory.addASTChild(currentAST, returnAST);
				primary_expr_AST = (AST)currentAST.root;
				break;
			}
			case LPAREN:
			{
				AST tmp105_AST = null;
				tmp105_AST = astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp105_AST);
				match(LPAREN);
				const_exp();
				astFactory.addASTChild(currentAST, returnAST);
				AST tmp106_AST = null;
				tmp106_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp106_AST);
				match(RPAREN);
				primary_expr_AST = (AST)currentAST.root;
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_42);
			} else {
			  throw ex;
			}
		}
		returnAST = primary_expr_AST;
	}
	
	public final void literal() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST literal_AST = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case INT:
			case OCTAL:
			case HEX:
			{
				integer_literal();
				astFactory.addASTChild(currentAST, returnAST);
				literal_AST = (AST)currentAST.root;
				break;
			}
			case STRING_LITERAL:
			{
				string_literal();
				astFactory.addASTChild(currentAST, returnAST);
				literal_AST = (AST)currentAST.root;
				break;
			}
			case WIDE_STRING_LITERAL:
			{
				wide_string_literal();
				astFactory.addASTChild(currentAST, returnAST);
				literal_AST = (AST)currentAST.root;
				break;
			}
			case CHAR_LITERAL:
			{
				character_literal();
				astFactory.addASTChild(currentAST, returnAST);
				literal_AST = (AST)currentAST.root;
				break;
			}
			case WIDE_CHAR_LITERAL:
			{
				wide_character_literal();
				astFactory.addASTChild(currentAST, returnAST);
				literal_AST = (AST)currentAST.root;
				break;
			}
			case FIXED:
			{
				fixed_pt_literal();
				astFactory.addASTChild(currentAST, returnAST);
				literal_AST = (AST)currentAST.root;
				break;
			}
			case FLOAT:
			{
				floating_pt_literal();
				astFactory.addASTChild(currentAST, returnAST);
				literal_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_TRUE:
			case LITERAL_FALSE:
			{
				boolean_literal();
				astFactory.addASTChild(currentAST, returnAST);
				literal_AST = (AST)currentAST.root;
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_42);
			} else {
			  throw ex;
			}
		}
		returnAST = literal_AST;
	}
	
	public final void integer_literal() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST integer_literal_AST = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case INT:
			{
				AST tmp107_AST = null;
				tmp107_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp107_AST);
				match(INT);
				integer_literal_AST = (AST)currentAST.root;
				break;
			}
			case OCTAL:
			{
				AST tmp108_AST = null;
				tmp108_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp108_AST);
				match(OCTAL);
				integer_literal_AST = (AST)currentAST.root;
				break;
			}
			case HEX:
			{
				AST tmp109_AST = null;
				tmp109_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp109_AST);
				match(HEX);
				integer_literal_AST = (AST)currentAST.root;
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_42);
			} else {
			  throw ex;
			}
		}
		returnAST = integer_literal_AST;
	}
	
	public final void string_literal() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST string_literal_AST = null;
		
		try {      // for error handling
			{
			int _cnt285=0;
			_loop285:
			do {
				if ((LA(1)==STRING_LITERAL)) {
					AST tmp110_AST = null;
					tmp110_AST = astFactory.create(LT(1));
					astFactory.addASTChild(currentAST, tmp110_AST);
					match(STRING_LITERAL);
				}
				else {
					if ( _cnt285>=1 ) { break _loop285; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt285++;
			} while (true);
			}
			string_literal_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_42);
			} else {
			  throw ex;
			}
		}
		returnAST = string_literal_AST;
	}
	
	public final void wide_string_literal() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST wide_string_literal_AST = null;
		
		try {      // for error handling
			{
			int _cnt288=0;
			_loop288:
			do {
				if ((LA(1)==WIDE_STRING_LITERAL)) {
					AST tmp111_AST = null;
					tmp111_AST = astFactory.create(LT(1));
					astFactory.addASTChild(currentAST, tmp111_AST);
					match(WIDE_STRING_LITERAL);
				}
				else {
					if ( _cnt288>=1 ) { break _loop288; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt288++;
			} while (true);
			}
			wide_string_literal_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_42);
			} else {
			  throw ex;
			}
		}
		returnAST = wide_string_literal_AST;
	}
	
	public final void character_literal() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST character_literal_AST = null;
		
		try {      // for error handling
			AST tmp112_AST = null;
			tmp112_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp112_AST);
			match(CHAR_LITERAL);
			character_literal_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_42);
			} else {
			  throw ex;
			}
		}
		returnAST = character_literal_AST;
	}
	
	public final void wide_character_literal() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST wide_character_literal_AST = null;
		
		try {      // for error handling
			AST tmp113_AST = null;
			tmp113_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp113_AST);
			match(WIDE_CHAR_LITERAL);
			wide_character_literal_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_42);
			} else {
			  throw ex;
			}
		}
		returnAST = wide_character_literal_AST;
	}
	
	public final void fixed_pt_literal() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST fixed_pt_literal_AST = null;
		
		try {      // for error handling
			AST tmp114_AST = null;
			tmp114_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp114_AST);
			match(FIXED);
			fixed_pt_literal_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_42);
			} else {
			  throw ex;
			}
		}
		returnAST = fixed_pt_literal_AST;
	}
	
	public final void floating_pt_literal() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST floating_pt_literal_AST = null;
		Token  f = null;
		AST f_AST = null;
		
		try {      // for error handling
			f = LT(1);
			f_AST = astFactory.create(f);
			astFactory.addASTChild(currentAST, f_AST);
			match(FLOAT);
			floating_pt_literal_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_42);
			} else {
			  throw ex;
			}
		}
		returnAST = floating_pt_literal_AST;
	}
	
	public final void boolean_literal() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST boolean_literal_AST = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case LITERAL_TRUE:
			{
				AST tmp115_AST = null;
				tmp115_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp115_AST);
				match(LITERAL_TRUE);
				boolean_literal_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_FALSE:
			{
				AST tmp116_AST = null;
				tmp116_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp116_AST);
				match(LITERAL_FALSE);
				boolean_literal_AST = (AST)currentAST.root;
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_42);
			} else {
			  throw ex;
			}
		}
		returnAST = boolean_literal_AST;
	}
	
	public final void positive_int_const() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST positive_int_const_AST = null;
		
		try {      // for error handling
			const_exp();
			astFactory.addASTChild(currentAST, returnAST);
			positive_int_const_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_43);
			} else {
			  throw ex;
			}
		}
		returnAST = positive_int_const_AST;
	}
	
	public final void type_declarator() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST type_declarator_AST = null;
		
		try {      // for error handling
			type_spec();
			astFactory.addASTChild(currentAST, returnAST);
			declarators();
			astFactory.addASTChild(currentAST, returnAST);
			type_declarator_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = type_declarator_AST;
	}
	
	public final void struct_type() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST struct_type_AST = null;
		
		try {      // for error handling
			AST tmp117_AST = null;
			tmp117_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp117_AST);
			match(LITERAL_struct);
			identifier();
			astFactory.addASTChild(currentAST, returnAST);
			match(LCURLY);
			member_list();
			astFactory.addASTChild(currentAST, returnAST);
			match(RCURLY);
			struct_type_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_23);
			} else {
			  throw ex;
			}
		}
		returnAST = struct_type_AST;
	}
	
	public final void union_type() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST union_type_AST = null;
		
		try {      // for error handling
			AST tmp120_AST = null;
			tmp120_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp120_AST);
			match(LITERAL_union);
			identifier();
			astFactory.addASTChild(currentAST, returnAST);
			match(LITERAL_switch);
			match(LPAREN);
			switch_type_spec();
			astFactory.addASTChild(currentAST, returnAST);
			match(RPAREN);
			match(LCURLY);
			switch_body();
			astFactory.addASTChild(currentAST, returnAST);
			match(RCURLY);
			union_type_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_23);
			} else {
			  throw ex;
			}
		}
		returnAST = union_type_AST;
	}
	
	public final void enum_type() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST enum_type_AST = null;
		
		try {      // for error handling
			AST tmp126_AST = null;
			tmp126_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp126_AST);
			match(LITERAL_enum);
			identifier();
			astFactory.addASTChild(currentAST, returnAST);
			match(LCURLY);
			enumerator_list();
			astFactory.addASTChild(currentAST, returnAST);
			match(RCURLY);
			enum_type_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_44);
			} else {
			  throw ex;
			}
		}
		returnAST = enum_type_AST;
	}
	
	public final void constr_forward_decl() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST constr_forward_decl_AST = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case LITERAL_struct:
			{
				AST tmp129_AST = null;
				tmp129_AST = astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp129_AST);
				match(LITERAL_struct);
				identifier();
				astFactory.addASTChild(currentAST, returnAST);
				constr_forward_decl_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_union:
			{
				AST tmp130_AST = null;
				tmp130_AST = astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp130_AST);
				match(LITERAL_union);
				identifier();
				astFactory.addASTChild(currentAST, returnAST);
				constr_forward_decl_AST = (AST)currentAST.root;
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = constr_forward_decl_AST;
	}
	
	public final void simple_type_spec() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST simple_type_spec_AST = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case LITERAL_float:
			case LITERAL_double:
			case LITERAL_long:
			case LITERAL_short:
			case LITERAL_unsigned:
			case LITERAL_char:
			case LITERAL_wchar:
			case LITERAL_boolean:
			case LITERAL_octet:
			case LITERAL_any:
			case LITERAL_Object:
			case LITERAL_ValueBase:
			{
				base_type_spec();
				astFactory.addASTChild(currentAST, returnAST);
				simple_type_spec_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_sequence:
			case LITERAL_string:
			case LITERAL_wstring:
			case LITERAL_fixed:
			{
				template_type_spec();
				astFactory.addASTChild(currentAST, returnAST);
				simple_type_spec_AST = (AST)currentAST.root;
				break;
			}
			case SCOPEOP:
			case IDENT:
			{
				scoped_name();
				astFactory.addASTChild(currentAST, returnAST);
				simple_type_spec_AST = (AST)currentAST.root;
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_36);
			} else {
			  throw ex;
			}
		}
		returnAST = simple_type_spec_AST;
	}
	
	public final void constr_type_spec() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST constr_type_spec_AST = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case LITERAL_struct:
			{
				struct_type();
				astFactory.addASTChild(currentAST, returnAST);
				constr_type_spec_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_union:
			{
				union_type();
				astFactory.addASTChild(currentAST, returnAST);
				constr_type_spec_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_enum:
			{
				enum_type();
				astFactory.addASTChild(currentAST, returnAST);
				constr_type_spec_AST = (AST)currentAST.root;
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_23);
			} else {
			  throw ex;
			}
		}
		returnAST = constr_type_spec_AST;
	}
	
	public final void base_type_spec() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST base_type_spec_AST = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case LITERAL_char:
			{
				char_type();
				astFactory.addASTChild(currentAST, returnAST);
				base_type_spec_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_wchar:
			{
				wide_char_type();
				astFactory.addASTChild(currentAST, returnAST);
				base_type_spec_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_boolean:
			{
				boolean_type();
				astFactory.addASTChild(currentAST, returnAST);
				base_type_spec_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_octet:
			{
				octet_type();
				astFactory.addASTChild(currentAST, returnAST);
				base_type_spec_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_any:
			{
				any_type();
				astFactory.addASTChild(currentAST, returnAST);
				base_type_spec_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_Object:
			{
				object_type();
				astFactory.addASTChild(currentAST, returnAST);
				base_type_spec_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_ValueBase:
			{
				value_base_type();
				astFactory.addASTChild(currentAST, returnAST);
				base_type_spec_AST = (AST)currentAST.root;
				break;
			}
			default:
				boolean synPredMatched122 = false;
				if ((((LA(1) >= LITERAL_float && LA(1) <= LITERAL_long)) && (_tokenSet_45.member(LA(2))) && (_tokenSet_46.member(LA(3))) && (_tokenSet_47.member(LA(4))))) {
					int _m122 = mark();
					synPredMatched122 = true;
					inputState.guessing++;
					try {
						{
						floating_pt_type();
						}
					}
					catch (RecognitionException pe) {
						synPredMatched122 = false;
					}
					rewind(_m122);
					inputState.guessing--;
				}
				if ( synPredMatched122 ) {
					floating_pt_type();
					astFactory.addASTChild(currentAST, returnAST);
					base_type_spec_AST = (AST)currentAST.root;
				}
				else if (((LA(1) >= LITERAL_long && LA(1) <= LITERAL_unsigned)) && (_tokenSet_48.member(LA(2))) && (_tokenSet_49.member(LA(3))) && (_tokenSet_47.member(LA(4)))) {
					integer_type();
					astFactory.addASTChild(currentAST, returnAST);
					base_type_spec_AST = (AST)currentAST.root;
				}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_36);
			} else {
			  throw ex;
			}
		}
		returnAST = base_type_spec_AST;
	}
	
	public final void template_type_spec() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST template_type_spec_AST = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case LITERAL_sequence:
			{
				sequence_type();
				astFactory.addASTChild(currentAST, returnAST);
				template_type_spec_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_string:
			{
				string_type();
				astFactory.addASTChild(currentAST, returnAST);
				template_type_spec_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_wstring:
			{
				wide_string_type();
				astFactory.addASTChild(currentAST, returnAST);
				template_type_spec_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_fixed:
			{
				fixed_pt_type();
				astFactory.addASTChild(currentAST, returnAST);
				template_type_spec_AST = (AST)currentAST.root;
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_36);
			} else {
			  throw ex;
			}
		}
		returnAST = template_type_spec_AST;
	}
	
	public final void any_type() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST any_type_AST = null;
		
		try {      // for error handling
			AST tmp131_AST = null;
			tmp131_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp131_AST);
			match(LITERAL_any);
			any_type_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_36);
			} else {
			  throw ex;
			}
		}
		returnAST = any_type_AST;
	}
	
	public final void object_type() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST object_type_AST = null;
		
		try {      // for error handling
			AST tmp132_AST = null;
			tmp132_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp132_AST);
			match(LITERAL_Object);
			object_type_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_36);
			} else {
			  throw ex;
			}
		}
		returnAST = object_type_AST;
	}
	
	public final void value_base_type() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST value_base_type_AST = null;
		
		try {      // for error handling
			AST tmp133_AST = null;
			tmp133_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp133_AST);
			match(LITERAL_ValueBase);
			value_base_type_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_36);
			} else {
			  throw ex;
			}
		}
		returnAST = value_base_type_AST;
	}
	
	public final void sequence_type() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST sequence_type_AST = null;
		
		try {      // for error handling
			AST tmp134_AST = null;
			tmp134_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp134_AST);
			match(LITERAL_sequence);
			match(LT);
			simple_type_spec();
			astFactory.addASTChild(currentAST, returnAST);
			opt_pos_int();
			astFactory.addASTChild(currentAST, returnAST);
			match(GT);
			sequence_type_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_36);
			} else {
			  throw ex;
			}
		}
		returnAST = sequence_type_AST;
	}
	
	public final void fixed_pt_type() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST fixed_pt_type_AST = null;
		
		try {      // for error handling
			AST tmp137_AST = null;
			tmp137_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp137_AST);
			match(LITERAL_fixed);
			match(LT);
			positive_int_const();
			astFactory.addASTChild(currentAST, returnAST);
			match(COMMA);
			positive_int_const();
			astFactory.addASTChild(currentAST, returnAST);
			match(GT);
			fixed_pt_type_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_36);
			} else {
			  throw ex;
			}
		}
		returnAST = fixed_pt_type_AST;
	}
	
	public final void declarator() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST declarator_AST = null;
		
		try {      // for error handling
			if ((LA(1)==IDENT) && (LA(2)==SEMI||LA(2)==COMMA)) {
				simple_declarator();
				astFactory.addASTChild(currentAST, returnAST);
				declarator_AST = (AST)currentAST.root;
			}
			else if ((LA(1)==IDENT) && (LA(2)==LBRACK)) {
				complex_declarator();
				astFactory.addASTChild(currentAST, returnAST);
				declarator_AST = (AST)currentAST.root;
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_50);
			} else {
			  throw ex;
			}
		}
		returnAST = declarator_AST;
	}
	
	public final void complex_declarator() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST complex_declarator_AST = null;
		
		try {      // for error handling
			array_declarator();
			astFactory.addASTChild(currentAST, returnAST);
			complex_declarator_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_50);
			} else {
			  throw ex;
			}
		}
		returnAST = complex_declarator_AST;
	}
	
	public final void array_declarator() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST array_declarator_AST = null;
		
		try {      // for error handling
			AST tmp141_AST = null;
			tmp141_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp141_AST);
			match(IDENT);
			{
			int _cnt176=0;
			_loop176:
			do {
				if ((LA(1)==LBRACK)) {
					fixed_array_size();
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					if ( _cnt176>=1 ) { break _loop176; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt176++;
			} while (true);
			}
			array_declarator_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_50);
			} else {
			  throw ex;
			}
		}
		returnAST = array_declarator_AST;
	}
	
	public final void signed_int() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST signed_int_AST = null;
		
		try {      // for error handling
			if ((LA(1)==LITERAL_short)) {
				signed_short_int();
				astFactory.addASTChild(currentAST, returnAST);
				signed_int_AST = (AST)currentAST.root;
			}
			else if ((LA(1)==LITERAL_long) && (_tokenSet_35.member(LA(2)))) {
				signed_long_int();
				astFactory.addASTChild(currentAST, returnAST);
				signed_int_AST = (AST)currentAST.root;
			}
			else if ((LA(1)==LITERAL_long) && (LA(2)==LITERAL_long)) {
				signed_longlong_int();
				astFactory.addASTChild(currentAST, returnAST);
				signed_int_AST = (AST)currentAST.root;
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_35);
			} else {
			  throw ex;
			}
		}
		returnAST = signed_int_AST;
	}
	
	public final void unsigned_int() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST unsigned_int_AST = null;
		
		try {      // for error handling
			if ((LA(1)==LITERAL_unsigned) && (LA(2)==LITERAL_short)) {
				unsigned_short_int();
				astFactory.addASTChild(currentAST, returnAST);
				unsigned_int_AST = (AST)currentAST.root;
			}
			else if ((LA(1)==LITERAL_unsigned) && (LA(2)==LITERAL_long) && (_tokenSet_35.member(LA(3)))) {
				unsigned_long_int();
				astFactory.addASTChild(currentAST, returnAST);
				unsigned_int_AST = (AST)currentAST.root;
			}
			else if ((LA(1)==LITERAL_unsigned) && (LA(2)==LITERAL_long) && (LA(3)==LITERAL_long)) {
				unsigned_longlong_int();
				astFactory.addASTChild(currentAST, returnAST);
				unsigned_int_AST = (AST)currentAST.root;
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_35);
			} else {
			  throw ex;
			}
		}
		returnAST = unsigned_int_AST;
	}
	
	public final void signed_short_int() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST signed_short_int_AST = null;
		
		try {      // for error handling
			AST tmp142_AST = null;
			tmp142_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp142_AST);
			match(LITERAL_short);
			signed_short_int_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_35);
			} else {
			  throw ex;
			}
		}
		returnAST = signed_short_int_AST;
	}
	
	public final void signed_long_int() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST signed_long_int_AST = null;
		
		try {      // for error handling
			AST tmp143_AST = null;
			tmp143_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp143_AST);
			match(LITERAL_long);
			signed_long_int_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_35);
			} else {
			  throw ex;
			}
		}
		returnAST = signed_long_int_AST;
	}
	
	public final void signed_longlong_int() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST signed_longlong_int_AST = null;
		
		try {      // for error handling
			AST tmp144_AST = null;
			tmp144_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp144_AST);
			match(LITERAL_long);
			AST tmp145_AST = null;
			tmp145_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp145_AST);
			match(LITERAL_long);
			signed_longlong_int_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_35);
			} else {
			  throw ex;
			}
		}
		returnAST = signed_longlong_int_AST;
	}
	
	public final void unsigned_short_int() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST unsigned_short_int_AST = null;
		
		try {      // for error handling
			AST tmp146_AST = null;
			tmp146_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp146_AST);
			match(LITERAL_unsigned);
			AST tmp147_AST = null;
			tmp147_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp147_AST);
			match(LITERAL_short);
			unsigned_short_int_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_35);
			} else {
			  throw ex;
			}
		}
		returnAST = unsigned_short_int_AST;
	}
	
	public final void unsigned_long_int() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST unsigned_long_int_AST = null;
		
		try {      // for error handling
			AST tmp148_AST = null;
			tmp148_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp148_AST);
			match(LITERAL_unsigned);
			AST tmp149_AST = null;
			tmp149_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp149_AST);
			match(LITERAL_long);
			unsigned_long_int_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_35);
			} else {
			  throw ex;
			}
		}
		returnAST = unsigned_long_int_AST;
	}
	
	public final void unsigned_longlong_int() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST unsigned_longlong_int_AST = null;
		
		try {      // for error handling
			AST tmp150_AST = null;
			tmp150_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp150_AST);
			match(LITERAL_unsigned);
			AST tmp151_AST = null;
			tmp151_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp151_AST);
			match(LITERAL_long);
			AST tmp152_AST = null;
			tmp152_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp152_AST);
			match(LITERAL_long);
			unsigned_longlong_int_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_35);
			} else {
			  throw ex;
			}
		}
		returnAST = unsigned_longlong_int_AST;
	}
	
	public final void member_list() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST member_list_AST = null;
		
		try {      // for error handling
			{
			int _cnt150=0;
			_loop150:
			do {
				if ((_tokenSet_13.member(LA(1)))) {
					member();
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					if ( _cnt150>=1 ) { break _loop150; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt150++;
			} while (true);
			}
			member_list_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_15);
			} else {
			  throw ex;
			}
		}
		returnAST = member_list_AST;
	}
	
	public final void member() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST member_AST = null;
		
		try {      // for error handling
			type_spec();
			astFactory.addASTChild(currentAST, returnAST);
			declarators();
			astFactory.addASTChild(currentAST, returnAST);
			match(SEMI);
			member_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_51);
			} else {
			  throw ex;
			}
		}
		returnAST = member_AST;
	}
	
	public final void switch_type_spec() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST switch_type_spec_AST = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case LITERAL_long:
			case LITERAL_short:
			case LITERAL_unsigned:
			{
				integer_type();
				astFactory.addASTChild(currentAST, returnAST);
				switch_type_spec_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_char:
			{
				char_type();
				astFactory.addASTChild(currentAST, returnAST);
				switch_type_spec_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_boolean:
			{
				boolean_type();
				astFactory.addASTChild(currentAST, returnAST);
				switch_type_spec_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_enum:
			{
				enum_type();
				astFactory.addASTChild(currentAST, returnAST);
				switch_type_spec_AST = (AST)currentAST.root;
				break;
			}
			case SCOPEOP:
			case IDENT:
			{
				scoped_name();
				astFactory.addASTChild(currentAST, returnAST);
				switch_type_spec_AST = (AST)currentAST.root;
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_27);
			} else {
			  throw ex;
			}
		}
		returnAST = switch_type_spec_AST;
	}
	
	public final void switch_body() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST switch_body_AST = null;
		
		try {      // for error handling
			case_stmt_list();
			astFactory.addASTChild(currentAST, returnAST);
			switch_body_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_15);
			} else {
			  throw ex;
			}
		}
		returnAST = switch_body_AST;
	}
	
	public final void case_stmt_list() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST case_stmt_list_AST = null;
		
		try {      // for error handling
			{
			int _cnt157=0;
			_loop157:
			do {
				if ((LA(1)==LITERAL_case||LA(1)==LITERAL_default)) {
					case_stmt();
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					if ( _cnt157>=1 ) { break _loop157; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt157++;
			} while (true);
			}
			case_stmt_list_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_15);
			} else {
			  throw ex;
			}
		}
		returnAST = case_stmt_list_AST;
	}
	
	public final void case_stmt() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST case_stmt_AST = null;
		
		try {      // for error handling
			{
			int _cnt160=0;
			_loop160:
			do {
				switch ( LA(1)) {
				case LITERAL_case:
				{
					AST tmp154_AST = null;
					tmp154_AST = astFactory.create(LT(1));
					astFactory.makeASTRoot(currentAST, tmp154_AST);
					match(LITERAL_case);
					const_exp();
					astFactory.addASTChild(currentAST, returnAST);
					match(COLON);
					break;
				}
				case LITERAL_default:
				{
					AST tmp156_AST = null;
					tmp156_AST = astFactory.create(LT(1));
					astFactory.makeASTRoot(currentAST, tmp156_AST);
					match(LITERAL_default);
					match(COLON);
					break;
				}
				default:
				{
					if ( _cnt160>=1 ) { break _loop160; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				}
				_cnt160++;
			} while (true);
			}
			element_spec();
			astFactory.addASTChild(currentAST, returnAST);
			match(SEMI);
			case_stmt_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_52);
			} else {
			  throw ex;
			}
		}
		returnAST = case_stmt_AST;
	}
	
	public final void element_spec() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST element_spec_AST = null;
		
		try {      // for error handling
			type_spec();
			astFactory.addASTChild(currentAST, returnAST);
			declarator();
			astFactory.addASTChild(currentAST, returnAST);
			element_spec_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = element_spec_AST;
	}
	
	public final void enumerator_list() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST enumerator_list_AST = null;
		
		try {      // for error handling
			enumerator();
			astFactory.addASTChild(currentAST, returnAST);
			{
			_loop165:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					enumerator();
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop165;
				}
				
			} while (true);
			}
			enumerator_list_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_15);
			} else {
			  throw ex;
			}
		}
		returnAST = enumerator_list_AST;
	}
	
	public final void enumerator() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST enumerator_AST = null;
		
		try {      // for error handling
			identifier();
			astFactory.addASTChild(currentAST, returnAST);
			enumerator_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_53);
			} else {
			  throw ex;
			}
		}
		returnAST = enumerator_AST;
	}
	
	public final void opt_pos_int() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST opt_pos_int_AST = null;
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case COMMA:
			{
				match(COMMA);
				positive_int_const();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case GT:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			opt_pos_int_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_54);
			} else {
			  throw ex;
			}
		}
		returnAST = opt_pos_int_AST;
	}
	
	public final void fixed_array_size() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST fixed_array_size_AST = null;
		
		try {      // for error handling
			match(LBRACK);
			positive_int_const();
			astFactory.addASTChild(currentAST, returnAST);
			match(RBRACK);
			fixed_array_size_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_55);
			} else {
			  throw ex;
			}
		}
		returnAST = fixed_array_size_AST;
	}
	
	public final void readonly_attr_spec() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST readonly_attr_spec_AST = null;
		
		try {      // for error handling
			AST tmp163_AST = null;
			tmp163_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp163_AST);
			match(LITERAL_readonly);
			AST tmp164_AST = null;
			tmp164_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp164_AST);
			match(LITERAL_attribute);
			param_type_spec();
			astFactory.addASTChild(currentAST, returnAST);
			readonly_attr_declarator();
			astFactory.addASTChild(currentAST, returnAST);
			readonly_attr_spec_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = readonly_attr_spec_AST;
	}
	
	public final void attr_spec() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST attr_spec_AST = null;
		
		try {      // for error handling
			AST tmp165_AST = null;
			tmp165_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp165_AST);
			match(LITERAL_attribute);
			param_type_spec();
			astFactory.addASTChild(currentAST, returnAST);
			attr_declarator();
			astFactory.addASTChild(currentAST, returnAST);
			attr_spec_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = attr_spec_AST;
	}
	
	public final void opt_member_list() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST opt_member_list_AST = null;
		
		try {      // for error handling
			{
			_loop182:
			do {
				if ((_tokenSet_13.member(LA(1)))) {
					member();
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop182;
				}
				
			} while (true);
			}
			opt_member_list_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_15);
			} else {
			  throw ex;
			}
		}
		returnAST = opt_member_list_AST;
	}
	
	public final void op_attribute() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST op_attribute_AST = null;
		
		try {      // for error handling
			AST tmp166_AST = null;
			tmp166_AST = astFactory.create(LT(1));
			astFactory.addASTChild(currentAST, tmp166_AST);
			match(LITERAL_oneway);
			op_attribute_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_56);
			} else {
			  throw ex;
			}
		}
		returnAST = op_attribute_AST;
	}
	
	public final void op_type_spec() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST op_type_spec_AST = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case SCOPEOP:
			case IDENT:
			case LITERAL_float:
			case LITERAL_double:
			case LITERAL_long:
			case LITERAL_short:
			case LITERAL_unsigned:
			case LITERAL_char:
			case LITERAL_wchar:
			case LITERAL_boolean:
			case LITERAL_octet:
			case LITERAL_any:
			case LITERAL_Object:
			case LITERAL_string:
			case LITERAL_wstring:
			case LITERAL_ValueBase:
			{
				param_type_spec();
				astFactory.addASTChild(currentAST, returnAST);
				op_type_spec_AST = (AST)currentAST.root;
				break;
			}
			case LITERAL_void:
			{
				AST tmp167_AST = null;
				tmp167_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp167_AST);
				match(LITERAL_void);
				op_type_spec_AST = (AST)currentAST.root;
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_31);
			} else {
			  throw ex;
			}
		}
		returnAST = op_type_spec_AST;
	}
	
	public final void parameter_dcls() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST parameter_dcls_AST = null;
		
		try {      // for error handling
			match(LPAREN);
			{
			switch ( LA(1)) {
			case LITERAL_in:
			case LITERAL_out:
			case LITERAL_inout:
			{
				param_dcl_list();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case RPAREN:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(RPAREN);
			parameter_dcls_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_57);
			} else {
			  throw ex;
			}
		}
		returnAST = parameter_dcls_AST;
	}
	
	public final void context_expr() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST context_expr_AST = null;
		
		try {      // for error handling
			AST tmp170_AST = null;
			tmp170_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp170_AST);
			match(LITERAL_context);
			match(LPAREN);
			string_literal_list();
			astFactory.addASTChild(currentAST, returnAST);
			match(RPAREN);
			context_expr_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = context_expr_AST;
	}
	
	public final void param_dcl_list() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST param_dcl_list_AST = null;
		
		try {      // for error handling
			param_dcl();
			astFactory.addASTChild(currentAST, returnAST);
			{
			_loop193:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					param_dcl();
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop193;
				}
				
			} while (true);
			}
			param_dcl_list_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_27);
			} else {
			  throw ex;
			}
		}
		returnAST = param_dcl_list_AST;
	}
	
	public final void param_dcl() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST param_dcl_AST = null;
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case LITERAL_in:
			{
				AST tmp174_AST = null;
				tmp174_AST = astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp174_AST);
				match(LITERAL_in);
				break;
			}
			case LITERAL_out:
			{
				AST tmp175_AST = null;
				tmp175_AST = astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp175_AST);
				match(LITERAL_out);
				break;
			}
			case LITERAL_inout:
			{
				AST tmp176_AST = null;
				tmp176_AST = astFactory.create(LT(1));
				astFactory.makeASTRoot(currentAST, tmp176_AST);
				match(LITERAL_inout);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			param_type_spec();
			astFactory.addASTChild(currentAST, returnAST);
			simple_declarator();
			astFactory.addASTChild(currentAST, returnAST);
			param_dcl_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_29);
			} else {
			  throw ex;
			}
		}
		returnAST = param_dcl_AST;
	}
	
	public final void string_literal_list() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST string_literal_list_AST = null;
		
		try {      // for error handling
			string_literal();
			astFactory.addASTChild(currentAST, returnAST);
			{
			_loop200:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					string_literal();
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop200;
				}
				
			} while (true);
			}
			string_literal_list_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_27);
			} else {
			  throw ex;
			}
		}
		returnAST = string_literal_list_AST;
	}
	
	public final void imported_scope() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST imported_scope_AST = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case SCOPEOP:
			case IDENT:
			{
				scoped_name();
				astFactory.addASTChild(currentAST, returnAST);
				imported_scope_AST = (AST)currentAST.root;
				break;
			}
			case STRING_LITERAL:
			{
				string_literal();
				astFactory.addASTChild(currentAST, returnAST);
				imported_scope_AST = (AST)currentAST.root;
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = imported_scope_AST;
	}
	
	public final void readonly_attr_declarator() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST readonly_attr_declarator_AST = null;
		
		try {      // for error handling
			simple_declarator();
			astFactory.addASTChild(currentAST, returnAST);
			{
			switch ( LA(1)) {
			case LITERAL_raises:
			{
				raises_expr();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case SEMI:
			case COMMA:
			{
				{
				_loop214:
				do {
					if ((LA(1)==COMMA)) {
						match(COMMA);
						simple_declarator();
						astFactory.addASTChild(currentAST, returnAST);
					}
					else {
						break _loop214;
					}
					
				} while (true);
				}
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			readonly_attr_declarator_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = readonly_attr_declarator_AST;
	}
	
	public final void attr_declarator() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST attr_declarator_AST = null;
		
		try {      // for error handling
			simple_declarator();
			astFactory.addASTChild(currentAST, returnAST);
			{
			boolean synPredMatched219 = false;
			if (((LA(1)==SEMI||LA(1)==LITERAL_getraises||LA(1)==LITERAL_setraises) && (_tokenSet_58.member(LA(2))) && (_tokenSet_59.member(LA(3))) && (_tokenSet_60.member(LA(4))))) {
				int _m219 = mark();
				synPredMatched219 = true;
				inputState.guessing++;
				try {
					{
					switch ( LA(1)) {
					case LITERAL_getraises:
					{
						match(LITERAL_getraises);
						break;
					}
					case LITERAL_setraises:
					{
						match(LITERAL_setraises);
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
				}
				catch (RecognitionException pe) {
					synPredMatched219 = false;
				}
				rewind(_m219);
				inputState.guessing--;
			}
			if ( synPredMatched219 ) {
				attr_raises_expr();
				astFactory.addASTChild(currentAST, returnAST);
			}
			else if ((LA(1)==SEMI||LA(1)==COMMA) && (_tokenSet_61.member(LA(2))) && (_tokenSet_62.member(LA(3))) && (_tokenSet_63.member(LA(4)))) {
				{
				_loop221:
				do {
					if ((LA(1)==COMMA)) {
						match(COMMA);
						simple_declarator();
						astFactory.addASTChild(currentAST, returnAST);
					}
					else {
						break _loop221;
					}
					
				} while (true);
				}
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
			attr_declarator_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = attr_declarator_AST;
	}
	
	public final void attr_raises_expr() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST attr_raises_expr_AST = null;
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case LITERAL_getraises:
			{
				get_excep_expr();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case SEMI:
			case LITERAL_setraises:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			{
			switch ( LA(1)) {
			case LITERAL_setraises:
			{
				set_excep_expr();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case SEMI:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			attr_raises_expr_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = attr_raises_expr_AST;
	}
	
	public final void get_excep_expr() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST get_excep_expr_AST = null;
		
		try {      // for error handling
			AST tmp180_AST = null;
			tmp180_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp180_AST);
			match(LITERAL_getraises);
			exception_list();
			astFactory.addASTChild(currentAST, returnAST);
			get_excep_expr_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_64);
			} else {
			  throw ex;
			}
		}
		returnAST = get_excep_expr_AST;
	}
	
	public final void set_excep_expr() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST set_excep_expr_AST = null;
		
		try {      // for error handling
			AST tmp181_AST = null;
			tmp181_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp181_AST);
			match(LITERAL_setraises);
			exception_list();
			astFactory.addASTChild(currentAST, returnAST);
			set_excep_expr_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = set_excep_expr_AST;
	}
	
	public final void exception_list() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST exception_list_AST = null;
		
		try {      // for error handling
			match(LPAREN);
			scoped_name();
			astFactory.addASTChild(currentAST, returnAST);
			{
			_loop229:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					scoped_name();
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop229;
				}
				
			} while (true);
			}
			match(RPAREN);
			exception_list_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_64);
			} else {
			  throw ex;
			}
		}
		returnAST = exception_list_AST;
	}
	
	public final void component_dcl() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST component_dcl_AST = null;
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case COLON:
			{
				component_inheritance_spec();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case LCURLY:
			case LITERAL_supports:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			{
			switch ( LA(1)) {
			case LITERAL_supports:
			{
				supported_interface_spec();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case LCURLY:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(LCURLY);
			component_body();
			astFactory.addASTChild(currentAST, returnAST);
			match(RCURLY);
			component_dcl_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = component_dcl_AST;
	}
	
	public final void component_inheritance_spec() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST component_inheritance_spec_AST = null;
		
		try {      // for error handling
			AST tmp187_AST = null;
			tmp187_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp187_AST);
			match(COLON);
			scoped_name();
			astFactory.addASTChild(currentAST, returnAST);
			component_inheritance_spec_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_25);
			} else {
			  throw ex;
			}
		}
		returnAST = component_inheritance_spec_AST;
	}
	
	public final void supported_interface_spec() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST supported_interface_spec_AST = null;
		
		try {      // for error handling
			AST tmp188_AST = null;
			tmp188_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp188_AST);
			match(LITERAL_supports);
			scoped_name();
			astFactory.addASTChild(currentAST, returnAST);
			{
			_loop237:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					scoped_name();
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop237;
				}
				
			} while (true);
			}
			supported_interface_spec_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_65);
			} else {
			  throw ex;
			}
		}
		returnAST = supported_interface_spec_AST;
	}
	
	public final void component_body() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST component_body_AST = null;
		
		try {      // for error handling
			{
			_loop241:
			do {
				if ((_tokenSet_66.member(LA(1)))) {
					component_export();
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop241;
				}
				
			} while (true);
			}
			component_body_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_15);
			} else {
			  throw ex;
			}
		}
		returnAST = component_body_AST;
	}
	
	public final void component_export() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST component_export_AST = null;
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case LITERAL_provides:
			{
				provides_dcl();
				astFactory.addASTChild(currentAST, returnAST);
				match(SEMI);
				break;
			}
			case LITERAL_uses:
			{
				uses_dcl();
				astFactory.addASTChild(currentAST, returnAST);
				match(SEMI);
				break;
			}
			case LITERAL_emits:
			{
				emits_dcl();
				astFactory.addASTChild(currentAST, returnAST);
				match(SEMI);
				break;
			}
			case LITERAL_publishes:
			{
				publishes_dcl();
				astFactory.addASTChild(currentAST, returnAST);
				match(SEMI);
				break;
			}
			case LITERAL_consumes:
			{
				consumes_dcl();
				astFactory.addASTChild(currentAST, returnAST);
				match(SEMI);
				break;
			}
			case LITERAL_readonly:
			case LITERAL_attribute:
			{
				attr_dcl();
				astFactory.addASTChild(currentAST, returnAST);
				match(SEMI);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			component_export_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_67);
			} else {
			  throw ex;
			}
		}
		returnAST = component_export_AST;
	}
	
	public final void provides_dcl() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST provides_dcl_AST = null;
		
		try {      // for error handling
			AST tmp196_AST = null;
			tmp196_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp196_AST);
			match(LITERAL_provides);
			interface_type();
			astFactory.addASTChild(currentAST, returnAST);
			identifier();
			astFactory.addASTChild(currentAST, returnAST);
			provides_dcl_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = provides_dcl_AST;
	}
	
	public final void uses_dcl() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST uses_dcl_AST = null;
		
		try {      // for error handling
			AST tmp197_AST = null;
			tmp197_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp197_AST);
			match(LITERAL_uses);
			{
			switch ( LA(1)) {
			case LITERAL_multiple:
			{
				AST tmp198_AST = null;
				tmp198_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp198_AST);
				match(LITERAL_multiple);
				break;
			}
			case SCOPEOP:
			case IDENT:
			case LITERAL_Object:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			interface_type();
			astFactory.addASTChild(currentAST, returnAST);
			identifier();
			astFactory.addASTChild(currentAST, returnAST);
			uses_dcl_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = uses_dcl_AST;
	}
	
	public final void emits_dcl() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST emits_dcl_AST = null;
		
		try {      // for error handling
			AST tmp199_AST = null;
			tmp199_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp199_AST);
			match(LITERAL_emits);
			scoped_name();
			astFactory.addASTChild(currentAST, returnAST);
			identifier();
			astFactory.addASTChild(currentAST, returnAST);
			emits_dcl_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = emits_dcl_AST;
	}
	
	public final void publishes_dcl() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST publishes_dcl_AST = null;
		
		try {      // for error handling
			AST tmp200_AST = null;
			tmp200_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp200_AST);
			match(LITERAL_publishes);
			scoped_name();
			astFactory.addASTChild(currentAST, returnAST);
			identifier();
			astFactory.addASTChild(currentAST, returnAST);
			publishes_dcl_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = publishes_dcl_AST;
	}
	
	public final void consumes_dcl() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST consumes_dcl_AST = null;
		
		try {      // for error handling
			AST tmp201_AST = null;
			tmp201_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp201_AST);
			match(LITERAL_consumes);
			scoped_name();
			astFactory.addASTChild(currentAST, returnAST);
			identifier();
			astFactory.addASTChild(currentAST, returnAST);
			consumes_dcl_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = consumes_dcl_AST;
	}
	
	public final void interface_type() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST interface_type_AST = null;
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case SCOPEOP:
			case IDENT:
			{
				scoped_name();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case LITERAL_Object:
			{
				AST tmp202_AST = null;
				tmp202_AST = astFactory.create(LT(1));
				astFactory.addASTChild(currentAST, tmp202_AST);
				match(LITERAL_Object);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			interface_type_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_31);
			} else {
			  throw ex;
			}
		}
		returnAST = interface_type_AST;
	}
	
	public final void home_header() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST home_header_AST = null;
		
		try {      // for error handling
			AST tmp203_AST = null;
			tmp203_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp203_AST);
			match(LITERAL_home);
			identifier();
			astFactory.addASTChild(currentAST, returnAST);
			{
			switch ( LA(1)) {
			case COLON:
			{
				home_inheritance_spec();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case LITERAL_supports:
			case LITERAL_manages:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			{
			switch ( LA(1)) {
			case LITERAL_supports:
			{
				supported_interface_spec();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case LITERAL_manages:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(LITERAL_manages);
			scoped_name();
			astFactory.addASTChild(currentAST, returnAST);
			{
			switch ( LA(1)) {
			case LITERAL_primarykey:
			{
				primary_key_spec();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case LCURLY:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			home_header_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_16);
			} else {
			  throw ex;
			}
		}
		returnAST = home_header_AST;
	}
	
	public final void home_body() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST home_body_AST = null;
		
		try {      // for error handling
			match(LCURLY);
			{
			_loop261:
			do {
				if ((_tokenSet_68.member(LA(1)))) {
					home_export();
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop261;
				}
				
			} while (true);
			}
			match(RCURLY);
			home_body_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = home_body_AST;
	}
	
	public final void home_inheritance_spec() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST home_inheritance_spec_AST = null;
		
		try {      // for error handling
			AST tmp207_AST = null;
			tmp207_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp207_AST);
			match(COLON);
			scoped_name();
			astFactory.addASTChild(currentAST, returnAST);
			home_inheritance_spec_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_69);
			} else {
			  throw ex;
			}
		}
		returnAST = home_inheritance_spec_AST;
	}
	
	public final void primary_key_spec() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST primary_key_spec_AST = null;
		
		try {      // for error handling
			AST tmp208_AST = null;
			tmp208_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp208_AST);
			match(LITERAL_primarykey);
			scoped_name();
			astFactory.addASTChild(currentAST, returnAST);
			primary_key_spec_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_16);
			} else {
			  throw ex;
			}
		}
		returnAST = primary_key_spec_AST;
	}
	
	public final void home_export() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST home_export_AST = null;
		
		try {      // for error handling
			{
			switch ( LA(1)) {
			case SCOPEOP:
			case IDENT:
			case LITERAL_const:
			case LITERAL_typedef:
			case LITERAL_native:
			case LITERAL_float:
			case LITERAL_double:
			case LITERAL_long:
			case LITERAL_short:
			case LITERAL_unsigned:
			case LITERAL_char:
			case LITERAL_wchar:
			case LITERAL_boolean:
			case LITERAL_octet:
			case LITERAL_any:
			case LITERAL_Object:
			case LITERAL_struct:
			case LITERAL_union:
			case LITERAL_enum:
			case LITERAL_string:
			case LITERAL_wstring:
			case LITERAL_exception:
			case LITERAL_oneway:
			case LITERAL_void:
			case LITERAL_ValueBase:
			case LITERAL_typeid:
			case LITERAL_typeprefix:
			case LITERAL_readonly:
			case LITERAL_attribute:
			{
				export();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case LITERAL_factory:
			{
				factory_dcl();
				astFactory.addASTChild(currentAST, returnAST);
				match(SEMI);
				break;
			}
			case LITERAL_finder:
			{
				finder_dcl();
				astFactory.addASTChild(currentAST, returnAST);
				match(SEMI);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			home_export_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_70);
			} else {
			  throw ex;
			}
		}
		returnAST = home_export_AST;
	}
	
	public final void factory_dcl() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST factory_dcl_AST = null;
		
		try {      // for error handling
			AST tmp211_AST = null;
			tmp211_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp211_AST);
			match(LITERAL_factory);
			identifier();
			astFactory.addASTChild(currentAST, returnAST);
			match(LPAREN);
			init_param_decls();
			astFactory.addASTChild(currentAST, returnAST);
			match(RPAREN);
			{
			switch ( LA(1)) {
			case LITERAL_raises:
			{
				raises_expr();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case SEMI:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			factory_dcl_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = factory_dcl_AST;
	}
	
	public final void finder_dcl() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST finder_dcl_AST = null;
		
		try {      // for error handling
			AST tmp214_AST = null;
			tmp214_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp214_AST);
			match(LITERAL_finder);
			identifier();
			astFactory.addASTChild(currentAST, returnAST);
			match(LPAREN);
			init_param_decls();
			astFactory.addASTChild(currentAST, returnAST);
			match(RPAREN);
			{
			switch ( LA(1)) {
			case LITERAL_raises:
			{
				raises_expr();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case SEMI:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			finder_dcl_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = finder_dcl_AST;
	}
	
	public final void event_abs() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST event_abs_AST = null;
		
		try {      // for error handling
			AST tmp217_AST = null;
			tmp217_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp217_AST);
			match(LITERAL_abstract);
			event_header();
			astFactory.addASTChild(currentAST, returnAST);
			{
			switch ( LA(1)) {
			case LCURLY:
			case COLON:
			case LITERAL_supports:
			{
				event_abs_dcl();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case SEMI:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			event_abs_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = event_abs_AST;
	}
	
	public final void event_custom() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST event_custom_AST = null;
		
		try {      // for error handling
			AST tmp218_AST = null;
			tmp218_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp218_AST);
			match(LITERAL_custom);
			event_header();
			astFactory.addASTChild(currentAST, returnAST);
			event_elem_dcl();
			astFactory.addASTChild(currentAST, returnAST);
			event_custom_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = event_custom_AST;
	}
	
	public final void event_dcl() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST event_dcl_AST = null;
		
		try {      // for error handling
			event_header();
			astFactory.addASTChild(currentAST, returnAST);
			{
			switch ( LA(1)) {
			case LCURLY:
			case COLON:
			case LITERAL_supports:
			{
				event_elem_dcl();
				astFactory.addASTChild(currentAST, returnAST);
				break;
			}
			case SEMI:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			event_dcl_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = event_dcl_AST;
	}
	
	public final void event_header() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST event_header_AST = null;
		
		try {      // for error handling
			AST tmp219_AST = null;
			tmp219_AST = astFactory.create(LT(1));
			astFactory.makeASTRoot(currentAST, tmp219_AST);
			match(LITERAL_eventtype);
			identifier();
			astFactory.addASTChild(currentAST, returnAST);
			event_header_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_71);
			} else {
			  throw ex;
			}
		}
		returnAST = event_header_AST;
	}
	
	public final void event_abs_dcl() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST event_abs_dcl_AST = null;
		
		try {      // for error handling
			value_inheritance_spec();
			astFactory.addASTChild(currentAST, returnAST);
			match(LCURLY);
			{
			_loop275:
			do {
				if ((_tokenSet_17.member(LA(1)))) {
					export();
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop275;
				}
				
			} while (true);
			}
			match(RCURLY);
			event_abs_dcl_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = event_abs_dcl_AST;
	}
	
	public final void event_elem_dcl() throws RecognitionException, TokenStreamException {
		
		returnAST = null;
		ASTPair currentAST = new ASTPair();
		AST event_elem_dcl_AST = null;
		
		try {      // for error handling
			value_inheritance_spec();
			astFactory.addASTChild(currentAST, returnAST);
			match(LCURLY);
			{
			_loop281:
			do {
				if ((_tokenSet_17.member(LA(1)))) {
					export();
					astFactory.addASTChild(currentAST, returnAST);
				}
				else {
					break _loop281;
				}
				
			} while (true);
			}
			match(RCURLY);
			event_elem_dcl_AST = (AST)currentAST.root;
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_10);
			} else {
			  throw ex;
			}
		}
		returnAST = event_elem_dcl_AST;
	}
	
	
	public static final String[] _tokenNames = {
		"<0>",
		"EOF",
		"<2>",
		"NULL_TREE_LOOKAHEAD",
		";",
		"\"abstract\"",
		"\"local\"",
		"\"interface\"",
		"\"custom\"",
		"\"valuetype\"",
		"\"eventtype\"",
		"\"module\"",
		"{",
		"}",
		":",
		",",
		"::",
		"an identifer",
		"\"truncatable\"",
		"\"supports\"",
		"\"public\"",
		"\"private\"",
		"\"factory\"",
		"(",
		")",
		"\"in\"",
		"\"const\"",
		"=",
		"|",
		"^",
		"&",
		"<<",
		">>",
		"+",
		"-",
		"*",
		"/",
		"%",
		"~",
		"\"TRUE\"",
		"\"FALSE\"",
		"\"typedef\"",
		"\"native\"",
		"\"float\"",
		"\"double\"",
		"\"long\"",
		"\"short\"",
		"\"unsigned\"",
		"\"char\"",
		"\"wchar\"",
		"\"boolean\"",
		"\"octet\"",
		"\"any\"",
		"\"Object\"",
		"\"struct\"",
		"\"union\"",
		"\"switch\"",
		"\"case\"",
		"\"default\"",
		"\"enum\"",
		"\"sequence\"",
		"<",
		">",
		"\"string\"",
		"\"wstring\"",
		"[",
		"]",
		"\"exception\"",
		"\"oneway\"",
		"\"void\"",
		"\"out\"",
		"\"inout\"",
		"\"raises\"",
		"\"context\"",
		"\"fixed\"",
		"\"ValueBase\"",
		"\"import\"",
		"\"typeid\"",
		"\"typeprefix\"",
		"\"readonly\"",
		"\"attribute\"",
		"\"getraises\"",
		"\"setraises\"",
		"\"component\"",
		"\"provides\"",
		"\"uses\"",
		"\"multiple\"",
		"\"emits\"",
		"\"publishes\"",
		"\"consumes\"",
		"\"home\"",
		"\"manages\"",
		"\"primarykey\"",
		"\"finder\"",
		"an integer value",
		"an octal value",
		"a hexadecimal value value",
		"a string literal",
		"a wide string literal",
		"a character literal",
		"a wide character literal",
		"FIXED",
		"a floating point value",
		"?",
		".",
		"!",
		"white space",
		"a preprocessor directive",
		"a comment",
		"a comment",
		"an escape sequence",
		"an escaped character value",
		"a digit",
		"a non-zero digit",
		"an octal digit",
		"a hexadecimal digit",
		"an escaped identifer"
	};
	
	protected void buildTokenTypeASTClassMap() {
		tokenTypeToASTClassMap=null;
	};
	
	private static final long[] mk_tokenSet_0() {
		long[] data = { 630510544968749024L, 67657736L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = { 2L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = { 630510544968749024L, 67661832L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	private static final long[] mk_tokenSet_3() {
		long[] data = { 151568L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_3 = new BitSet(mk_tokenSet_3());
	private static final long[] mk_tokenSet_4() {
		long[] data = { -8574855889469341710L, 67758137L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_4 = new BitSet(mk_tokenSet_4());
	private static final long[] mk_tokenSet_5() {
		long[] data = { -7421940981998858224L, 3073L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_5 = new BitSet(mk_tokenSet_5());
	private static final long[] mk_tokenSet_6() {
		long[] data = { -6269012880247521294L, 67758137L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_6 = new BitSet(mk_tokenSet_6());
	private static final long[] mk_tokenSet_7() {
		long[] data = { 675856L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_7 = new BitSet(mk_tokenSet_7());
	private static final long[] mk_tokenSet_8() {
		long[] data = { -8574855889468555278L, 67758137L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_8 = new BitSet(mk_tokenSet_8());
	private static final long[] mk_tokenSet_9() {
		long[] data = { 630510544968757218L, 67657736L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_9 = new BitSet(mk_tokenSet_9());
	private static final long[] mk_tokenSet_10() {
		long[] data = { 16L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_10 = new BitSet(mk_tokenSet_10());
	private static final long[] mk_tokenSet_11() {
		long[] data = { -8574855889469345792L, 124985L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_11 = new BitSet(mk_tokenSet_11());
	private static final long[] mk_tokenSet_12() {
		long[] data = { 630510544968757234L, 67657736L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_12 = new BitSet(mk_tokenSet_12());
	private static final long[] mk_tokenSet_13() {
		long[] data = { -7421940981999403008L, 3073L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_13 = new BitSet(mk_tokenSet_13());
	private static final long[] mk_tokenSet_14() {
		long[] data = { -2738197094764646384L, 8992984325L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_14 = new BitSet(mk_tokenSet_14());
	private static final long[] mk_tokenSet_15() {
		long[] data = { 8192L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_15 = new BitSet(mk_tokenSet_15());
	private static final long[] mk_tokenSet_16() {
		long[] data = { 4096L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_16 = new BitSet(mk_tokenSet_16());
	private static final long[] mk_tokenSet_17() {
		long[] data = { -8574855889469374464L, 124985L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_17 = new BitSet(mk_tokenSet_17());
	private static final long[] mk_tokenSet_18() {
		long[] data = { -8574855889462026240L, 536995897L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_18 = new BitSet(mk_tokenSet_18());
	private static final long[] mk_tokenSet_19() {
		long[] data = { 16781312L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_19 = new BitSet(mk_tokenSet_19());
	private static final long[] mk_tokenSet_20() {
		long[] data = { 36864L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_20 = new BitSet(mk_tokenSet_20());
	private static final long[] mk_tokenSet_21() {
		long[] data = { 4611686293054345232L, 8992587780L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_21 = new BitSet(mk_tokenSet_21());
	private static final long[] mk_tokenSet_22() {
		long[] data = { -8574855889462034432L, 124985L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_22 = new BitSet(mk_tokenSet_22());
	private static final long[] mk_tokenSet_23() {
		long[] data = { 131088L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_23 = new BitSet(mk_tokenSet_23());
	private static final long[] mk_tokenSet_24() {
		long[] data = { -8574855889462026240L, 124985L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_24 = new BitSet(mk_tokenSet_24());
	private static final long[] mk_tokenSet_25() {
		long[] data = { 528384L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_25 = new BitSet(mk_tokenSet_25());
	private static final long[] mk_tokenSet_26() {
		long[] data = { 561152L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_26 = new BitSet(mk_tokenSet_26());
	private static final long[] mk_tokenSet_27() {
		long[] data = { 16777216L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_27 = new BitSet(mk_tokenSet_27());
	private static final long[] mk_tokenSet_28() {
		long[] data = { 16L, 512L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_28 = new BitSet(mk_tokenSet_28());
	private static final long[] mk_tokenSet_29() {
		long[] data = { 16809984L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_29 = new BitSet(mk_tokenSet_29());
	private static final long[] mk_tokenSet_30() {
		long[] data = { -9205366434438119424L, 2049L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_30 = new BitSet(mk_tokenSet_30());
	private static final long[] mk_tokenSet_31() {
		long[] data = { 131072L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_31 = new BitSet(mk_tokenSet_31());
	private static final long[] mk_tokenSet_32() {
		long[] data = { 16810000L, 393472L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_32 = new BitSet(mk_tokenSet_32());
	private static final long[] mk_tokenSet_33() {
		long[] data = { 1950057955328L, 548682072064L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_33 = new BitSet(mk_tokenSet_33());
	private static final long[] mk_tokenSet_34() {
		long[] data = { 4611686018444214288L, 4L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_34 = new BitSet(mk_tokenSet_34());
	private static final long[] mk_tokenSet_35() {
		long[] data = { 4611686018444328976L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_35 = new BitSet(mk_tokenSet_35());
	private static final long[] mk_tokenSet_36() {
		long[] data = { 4611686018427551760L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_36 = new BitSet(mk_tokenSet_36());
	private static final long[] mk_tokenSet_37() {
		long[] data = { 4611686018712649744L, 4L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_37 = new BitSet(mk_tokenSet_37());
	private static final long[] mk_tokenSet_38() {
		long[] data = { 4611686019249520656L, 4L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_38 = new BitSet(mk_tokenSet_38());
	private static final long[] mk_tokenSet_39() {
		long[] data = { 4611686020323262480L, 4L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_39 = new BitSet(mk_tokenSet_39());
	private static final long[] mk_tokenSet_40() {
		long[] data = { 4611686026765713424L, 4L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_40 = new BitSet(mk_tokenSet_40());
	private static final long[] mk_tokenSet_41() {
		long[] data = { 4611686052535517200L, 4L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_41 = new BitSet(mk_tokenSet_41());
	private static final long[] mk_tokenSet_42() {
		long[] data = { 4611686293053685776L, 4L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_42 = new BitSet(mk_tokenSet_42());
	private static final long[] mk_tokenSet_43() {
		long[] data = { 4611686018427420672L, 4L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_43 = new BitSet(mk_tokenSet_43());
	private static final long[] mk_tokenSet_44() {
		long[] data = { 16908304L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_44 = new BitSet(mk_tokenSet_44());
	private static final long[] mk_tokenSet_45() {
		long[] data = { 4611703610613596176L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_45 = new BitSet(mk_tokenSet_45());
	private static final long[] mk_tokenSet_46() {
		long[] data = { 5242198513336692722L, 548750123274L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_46 = new BitSet(mk_tokenSet_46());
	private static final long[] mk_tokenSet_47() {
		long[] data = { -2377900603386646542L, 549348962299L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_47 = new BitSet(mk_tokenSet_47());
	private static final long[] mk_tokenSet_48() {
		long[] data = { 4611791571543818256L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_48 = new BitSet(mk_tokenSet_48());
	private static final long[] mk_tokenSet_49() {
		long[] data = { 5242233697708781554L, 548750123274L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_49 = new BitSet(mk_tokenSet_49());
	private static final long[] mk_tokenSet_50() {
		long[] data = { 32784L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_50 = new BitSet(mk_tokenSet_50());
	private static final long[] mk_tokenSet_51() {
		long[] data = { -7421940981999394816L, 3073L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_51 = new BitSet(mk_tokenSet_51());
	private static final long[] mk_tokenSet_52() {
		long[] data = { 432345564227575808L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_52 = new BitSet(mk_tokenSet_52());
	private static final long[] mk_tokenSet_53() {
		long[] data = { 40960L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_53 = new BitSet(mk_tokenSet_53());
	private static final long[] mk_tokenSet_54() {
		long[] data = { 4611686018427387904L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_54 = new BitSet(mk_tokenSet_54());
	private static final long[] mk_tokenSet_55() {
		long[] data = { 32784L, 2L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_55 = new BitSet(mk_tokenSet_55());
	private static final long[] mk_tokenSet_56() {
		long[] data = { -9205366434438119424L, 2081L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_56 = new BitSet(mk_tokenSet_56());
	private static final long[] mk_tokenSet_57() {
		long[] data = { 16L, 768L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_57 = new BitSet(mk_tokenSet_57());
	private static final long[] mk_tokenSet_58() {
		long[] data = { -8574855889453637632L, 598861881L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_58 = new BitSet(mk_tokenSet_58());
	private static final long[] mk_tokenSet_59() {
		long[] data = { -5116097972785709040L, 4262945L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_59 = new BitSet(mk_tokenSet_59());
	private static final long[] mk_tokenSet_60() {
		long[] data = { -6196953336277385230L, 548749731849L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_60 = new BitSet(mk_tokenSet_60());
	private static final long[] mk_tokenSet_61() {
		long[] data = { -8574855889462026240L, 598861881L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_61 = new BitSet(mk_tokenSet_61());
	private static final long[] mk_tokenSet_62() {
		long[] data = { -5116097972785676272L, 4262945L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_62 = new BitSet(mk_tokenSet_62());
	private static final long[] mk_tokenSet_63() {
		long[] data = { -6196953336286855182L, 549348567097L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_63 = new BitSet(mk_tokenSet_63());
	private static final long[] mk_tokenSet_64() {
		long[] data = { 16L, 262144L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_64 = new BitSet(mk_tokenSet_64());
	private static final long[] mk_tokenSet_65() {
		long[] data = { 4096L, 134217728L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_65 = new BitSet(mk_tokenSet_65());
	private static final long[] mk_tokenSet_66() {
		long[] data = { 0L, 61964288L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_66 = new BitSet(mk_tokenSet_66());
	private static final long[] mk_tokenSet_67() {
		long[] data = { 8192L, 61964288L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_67 = new BitSet(mk_tokenSet_67());
	private static final long[] mk_tokenSet_68() {
		long[] data = { -8574855889465180160L, 536995897L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_68 = new BitSet(mk_tokenSet_68());
	private static final long[] mk_tokenSet_69() {
		long[] data = { 524288L, 134217728L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_69 = new BitSet(mk_tokenSet_69());
	private static final long[] mk_tokenSet_70() {
		long[] data = { -8574855889465171968L, 536995897L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_70 = new BitSet(mk_tokenSet_70());
	private static final long[] mk_tokenSet_71() {
		long[] data = { 544784L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_71 = new BitSet(mk_tokenSet_71());
	
	}
