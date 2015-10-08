package org.solr.patch.negetive.fl;
 
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.lucene.queries.function.FunctionQuery;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.queries.function.valuesource.QueryValueSource;
import org.apache.lucene.search.Query;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.transform.DocTransformer;
import org.apache.solr.response.transform.DocTransformers;
import org.apache.solr.response.transform.RenameFieldTransformer;
import org.apache.solr.response.transform.ScoreAugmenter;
import org.apache.solr.response.transform.TransformerFactory;
import org.apache.solr.response.transform.ValueSourceAugmenter;
import org.apache.solr.search.FunctionQParser;
import org.apache.solr.search.FunctionQParserPlugin;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QueryParsing;
import org.apache.solr.search.SyntaxError;


/**
 * The implementation of return fields parsing for Solr with exclusion capability in filed list (fl) tag.Also it incorporates the logic 
 * to parse inbuilt as well as Custom Transformers.
 * 
 * @author Kuntal Ganguly
 * 
 */


/*	General rules
 ------------------
1) an inclusion (literal or glob) is ignored if in case of *.
	fl=* name na* (return all fields, name and na* fields are already included)
	fl=name * (same as before)

2) any exclusion token will be ignored if an inclusion has been defined before
	fl=name -id (returns all fields except id so "name", with other real fields, is implicitly included)

3) an inclusion token will clear all exclusion
	fl=-id,name (means "returns only name", no matter what exclusions are defined before)

4) score, transformers and functions doesn't change the behavior described above, they are just added to response

5) an empty or null fl will execute as "*" (all real fields) 

*/


public final class SolrReturnFields extends ReturnFields {
 

 private abstract class ParserState {
    
    protected abstract void onChar(char aChar, StringBuilder expressionBuffer, SolrQueryRequest request, DocTransformers augmenters)
       throws SyntaxError;
 


    protected void switchTo(final ParserState newState) {
      currentState = newState;
    }
    
  
    protected void restartWithNewToken(final StringBuilder expressionBuffer) {
      expressionBuffer.setLength(0);
      currentState = detectingTokenType;
    }
    

    protected boolean isFieldListExpressionStart(final char ch) {
      return Character.isJavaIdentifierPart(ch) || ch == POUND_SIGN || ch == LEFT_BRACE;
    }
    
   
   protected boolean isFieldListExpressionPart(final char ch) {
      return Character.isJavaIdentifierPart(ch) || ch == DOT || ch == COLON || ch == POUND_SIGN || ch == HYPHEN || ch == PLUS
          || ch == LEFT_BRACE || ch == RIGHT_BRACE || ch == '!';
    }

    protected boolean isSolrFunctionPart(final char ch) {
      return isFieldListExpressionPart(ch) || ch == ',' || ch == '.' || ch == ' ' || ch == '\'' || ch == '\"' || ch == '='
          || ch == '-' || ch == '/' || ch == PLUS;
     }


    protected boolean isConstantOrFunction(final StringBuilder buffer) {
      if (buffer.indexOf("{!func") != -1) {
        return true;
       }


      int offset = buffer.lastIndexOf(ALIAS_VALUE_SEPARATOR);
      offset = (offset == -1) ? 0 : offset + 1;
      for (int i = offset; i < buffer.length(); i++) {
        final char ch = buffer.charAt(i);
        if (!(Character.isDigit(ch) || ch == HYPHEN || ch == PLUS || ch == DOT)) {
          return false;
        }
       }
      return true;
     }
   }
 


  final ParserState collectingExclusionGlob = new ParserState() {
    @Override
    public void onChar(final char aChar, final StringBuilder expressionBuffer, final SolrQueryRequest request,
        final DocTransformers augmenters) throws SyntaxError {
      if (!isFieldListExpressionPart(aChar)) {
        if (aChar == WILDCARD || aChar == QUESTION_MARK) {
          expressionBuffer.append(aChar);
         }
       onExclusionGlobExpression(expressionBuffer);
        restartWithNewToken(expressionBuffer);
      } else {
        expressionBuffer.append(aChar);
       }

     }

  };

