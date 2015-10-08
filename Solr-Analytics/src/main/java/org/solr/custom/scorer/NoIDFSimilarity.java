package org.solr.custom.scorer;

import org.apache.lucene.search.similarities.DefaultSimilarity;

/**
 * Example to show NO IDF implementation.
 * Sometimes it is not meaningless to apply high score based on rareness of term (such as on fields like names,emails)
 * @author kuntal
 *
 */

public class NoIDFSimilarity extends DefaultSimilarity {
	
	
	@Override
	public float idf(long docFreq, long numDocs) {
		
		//Default Inverse Document Frequency is Overrriden
		//return (float)(Math.log(numDocs/(double)(docFreq+1)) + 1.0);
		return 1.0f;
	}
}
