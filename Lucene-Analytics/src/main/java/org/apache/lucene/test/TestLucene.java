package org.apache.lucene.test;

import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.util.Version;

public class TestLucene {

	public static void main(String[] args) {
		StringReader reader = new StringReader("Lucene is mainly used for information retrieval and you can read more about it at lucene.apache.org.");
				StandardAnalyzer wa = new StandardAnalyzer(Version.LUCENE_4_9);
				TokenStream ts = null;
				try {
					ts = wa.tokenStream("field", reader);
					OffsetAttribute offsetAtt = ts.addAttribute(OffsetAttribute.class);
					CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);
					ts.reset();
					while (ts.incrementToken()) {
						String token = termAtt.toString();
						System.out.println("[" + token + "]");
						System.out.println("Token starting offset: " + offsetAtt.
								startOffset());
						System.out.println(" Token ending offset: " + offsetAtt.
								endOffset());
						System.out.println("");
					}
					ts.end();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						ts.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					wa.close();
				}
	}

}
