package com.lucene.custom.analyzer;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.util.Version;
 
public class ECharacterAnalyser extends Analyzer {
	private final Version version;
 
	public ECharacterAnalyser(final Version version) {
		this.version = version;
	}
 
	@Override
	protected TokenStreamComponents createComponents(final String field,final Reader reader) {
		Tokenizer tokenizer = new ECharacterTokenizer(version, reader);
		TokenStream filter = new LowerCaseFilter(version, tokenizer);
		return new TokenStreamComponents(tokenizer, filter);
	}
}
