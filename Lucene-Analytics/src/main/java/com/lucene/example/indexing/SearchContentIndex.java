package com.lucene.example.indexing;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;


/**
 * Sample application for searching an index
 * @author kuntal
 *
 */
public class SearchContentIndex {

	/**
	 * @param args
	 * @throws ParseException 
	 */
	public static void main(String[] args) {
		try {
			SearchContentIndex.searchIndex();
		} catch (IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

	private static void searchIndex() throws IOException, ParseException{
		File indexDir = new File(WriteContentIndex.INDEX_DIRECTORY);

		Directory index = FSDirectory.open(indexDir);

		// Build a Query object
		Query query;
		query = new QueryParser(Version.LUCENE_4_9, "text", new StandardAnalyzer(Version.LUCENE_4_9)).parse("RDBMS");

		int hitsPerPage = 10;
		IndexReader reader = IndexReader.open(index);
		IndexSearcher searcher = new IndexSearcher(reader);
		TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage, true);
		searcher.search(query, collector);

		System.out.println("total hits: " + collector.getTotalHits());		

		ScoreDoc[] hits = collector.topDocs().scoreDocs;
		for (ScoreDoc hit : hits) {
			Document doc = reader.document(hit.doc);
			System.out.println(doc.get("file") + "  (" + hit.score + ")");
		}

	}

}
