package org.apache.lucene.analysis.ngram;


import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.reverse.ReverseStringFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;
import org.apache.lucene.analysis.util.CharacterUtils;
import org.apache.lucene.util.Version;

/**
 * Tokenizes the given token into n-grams of given size(s).
 * <p>
 * This {@link TokenFilter} create n-grams from the beginning edge or ending edge of a input token.
 * <p><a name="version"/>As of Lucene 4.4, this filter does not support
 * {@link Side#BACK} (you can use {@link ReverseStringFilter} up-front and
 * afterward to get the same behavior), handles supplementary characters
 * correctly and does not update offsets anymore.
 * 
 * @author kuntal
 */
public final class CustomNGramTokenFilter extends TokenFilter {
  public static final Side DEFAULT_SIDE = Side.FRONT;
  public static final int DEFAULT_MAX_GRAM_SIZE = 2;
  public static final int DEFAULT_MIN_GRAM_SIZE = 1;

  //tag for preserving original token--PATCH 
  public static final boolean DEFAULT_PRESERVE_ORIGINAL = false;

  /** Specifies which side of the input the n-gram should be generated from */
  public static enum Side {

    /** Get the n-gram from the front of the input */
    FRONT {
      @Override
      public String getLabel() { return "front"; }
    },

    /** Get the n-gram from the end of the input */
    @Deprecated
    BACK  {
      @Override
      public String getLabel() { return "back"; }
    };

    public abstract String getLabel();

    // Get the appropriate Side from a string
    public static Side getSide(String sideName) {
      if (FRONT.getLabel().equals(sideName)) {
        return FRONT;
      }
      if (BACK.getLabel().equals(sideName)) {
        return BACK;
      }
      return null;
    }
  }

  private final Version version;
  private final CharacterUtils charUtils;
  private final int minGram;
  private final int maxGram;
  
  //tag for preserving original token --PATCH 
  private final boolean preserveOriginal;
  private boolean isOriginalPreserved = false;
  private Side side;
  private char[] curTermBuffer;
  private int curTermLength;
  private int curGramSize;
  private int tokStart;
  private int tokEnd; // only used if the length changed before this filter
  private boolean updateOffsets; // never if the length changed before this filter
  private int curPos;
  private boolean hasIllegalOffsets; // only if the length changed before this filter
  private int savePosIncr;
  private int savePosLen;
  
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
  private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
  private final PositionLengthAttribute posLenAtt = addAttribute(PositionLengthAttribute.class);

  /**
   * Creates CustomNGramFilterFactory that can generate n-grams in the sizes of the given range
   *
   * @param version the <a href="#version">Lucene match version</a>
   * @param input {@link TokenStream} holding the input to be tokenized
   * @param side the {@link Side} from which to chop off an n-gram
   * @param minGram the smallest n-gram to generate
   * @param maxGram the largest n-gram to generate
   * @param preserveOriginal to store the original token based on its value
   */
  @Deprecated
  public CustomNGramTokenFilter(Version version, TokenStream input, Side side, int minGram, int maxGram,boolean preserveOriginal) {
    super(input);

    if (version == null) {
      throw new IllegalArgumentException("version must not be null");
    }

    if (version.onOrAfter(Version.LUCENE_42) && side == Side.BACK) {
      throw new IllegalArgumentException("Side.BACK is not supported anymore as of Lucene 4.4, use ReverseStringFilter up-front and afterward");
    }

    if (side == null) {
      throw new IllegalArgumentException("sideLabel must be either front or back");
    }

    if (minGram < 1) {
      throw new IllegalArgumentException("minGram must be greater than zero");
    }

    if (minGram > maxGram) {
      throw new IllegalArgumentException("minGram must not be greater than maxGram");
    }

    this.version = version;
    this.charUtils = version.onOrAfter(Version.LUCENE_4_9)
        ? CharacterUtils.getInstance(version)
        : CharacterUtils.getJava4Instance();
    this.minGram = minGram;
    this.maxGram = maxGram;
   
    //tag for preserving original token --PATCH 
    this.preserveOriginal = preserveOriginal;
    this.side = side;
  }

