package org.apache.lucene.test;

import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.join.JoinUtil;
import org.apache.lucene.search.join.ScoreMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

public class QueryTimeJoin {

	public static void main(String[] args) throws IOException {

		StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_4_9);
		Directory directory = new RAMDirectory();
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_9,analyzer);
		IndexWriter indexWriter = new IndexWriter(directory, config);
		
		Document doc = new Document();
		doc.add(new StringField("name", "A Book", Field.Store.YES));
		doc.add(new StringField("type", "book", Field.Store.YES));
		doc.add(new LongField("bookAuthorId", 1, Field.Store.YES));
		doc.add(new LongField("bookId", 1, Field.Store.YES));
		indexWriter.addDocument(doc);
		
		doc = new Document();
		doc.add(new StringField("name", "An Author", Field.Store.YES));
		doc.add(new StringField("type", "author", Field.Store.YES));
		doc.add(new LongField("authorId", 1, Field.Store.YES));
		indexWriter.addDocument(doc);
		
		indexWriter.commit();

		IndexReader indexReader = DirectoryReader.open(directory);
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);
		
		String fromField = "bookAuthorId";
		boolean multipleValuesPerDocument = false;
		String toField = "authorId";
		
		ScoreMode scoreMode = ScoreMode.Max;
		
		Query fromQuery = new TermQuery(new Term("type", "book"));
		Query joinQuery = JoinUtil.createJoinQuery(
				fromField,
				multipleValuesPerDocument,
				toField,
				fromQuery,
				indexSearcher,
				scoreMode);
		
		TopDocs topDocs = indexSearcher.search(joinQuery, 10);
		
		System.out.println("Total hits: " + topDocs.totalHits);
		
		for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
			doc = indexReader.document(scoreDoc.doc);
			System.out.println(scoreDoc.score + ": " +doc.getField("name").stringValue());
		}

	}

}
