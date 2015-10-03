package org.apache.lucene.test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

public class CourtesyTitleFilter extends TokenFilter {
	
	Map<String,String> courtesyTitleMap = new	HashMap<String,String>();

	private CharTermAttribute termAttr;
	
	public CourtesyTitleFilter(TokenStream input) {
		super(input);
		termAttr = addAttribute(CharTermAttribute.class);
		courtesyTitleMap.put("Dr", "doctor");
		courtesyTitleMap.put("Mr", "mister");
		courtesyTitleMap.put("Mrs", "miss");
	}
	
	@Override
	public boolean incrementToken() throws IOException {
		if (!input.incrementToken())return false;
		
		String small = termAttr.toString();
		if(courtesyTitleMap.containsKey(small)) {
			termAttr.setEmpty().append(courtesyTitleMap.get(small));
		}
		return true;
	}
}


