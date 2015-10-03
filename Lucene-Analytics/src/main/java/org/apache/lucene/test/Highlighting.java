package org.apache.lucene.test;

import java.io.IOException;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
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
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLEncoder;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

public class Highlighting {

	public static void main(String[] args) throws IOException, InvalidTokenOffsetsException {


		StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_4_9);
		Directory directory = new RAMDirectory();
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_9,analyzer);
		IndexWriter indexWriter = new IndexWriter(directory, config);

		Document doc = new Document();
		doc.add(new TextField("content", "Humpty Dumpty sat on a wall",	Field.Store.YES));
		indexWriter.addDocument(doc);

		doc = new Document();
		doc.add(new TextField("content", "Humpty Dumpty had a great fall",Field.Store.YES));
		indexWriter.addDocument(doc);

		doc = new Document();
		doc.add(new TextField("content", "All the king's horses and all	the king's men", Field.Store.YES));
		indexWriter.addDocument(doc);

		doc = new Document();
		doc.add(new TextField("content", "Couldn't put Humpty together	again", Field.Store.YES));
		indexWriter.addDocument(doc);
		indexWriter.commit();
		indexWriter.close();

		IndexReader indexReader = DirectoryReader.open(directory);
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);

		Query query = new TermQuery(new Term("content", "humpty"));
		TopDocs topDocs = indexSearcher.search(query, 10);

		SimpleHTMLFormatter simpleHTMLFormatter = new	SimpleHTMLFormatter("<strong>", "</strong>");
		SimpleHTMLEncoder simpleHTMLEncoder = new SimpleHTMLEncoder();
		Highlighter highlighter = new Highlighter(simpleHTMLFormatter,simpleHTMLEncoder, new QueryScorer(query));

		for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
			doc = indexSearcher.doc(scoreDoc.doc);
			String text = doc.get("content");
			TokenStream tokenStream =
					TokenSources.getAnyTokenStream(indexReader, scoreDoc.doc,"content", analyzer);

			TextFragment[] textFragments =	highlighter.getBestTextFragments(tokenStream, text, false,	10);
			for (TextFragment textFragment : textFragments) {
				if (textFragment != null && textFragment.getScore() > 0) {
					System.out.println(textFragment.toString());
				}
			}
		}
	}

}
