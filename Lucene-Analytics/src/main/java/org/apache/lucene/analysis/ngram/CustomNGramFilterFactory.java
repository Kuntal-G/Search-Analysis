package org.apache.lucene.analysis.ngram;



import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

/**
 * 
 * @author kuntal
 *
 */

public class CustomNGramFilterFactory extends TokenFilterFactory {
	 
	private int maxGramSize = 0;
	private int minGramSize = 0;
    private String side;
    
    //tag for preserving original token --PATCH 
    private  boolean preserveOriginal;

  

	protected CustomNGramFilterFactory(Map<String, String> args) {
		super(args);
			    String maxArg = args.get("maxGramSize");
			    maxGramSize = (maxArg != null ? Integer.parseInt(maxArg): CustomNGramTokenFilter.DEFAULT_MAX_GRAM_SIZE);

			    String minArg = args.get("minGramSize");
			    minGramSize = (minArg != null ? Integer.parseInt(minArg): CustomNGramTokenFilter.DEFAULT_MIN_GRAM_SIZE);
			    
			  //tag for preserving original token --PATCH 
			   String preserveOrig = args.get("preserveOriginal");
			   preserveOriginal =(preserveOrig!=null? Boolean.parseBoolean(preserveOrig): CustomNGramTokenFilter.DEFAULT_PRESERVE_ORIGINAL);
			   
			    side = args.get("side");
			    if (side == null) {
			      side = CustomNGramTokenFilter.Side.FRONT.getLabel();
			    }
			}

  

  @Override
  public CustomNGramTokenFilter create(TokenStream input) {
	 return new CustomNGramTokenFilter(luceneMatchVersion, input, side, minGramSize, maxGramSize, preserveOriginal);
  }
}