  final ParserState collectingExclusionToken = new ParserState() {
    @Override
    public void onChar(final char aChar, final StringBuilder buffer, final SolrQueryRequest request,
        final DocTransformers augmenters) throws SyntaxError {
      if (aChar == WILDCARD || aChar == QUESTION_MARK)
      {
        buffer.append(aChar);
        switchTo(collectingExclusionGlob);
      } else if (!isFieldListExpressionPart(aChar)) {
        if (isConstantOrFunction(buffer)) {
          onFunction(buffer, augmenters, request);
        } else {
          onExclusionLiteralFieldName(buffer);
        }
        restartWithNewToken(buffer);
      } else {
        buffer.append(aChar);
       }

     }

  };

  final ParserState collectingTransformer = new ParserState() {
    @Override
    public void onChar(final char aChar, final StringBuilder expressionBuffer, final SolrQueryRequest request,
        final DocTransformers augmenters) throws SyntaxError {
      expressionBuffer.append(aChar);
      if (aChar == CLOSE_BRACKET) {
        onTransformer(expressionBuffer, request, augmenters);
        restartWithNewToken(expressionBuffer);
      }
     }

  };

  final ParserState collectingInclusionGlob = new ParserState() {
    @Override
    public void onChar(final char aChar, final StringBuilder buffer, final SolrQueryRequest request,
        final DocTransformers augmenters) {
      if (isFieldListExpressionPart(aChar) || aChar == WILDCARD || aChar == QUESTION_MARK) {
        buffer.append(aChar);
      } else {
        onInclusionGlob(buffer);
        restartWithNewToken(buffer);
      }
     }

  };

  final ParserState collectingFunction = new ParserState() {
    private int openParenthesis;
    
    @Override
   public void onChar(final char aChar, final StringBuilder expressionBuffer, final SolrQueryRequest request,
        final DocTransformers augmenters)
       throws SyntaxError {
      switch (aChar) {
        case OPEN_PARENTHESIS:
          openParenthesis++;
          expressionBuffer.append(aChar);
           break;

        case CLOSE_PARENTHESIS:
          openParenthesis--;
          expressionBuffer.append(aChar);
         if (openParenthesis == 0) {
            onFunction(expressionBuffer, augmenters, request);
           restartWithNewToken(expressionBuffer);
          }
          break;
        default:
          if (isSolrFunctionPart(aChar)) {
           expressionBuffer.append(aChar);
          }
       }

     }

 };
 

  final ParserState collectingLiteral = new ParserState() {
    private int quoteCount;
    
    @Override
    public void onChar(final char aChar, final StringBuilder expressionBuffer, final SolrQueryRequest request,
       final DocTransformers augmenters) throws SyntaxError {
     switch (aChar) {
        case QUOTE:
        case DQUOTE:
          quoteCount++;
          expressionBuffer.append(aChar);
          if (quoteCount % 2 == 0) {
            onFunction(expressionBuffer, augmenters, request);
            restartWithNewToken(expressionBuffer);
           }

          break;
        default:
         if (isSolrFunctionPart(aChar)) {
           expressionBuffer.append(aChar);
           }

      }
    }
  };

  ParserState maybeInclusionLiteralOrGlobOrFunction = new ParserState() {
    @Override
    public void onChar(final char aChar, final StringBuilder builder, final SolrQueryRequest request,
        final DocTransformers augmenters) throws SyntaxError {
      switch (aChar) {
        case OPEN_PARENTHESIS:
          switchTo(collectingFunction);
          currentState.onChar(aChar, builder, request, augmenters);
          break;
        case QUOTE:
        case DQUOTE:
          switchTo(collectingLiteral);
          currentState.onChar(aChar, builder, request, augmenters);
          break;
        case QUESTION_MARK:
        case WILDCARD:
         builder.append(aChar);
         switchTo(collectingInclusionGlob);
         break;
        case OPEN_BRACKET:
          builder.append(aChar);
         switchTo(collectingTransformer);
          break;
        default:
          if (!isFieldListExpressionPart(aChar)) {
            if (isConstantOrFunction(builder)) {
              onFunction(builder, augmenters, request);
             } else {

              onInclusionLiteralExpression(builder, augmenters, true, true);
             }

            restartWithNewToken(builder);
          } else {
            builder.append(aChar);
           }

      }
    }
  };

