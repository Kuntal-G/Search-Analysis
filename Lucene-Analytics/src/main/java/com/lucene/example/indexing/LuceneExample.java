/*package com.lucene.example.indexing;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.AnalyzerWrapper;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

*//**
 * Sample example to test custom Analyzer with Custom Ngram Filter Factory
 * Use it for multivalued,Multi Analyzer 
 * @author kuntal
 *
 *//*
public class LuceneExample {
	public static final String INDEX_DIRECTORY = "/Volumes/ramdisk/";
	public static void main(String[] args) {
		
	}
	
	private void indexing(){
		File indexDir = new File(INDEX_DIRECTORY);
		
		Directory directory = FSDirectory.open(indexDir);
		Analyzer defaultAnalyzer = new StandardAnalyzer(Version.LUCENE_4_9);
		
		Map<String,Analyzer> analyzersMap=new HashMap<String, Analyzer>();
		analyzersMap.put("keyword", new KeywordAnalyzer());
		analyzersMap.put("whitespace", new WhitespaceAnalyzer(Version.LUCENE_4_9));
		
		PerFieldAnalyzerWrapper anlyzerWrapper=new PerFieldAnalyzerWrapper(defaultAnalyzer, analyzersMap);
		IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_4_9, anlyzerWrapper);
		IndexWriter writer = new IndexWriter(directory, conf);
		Document doc = new Document();
		//doc.add(new Field("file", fileName, Store.YES, Index.NO));
		
		
		writer.commit();
		writer.deleteUnusedFiles();
		
		System.out.println(writer.maxDoc() + " documents written");
		
	}
private void searching(){
	
}


}
*/