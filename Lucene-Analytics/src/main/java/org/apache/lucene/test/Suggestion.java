package org.apache.lucene.test;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.analysis.hunspell.Dictionary;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.spell.LuceneDictionary;
import org.apache.lucene.search.suggest.Lookup;
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.search.suggest.analyzing.AnalyzingSuggester;
import org.apache.lucene.search.suggest.analyzing.FreeTextSuggester;
import org.apache.lucene.search.suggest.analyzing.FuzzySuggester;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

public class Suggestion {

	public static void main(String[] args) throws IOException {

		StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_4_9);
		Directory directory = new RAMDirectory();
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_9,analyzer);
		IndexWriter indexWriter = new IndexWriter(directory, config);

		Document doc = new Document();
		doc.add(new StringField("content", "Humpty Dumpty sat on a wall",	Field.Store.YES));
		indexWriter.addDocument(doc);

		doc = new Document();
		doc.add(new StringField("content", "Humpty Dumpty had a great	fall", Field.Store.YES));
		indexWriter.addDocument(doc);

		doc = new Document();
		doc.add(new StringField("content", "All the king's horses and all	the king's men", Field.Store.YES));
		indexWriter.addDocument(doc);

		doc = new Document();
		doc.add(new StringField("content", "Couldn't put Humpty together	again", Field.Store.YES));
		indexWriter.addDocument(doc);

		indexWriter.commit();
		indexWriter.close();
		IndexReader indexReader = DirectoryReader.open(directory);
		LuceneDictionary dictionary = new LuceneDictionary(indexReader,"content");

		AnalyzingSuggester analyzingSuggester = new AnalyzingSuggester(new	StandardAnalyzer(Version.LUCENE_4_9));
		analyzingSuggester.build(dictionary);
		List<Lookup.LookupResult> lookupResultList = analyzingSuggester.lookup("humpty dum", false, 10);

		for (Lookup.LookupResult lookupResult : lookupResultList) {
			System.out.println(lookupResult.key + ": " +lookupResult.value);
		}

		AnalyzingInfixSuggester analyzingInfixSuggester = new AnalyzingInfixSuggester(Version.LUCENE_4_9, directory, analyzer);
		analyzingInfixSuggester.build(dictionary);
		List<Lookup.LookupResult> infixlookupResultList =analyzingInfixSuggester.lookup("put h", false, 10);
		for (Lookup.LookupResult lookupResult : infixlookupResultList) {
			System.out.println(lookupResult.key + ": " +lookupResult.value);
		}

		FreeTextSuggester freeTextSuggester = new FreeTextSuggester(analyzer,analyzer, 3);
		freeTextSuggester.build(dictionary);
		List<Lookup.LookupResult> freetextlookupResultList = freeTextSuggester.	lookup("h", false, 10);
		for (Lookup.LookupResult lookupResult : lookupResultList) {
			System.out.println(lookupResult.key + ": " + lookupResult.value);
		}

		FuzzySuggester fuzzySuggester = new FuzzySuggester(analyzer);
		fuzzySuggester.build(dictionary);
		List<Lookup.LookupResult> fuzzylookupResultList =fuzzySuggester.lookup("hampty", false, 10);
		for (Lookup.LookupResult lookupResult : fuzzylookupResultList) {
			System.out.println(lookupResult.key + ": " +lookupResult.value);
		}
	}

}