  final ParserState detectingTokenType = new ParserState() {
    @Override
    public void onChar(final char aChar, final StringBuilder builder, final SolrQueryRequest request,
        final DocTransformers augmenters) throws SyntaxError {
      switch (aChar) {
        case HYPHEN:
          switchTo(collectingExclusionToken);
         break;
        case OPEN_BRACKET:
          builder.append(aChar);
          switchTo(collectingTransformer);
          break;
        case QUESTION_MARK:
        case WILDCARD:
         builder.append(aChar);
          switchTo(collectingInclusionGlob);
          break;
        case QUOTE:
        case DQUOTE:
          switchTo(collectingLiteral);
          currentState.onChar(aChar, builder, request, augmenters);
         break;
        default:
          if (isFieldListExpressionStart(aChar)) {
            builder.append(aChar);
            switchTo(maybeInclusionLiteralOrGlobOrFunction);
           }
      }
    }
  };
  
  // Parser state (initial state is "detecting token type").
  private ParserState currentState = detectingTokenType;
  

  void parse(final SolrQueryRequest request, final String... fieldLists) throws SyntaxError {
    final DocTransformers augmenters = new DocTransformers();
    final StringBuilder charBuffer = new StringBuilder();
    for (String fieldList : fieldLists) {
      for (int i = 0; i < fieldList.length(); i++) {
        final char aChar = fieldList.charAt(i);
        currentState.onChar(aChar, charBuffer, request, augmenters);
     }
 

      currentState.onChar(' ', charBuffer, request, augmenters);
    }
    
    if (augmenters.size() == 1) {
      transformer = augmenters.getTransformer(0);
    } else if (augmenters.size() > 1) {
      transformer = augmenters;
    }
  }
  
  private boolean wantsAllFields;
  
  private Set<String> luceneFieldNames;
  private Set<String> requestedFieldNames;
 
  private Set<String> inclusions;
  private Set<String> exclusions;
 private Set<String> inclusionGlobs;
  private Set<String> exclusionGlobs;
  
  protected DocTransformer transformer;
  
  private final Map<String,Boolean> cache = new HashMap<String,Boolean>();
 

  public SolrReturnFields() {
    this((String[]) null, null);
  }
 

  public SolrReturnFields(final SolrQueryRequest request) {
    this(request.getParams().getParams(CommonParams.FL), request);
  }

