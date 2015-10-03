package org.apache.lucene.test;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

public class GenderFilter extends TokenFilter {
	
	GenderAttribute genderAtt = addAttribute(GenderAttribute.class);
	CharTermAttribute charTermAtt = addAttribute(CharTermAttribute.class);
	
	protected GenderFilter(TokenStream input) {
		super(input);
	}
	
	public boolean incrementToken() throws IOException {
		if (!input.incrementToken()) return false; 
		genderAtt.setGender(determineGender(charTermAtt.toString()));
		return true;
	}
	
	protected GenderAttribute.Gender determineGender(String term) {
		if (term.equalsIgnoreCase("mr") || term.equalsIgnoreCase("mister")) {
			return GenderAttribute.Gender.Male;
		} else if (term.equalsIgnoreCase("mrs") || term.equalsIgnoreCase("misters")) {
			return GenderAttribute.Gender.Female;
		}
		return GenderAttribute.Gender.Undefined;
	}
}