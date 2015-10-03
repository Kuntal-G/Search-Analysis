package org.apache.lucene.test;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LetterTokenizer;
import org.apache.lucene.util.Version;

public class CourtesyTitleAnalyzer extends Analyzer {
	
	@Override
	protected TokenStreamComponents createComponents(String	fieldName, Reader reader) {
		
		Tokenizer letterTokenizer = new LetterTokenizer(Version.LUCENE_4_9, reader);
		TokenStream filter = new CourtesyTitleFilter(letterTokenizer);
		return new TokenStreamComponents(letterTokenizer,filter);
	}
}
