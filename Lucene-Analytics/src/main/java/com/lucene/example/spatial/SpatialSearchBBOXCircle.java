package com.lucene.example.spatial;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.spatial.SpatialStrategy;
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.spatial.query.SpatialArgs;
import org.apache.lucene.spatial.query.SpatialOperation;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;

import com.spatial4j.core.context.SpatialContext;
import com.spatial4j.core.shape.Shape;

/**
 * Spatial Search with Bounding Box(BBOX)and Circle using Lucene
 * @author kuntal
 *
 */

public class SpatialSearchBBOXCircle {

	private IndexWriter indexWriter;
	private IndexReader indexReader;
	private IndexSearcher searcher;
	private SpatialContext ctx;
	private SpatialStrategy strategy;

	public SpatialSearchBBOXCircle(String indexPath) {

		StandardAnalyzer a = new StandardAnalyzer(Version.LUCENE_4_9);
		IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_4_9, a);
		Directory directory;

		try {
			directory = new SimpleFSDirectory(new File(indexPath));
			indexWriter = new IndexWriter(directory, iwc);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		this.ctx = SpatialContext.GEO;

		SpatialPrefixTree grid = new GeohashPrefixTree(ctx, 11);
		this.strategy = new RecursivePrefixTreeStrategy(grid, "location");
	}

	public void indexDocuments() throws IOException {

		indexWriter.addDocument(newGeoDocument(1, "Bangalore", ctx.makePoint(12.9558, 77.620979)));
		indexWriter.addDocument(newGeoDocument(2, "Cubbon Park", ctx.makePoint(12.974045, 77.591995)));
		indexWriter.addDocument(newGeoDocument(3, "Tipu palace", ctx.makePoint(12.959365, 77.573792)));
		indexWriter.addDocument(newGeoDocument(4, "Bangalore palace", ctx.makePoint(12.998095, 77.592041)));
		indexWriter.addDocument(newGeoDocument(5, "Monkey Bar", ctx.makePoint(12.97018, 77.61219)));
		indexWriter.addDocument(newGeoDocument(6, "Chennai", ctx.makePoint(13.060422, 80.249583)));
		indexWriter.addDocument(newGeoDocument(7, "Elliot's Beach", ctx.makePoint(12.998976, 80.271286))); 
		indexWriter.addDocument(newGeoDocument(8, "Kapaleeshwar Temple", ctx.makePoint(13.033889, 80.269722)));


		indexWriter.commit();
		indexWriter.close();
	}


	private Document newGeoDocument(int id, String name, Shape shape) {

		FieldType ft = new FieldType();
		ft.setIndexed(true);
		ft.setStored(true);

		Document doc = new Document();

		doc.add(new IntField("id", id, Store.YES));
		doc.add(new Field("name", name, ft));
		for(IndexableField f:strategy.createIndexableFields(shape)) {
			doc.add(f);
		}

		doc.add(new StoredField(strategy.getFieldName(), ctx.toString(shape)));

		return doc;
	}

	public void setSearchIndexPath(String indexPath) throws IOException{
		this.indexReader = DirectoryReader.open(new SimpleFSDirectory(new File(indexPath)));
		this.searcher = new IndexSearcher(indexReader);
	}

	/**
	 * Search using BBox
	 * @param minLat
	 * @param minLng
	 * @param maxLat
	 * @param maxLng
	 * @throws IOException
	 */
	public void searchBBox(Double minLat, Double minLng, Double maxLat, Double maxLng) throws IOException {

		SpatialArgs args = new SpatialArgs(SpatialOperation.IsWithin, ctx.makeRectangle(minLat, maxLat, minLng, maxLng));

		Filter filter = strategy.makeFilter(args);
		int limit = 10; 
		TopDocs topDocs = searcher.search(new MatchAllDocsQuery(), filter, limit);

		ScoreDoc[] scoreDocs = topDocs.scoreDocs; 
		for (ScoreDoc s : scoreDocs) { 
			Document doc = searcher.doc(s.doc); 
			System.out.println(doc.get("id") + "\t" + doc.get("name")); 
		} 
	}

	/**
	 * Search using Circle
	 * @param lat
	 * @param lng
	 * @param distance
	 * @throws IOException
	 */

	public void searchCircle(Double lat, Double lng, Double distance) throws IOException {

		SpatialArgs args = new SpatialArgs(SpatialOperation.IsWithin, ctx.makeCircle(lat,lng, distance));

		Filter filter = strategy.makeFilter(args);
		int limit = 10; 
		TopDocs topDocs = searcher.search(new MatchAllDocsQuery(), filter, limit);

		ScoreDoc[] scoreDocs = topDocs.scoreDocs; 
		for (ScoreDoc s : scoreDocs) { 
			Document doc = searcher.doc(s.doc); 
			System.out.println(doc.get("id") + "\t" + doc.get("name")); 
		} 
	}
	

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		String indexPath = "/home/kuntal/indexPath/spatialbbox3";

		SpatialSearchBBOXCircle s = new SpatialSearchBBOXCircle(indexPath);

		//Indexes sample documents
		s.indexDocuments();
		s.setSearchIndexPath(indexPath);

		//Get Places Within Chennai Bounding Box. 
		System.out.println("Places WithIn Chennai Bounding Box\n"); 
		s.searchBBox(12.9673, 80.184631, 13.15148, 80.306709); 


		//Get Places Within Bangalore Bounding Box.
		System.out.println("Places WithIn Bangalore Bounding Box"); 
		s.searchBBox(12.76805, 77.465202, 13.14355, 77.776749);
		
		
		
		System.out.println("Places WithIn Chennai Circle\n"); 
		s.searchCircle(12.9673, 80.184631,2.0); 


		//Get Places Within Bangalore Bounding Box.
		System.out.println("Places WithIn Bangalore Circle\n"); 
		s.searchCircle(12.76805, 77.465202,2.0);

	}

}