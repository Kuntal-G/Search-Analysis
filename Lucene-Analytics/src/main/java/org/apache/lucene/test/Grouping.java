package org.apache.lucene.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.CachingWrapperFilter;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.grouping.BlockGroupingCollector;
import org.apache.lucene.search.grouping.GroupDocs;
import org.apache.lucene.search.grouping.TopGroups;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

public class Grouping {

	public static void main(String[] args) throws IOException{


		StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_4_9);
		Directory directory = new RAMDirectory();
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_9,analyzer);
		IndexWriter indexWriter = new IndexWriter(directory, config);
		
		
		FieldType groupEndFieldType = new FieldType();
		groupEndFieldType.setStored(false);
		groupEndFieldType.setTokenized(false);
		groupEndFieldType.setIndexed(true);
		groupEndFieldType.setIndexOptions(FieldInfo.IndexOptions.DOCS_ONLY);
		groupEndFieldType.setOmitNorms(true);
		
		Field groupEndField = new Field("groupEnd", "x",groupEndFieldType);
		
		List<Document> documentList = new ArrayList();
		
		Document doc = new Document();
		doc.add(new StringField("BookId", "B1", Field.Store.YES));
		doc.add(new StringField("Category", "Cat 1", Field.Store.YES));
		documentList.add(doc);
		
		doc = new Document();
		doc.add(new StringField("BookId", "B2", Field.Store.YES));
		doc.add(new StringField("Category", "Cat 1", Field.Store.YES));
		documentList.add(doc);
		doc.add(groupEndField);
		
		indexWriter.addDocuments(documentList);
		documentList = new ArrayList();
		doc = new Document();
		doc.add(new StringField("BookId", "B3", Field.Store.YES));
		doc.add(new StringField("Category", "Cat 2", Field.Store.YES));
		documentList.add(doc);
		doc.add(groupEndField);
		indexWriter.addDocuments(documentList);
		indexWriter.commit();
		
		
		Filter groupEndDocs = new CachingWrapperFilter(new	QueryWrapperFilter(new TermQuery(new Term("groupEnd", "x"))));
		IndexReader indexReader = DirectoryReader.open(directory);
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);
		BlockGroupingCollector blockGroupingCollector = new	BlockGroupingCollector(Sort.RELEVANCE, 10, true, groupEndDocs);
		indexSearcher.search(new MatchAllDocsQuery(), null,	blockGroupingCollector);
		
		TopGroups topGroups = blockGroupingCollector.getTopGroups(Sort.	RELEVANCE, 0, 0, 10,true);
		System.out.println("Total group count: " +	topGroups.totalGroupCount);
		System.out.println("Total group hit count: " +	topGroups.totalGroupedHitCount);
		
		for (GroupDocs groupDocs : topGroups.groups) {
			System.out.println("Group: " + groupDocs.groupValue);
			
			for (ScoreDoc scoreDoc : groupDocs.scoreDocs) {
				doc = indexSearcher.doc(scoreDoc.doc);
				System.out.println("Category: " +doc.getField("Category").stringValue() + ", BookId: " +doc.getField("BookId").stringValue());
			}
		}
	}

}