  /**
   * Creates CustomNGramFilterFactory that can generate n-grams in the sizes of the given range
   *
   * @param version the <a href="#version">Lucene match version</a>
   * @param input {@link TokenStream} holding the input to be tokenized
   * @param sideLabel the name of the {@link Side} from which to chop off an n-gram
   * @param minGram the smallest n-gram to generate
   * @param maxGram the largest n-gram to generate
   * @param preserveOriginal to store the original token based on its value
   */
  @Deprecated
  public CustomNGramTokenFilter(Version version, TokenStream input, String sideLabel, int minGram, int maxGram,boolean preserveOriginal) {
    this(version, input, Side.getSide(sideLabel), minGram, maxGram,preserveOriginal);
    
  }

  /**
   * Creates CustomNGramFilterFactory that can generate n-grams in the sizes of the given range
   *
   * @param version the <a href="#version">Lucene match version</a>
   * @param input {@link TokenStream} holding the input to be tokenized
   * @param minGram the smallest n-gram to generate
   * @param maxGram the largest n-gram to generate
   * @param preserveOriginal to store the original token based on its value
   */
  public CustomNGramTokenFilter(Version version, TokenStream input, int minGram, int maxGram,boolean preserveOriginal) {
	  
    this(version, input, Side.FRONT, minGram, maxGram,preserveOriginal);
    
  }

  @Override
  public final boolean incrementToken() throws IOException {
	  
	  while (true) {
	      if (curTermBuffer == null) {
	        if (!input.incrementToken()) {
	          return false;
	        } else {
	          curTermBuffer = termAtt.buffer().clone();
	          curTermLength = termAtt.length();
	          curGramSize = minGram;
	          curPos = 0;
	          tokStart = offsetAtt.startOffset();
	          tokEnd = offsetAtt.endOffset();
	          // if length by start + end offsets doesn't match the term text then assume
	          // this is a synonym and don't adjust the offsets.
	          hasIllegalOffsets = (tokStart + curTermLength) != tokEnd;
	          savePosIncr += posIncrAtt.getPositionIncrement();
		  savePosLen = posLenAtt.getPositionLength();
	          isOriginalPreserved = false;
	        }
	      }
	      
	      /*
	       * Patching preserving original token-START
	       */
	      //Looping done to iterate the entire input and tokenize the input based on min/maxgram size.
	      while (curGramSize <= maxGram) {
	        while (curPos+curGramSize <= curTermLength) {     // while there is input
	          clearAttributes();
	          termAtt.copyBuffer(curTermBuffer, curPos, curGramSize);
	          if (hasIllegalOffsets) {
	            offsetAtt.setOffset(tokStart, tokEnd);
	          } else {
	            offsetAtt.setOffset(tokStart + curPos, tokStart + curPos + curGramSize);
	          }
	          curPos++;
	          return true;
	        }
	        curGramSize++;                         // increase n-gram size
	        curPos = 0;
	      }
	     
     
            if(preserveOriginal && !isOriginalPreserved && ((curTermLength < minGram) || (curTermLength > maxGram))) {
            	
               // whole term will be included so curGramSize is curTermLength
               curGramSize = curTermLength;
               // grab gramSize chars from front or back
               final int start = side == Side.FRONT ? 0 : charUtils.offsetByCodePoints(curTermBuffer, 0, curTermLength, curTermLength, -curGramSize);
               final int end = charUtils.offsetByCodePoints(curTermBuffer, 0, curTermLength, start, curGramSize);
               clearAttributes();
               if (!hasIllegalOffsets) {
                 offsetAtt.setOffset(tokStart + start, tokStart + end);
               } else {
                 offsetAtt.setOffset(tokStart, tokEnd);
               }
               if(curTermLength < minGram) {
                 posIncrAtt.setPositionIncrement(savePosIncr);
                 savePosIncr = 0;
               //} else if(curTermLength > maxGram) {
               // then curTermLength > maxGram
               } else {
                 posIncrAtt.setPositionIncrement(0);
               }
               posLenAtt.setPositionLength(savePosLen);
               termAtt.copyBuffer(curTermBuffer, start, end - start);
               curGramSize++;
               isOriginalPreserved = true;
               return true;
            }
             curTermBuffer = null;
           }
}
  /*
   * Patching preserving original token-END
   */


  @Override
  public void reset() throws IOException {
    super.reset();
    curTermBuffer = null;
    savePosIncr = 0;
  }
}
