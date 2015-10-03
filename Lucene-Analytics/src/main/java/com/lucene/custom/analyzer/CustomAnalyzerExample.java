package com.lucene.custom.analyzer;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
 
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

public class CustomAnalyzerExample {
	
	private final Version version = Version.LUCENE_4_9;
	 
	public void run() throws IOException {
		Directory index = new RAMDirectory();
		
		Map<String, Analyzer> analyzerPerField = new HashMap<String, Analyzer>();
		analyzerPerField.put("email", new KeywordAnalyzer());
		analyzerPerField.put("specials", new ECharacterAnalyser(version));
		PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(	new StandardAnalyzer(version), analyzerPerField);
		
		IndexWriterConfig config = new IndexWriterConfig(version, analyzer).setOpenMode(OpenMode.CREATE);
		IndexWriter writer = new IndexWriter(index, config);
		
		Document doc = new Document();
		doc.add(new TextField("author", "kitty cat", Store.YES));
		doc.add(new TextField("email", "kitty@cat.com", Store.YES));
		doc.add(new TextField("email", "kitty2@cat.com", Store.YES));
		doc.add(new TextField("specials", "13e12exoxoe45e66", Store.YES));
		writer.addDocument(doc);
		writer.commit();
		writer.close();
 
		int limit = 20;
		try (IndexReader reader = DirectoryReader.open(index)) {
			Query query = new TermQuery(new Term("email", "kitty@cat.com"));
			printSearchResults(limit, query, reader);
 
			query = new TermQuery(new Term("specials", "xoxo"));
			printSearchResults(limit, query, reader);
 
			query = new TermQuery(new Term("author", "kitty"));
			printSearchResults(limit, query, reader);
		}
 
		index.close();
	}
 
	private void printSearchResults(final int limit, final Query query,	final IndexReader reader) throws IOException {
		IndexSearcher searcher = new IndexSearcher(reader);
		TopDocs docs = searcher.search(query, limit);
 
		System.out.println(docs.totalHits + " found for query: " + query);
 
		for (final ScoreDoc scoreDoc : docs.scoreDocs) {
			System.out.println(searcher.doc(scoreDoc.doc));
		}
	}
 
	public static void main(final String[] args) throws IOException {
		new CustomAnalyzerExample().run();
	}
}