  public SolrReturnFields(final String[] fl, final SolrQueryRequest request) {
    if (fl == null || fl.length == 0) {
      wantsAllFields = true;
    } else {
         try {

          parse(request, fl);
        } catch (SyntaxError exception) {
          throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, exception.getMessage(), exception);
        }
      
      wantsAllFields |= ((inclusions == null && inclusionGlobs == null) && (exclusions == null && exclusionGlobs == null));
    }
  }

  public SolrReturnFields(final String fl, final SolrQueryRequest request) {
    this(new String[] {fl}, request);
    }

  void onFunction(final StringBuilder expressionBuffer, final DocTransformers augmenters, final SolrQueryRequest request)
      throws SyntaxError {
    
    final String alias = getExpressionAlias(expressionBuffer);
    final String function = getExpressionValue(expressionBuffer);
 
    final QParser parser = QParser.getParser(function, FunctionQParserPlugin.NAME, request);
    Query q = null;
 

    try {
      if (parser instanceof FunctionQParser) {
        FunctionQParser fparser = (FunctionQParser) parser;
        fparser.setParseMultipleSources(false);
        fparser.setParseToEnd(false);
        q = fparser.getQuery();
      } else {
        // A QParser that's not for function queries.
        // It must have been specified via local params.
        q = parser.getQuery();
        assert parser.getLocalParams() != null;
      }
      
      final ValueSource vs = (q instanceof FunctionQuery) ? ((FunctionQuery) q).getValueSource() : new QueryValueSource(q, 0.0f);
      
      String aliasThatWillBeUsed = alias;
 

      if (alias == null) {
        final SolrParams localParams = parser.getLocalParams();
        if (localParams != null) {
          aliasThatWillBeUsed = localParams.get("key");
       }
      }
      
      aliasThatWillBeUsed = (aliasThatWillBeUsed != null) ? aliasThatWillBeUsed : function;
 
     // Collect the function as it would be a literal
      onInclusionLiteralExpression(expressionBuffer, augmenters, false, false);
 

      augmenters.addTransformer(new ValueSourceAugmenter(aliasThatWillBeUsed, parser, vs));
    } catch (SyntaxError exception) {
      if (request.getSchema().getFieldOrNull(function) != null) {
        // OK, it was an oddly named field
        onInclusionLiteralExpression(expressionBuffer, augmenters, true, true);
      } else {
        throw new SolrException(SolrException.ErrorCode.BAD_REQUEST,
            "Error parsing fieldname: " + exception.getMessage(), exception);
      }
    }
  }
  
  
  
 
  void onInclusionLiteralExpression(final StringBuilder expressionBuffer, final DocTransformers augmenters, final boolean rename,
      final boolean isRealField) {
    final String alias = getExpressionAlias(expressionBuffer);
    final String fieldname = getExpressionValue(expressionBuffer);
    final String requestedName = (alias != null) ? alias : fieldname;
    
    requestedFieldNames().add(requestedName);
    inclusions().add(requestedName);
 

    if (SCORE.equals(fieldname)) {
     augmenters.addTransformer(new ScoreAugmenter((alias == null) ? SCORE : alias));
    } else {
 

      if (isRealField) {
        luceneFieldNames().add(fieldname);
        clearExclusions();
      }
       if (alias != null && rename) {
        augmenters.addTransformer(new RenameFieldTransformer(fieldname, alias, true));
     }
     }
   }


  void onExclusionLiteralFieldName(final StringBuilder fieldNameBuffer) throws SyntaxError {
    if ((inclusions == null || inclusions.isEmpty()) && (inclusionGlobs == null || inclusionGlobs.isEmpty())) {
      if (getExpressionAlias(fieldNameBuffer) == null)
      {
        wantsAllFields = false;
        exclusions().add(getExpressionValue(fieldNameBuffer));
      }
     }
  }
  
  
  /**
   * Logic to handle Custom as well as inbuilt Doc Transformer.
   * 
   */

  void onTransformer(final StringBuilder expressionBuffer, final SolrQueryRequest request, final DocTransformers augmenters)
      throws SyntaxError {
	  if(expressionBuffer!=null){
	   String fl_Content=expressionBuffer.toString();
		if(fl_Content.contains("[") && fl_Content.contains("]")){
						
			if(fl_Content.indexOf("[")==0){
				 //Logic to Parse Custom Transformers
				
				  final Map<String,String> augmenterCustomArgs = new HashMap<String,String>();
				  QueryParsing.parseLocalParams(expressionBuffer.toString(), 0, augmenterCustomArgs, request.getParams(), "[", CLOSE_BRACKET);
				  final String augmenterCustomName = augmenterCustomArgs.remove("type");
				  final String customDisp='['+augmenterCustomName+']';
				  final TransformerFactory customFactory = request.getCore().getTransformerFactory(augmenterCustomName);
			      if( customFactory != null ) {
			      MapSolrParams augmenterCustomParams = new MapSolrParams( augmenterCustomArgs );
			      augmenters.addTransformer( customFactory.create(customDisp, augmenterCustomParams, request) );
				
			}
			}else if(fl_Content.indexOf("[")>0 && fl_Content.contains(":[")){
				//Logic to Parse In_Built Transformers
				
			    final String alias = getExpressionAlias(expressionBuffer);
			    final String transfomerExpression = getExpressionValue(expressionBuffer);
			    final Map<String,String> augmenterArgs = new HashMap<String,String>();
			    QueryParsing.parseLocalParams(transfomerExpression, 0, augmenterArgs, request.getParams(), "[", CLOSE_BRACKET);
			    final String augmenterName = augmenterArgs.remove("type");
			    final String aliasThatWillBeUsed = (alias != null) ? alias : OPEN_BRACKET + augmenterName + CLOSE_BRACKET;
			    final TransformerFactory factory = request.getCore().getTransformerFactory(augmenterName);
			    if (factory != null) {
			    augmenters.addTransformer(factory.create(aliasThatWillBeUsed, new MapSolrParams(augmenterArgs), request));
			    onInclusionLiteralExpression(expressionBuffer, augmenters, false, false);	
						
						}
					}

				}

			}
		}



  
  

  void onInclusionGlob(final StringBuilder bufferChar) {
    final String glob = getExpressionValue(bufferChar);
 

    if (!ALL_FIELDS.equals(glob)) {
      inclusionGlobs().add(glob);
      clearExclusions();
    } else {
      // Special case: * is seen as an inclusion glob
     wantsAllFields = ((exclusions == null || exclusions.isEmpty()) && (exclusionGlobs == null || exclusionGlobs.isEmpty()));
    }
  }
  

  void onExclusionGlobExpression(final StringBuilder expressionBuffer) {
    if (getExpressionAlias(expressionBuffer) == null) {
      final String glob = getExpressionValue(expressionBuffer);
      
      if (!ALL_FIELDS.equals(glob) && (inclusions == null || inclusions.isEmpty())
          && (inclusionGlobs == null || inclusionGlobs.isEmpty())) {
          wantsAllFields = false;
          exclusionGlobs().add(glob);
      }
     }
   }
 
   @Override
  public Set<String> getLuceneFieldNames() {
    return (wantsAllFields || (luceneFieldNames == null || luceneFieldNames.isEmpty())) ? null : luceneFieldNames;
   }
 
   public Set<String> getRequestedFieldNames() {
    if (wantsAllFields || requestedFieldNames == null || requestedFieldNames.isEmpty()) {
       return null;
     }

    return requestedFieldNames;
 }
  
  @Override
  public boolean wantsField(final String name) {
    Boolean mustInclude = cache.get(name);
    if (mustInclude == null) // first time request for this field
    {
      if ((exclusions == null || exclusions.isEmpty()) && (exclusionGlobs == null || exclusionGlobs.isEmpty())) {
        mustInclude = wantsAllFields() || (inclusions != null && inclusions.contains(name))
            || (inclusionGlobs != null && wildcardMatch(name, inclusionGlobs));
      } else {
        mustInclude = !((exclusions != null && exclusions.contains(name)) || (exclusionGlobs != null && wildcardMatch(name,
            exclusionGlobs)));
      }
      cache.put(name, mustInclude);
    }
    return mustInclude;
  }
 
  @Override
  public boolean wantsAllFields() {
    return wantsAllFields;
  }
  
  @Override
  public boolean wantsScore() {
   return inclusions != null && inclusions.contains(SCORE);
   }
   
   public boolean hasPatternMatching() {

    return inclusionGlobs != null && !inclusionGlobs.isEmpty();
   }
 
   @Override
  public DocTransformer getTransformer() {
    return transformer;
  }
 

  private boolean wildcardMatch(final String name, final Set<String> globs) {
    for (final String glob : globs) {
      if (FilenameUtils.wildcardMatch(name, glob)) {
         return true;
       }
     }
     return false;
   }
   

  private String getExpressionAlias(final StringBuilder bufferChar)
   {

    final int indexOfColon = bufferChar.indexOf(ALIAS_VALUE_SEPARATOR);
    return indexOfColon == -1 ? null : bufferChar.substring(0, indexOfColon);
   }


  private String getExpressionValue(final StringBuilder bufferChar) {
    final int indexOfColon = bufferChar.indexOf(ALIAS_VALUE_SEPARATOR);
    return indexOfColon == -1 ? bufferChar.toString() : bufferChar.substring(indexOfColon + 1);
  }
  

  private void clearExclusions() {
    if (exclusions != null) {
      exclusions.clear();
    }
    
    if (exclusionGlobs != null) {
      exclusionGlobs.clear();
    }
   }
 

  private Set<String> inclusions() {
    if (inclusions == null) {
      inclusions = new HashSet<String>();
    }
    return inclusions;
   }


  private Set<String> inclusionGlobs() {
    if (inclusionGlobs == null) {
     inclusionGlobs = new HashSet<String>();
    }
    return inclusionGlobs;
  }
  

  private Set<String> exclusions() {
    if (exclusions == null) {
      exclusions = new HashSet<String>();
    }
    return exclusions;
  }
  

  private Set<String> exclusionGlobs() {
    if (exclusionGlobs == null) {
      exclusionGlobs = new HashSet<String>();
    }
    return exclusionGlobs;
  }
  

  private Set<String> luceneFieldNames() {
    if (luceneFieldNames == null) {
      luceneFieldNames = new HashSet<String>();
    }
    return luceneFieldNames;
  }

  private Set<String> requestedFieldNames() {
    if (requestedFieldNames == null) {
      requestedFieldNames = new LinkedHashSet<String>();
    }
    return requestedFieldNames;
  }
}