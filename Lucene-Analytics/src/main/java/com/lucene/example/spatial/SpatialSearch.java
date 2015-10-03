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
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
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
import com.spatial4j.core.distance.DistanceUtils;
import com.spatial4j.core.shape.Point;
import com.spatial4j.core.shape.Shape;

/**
 * Spatial Search using Lucene
 * @author kuntal
 *
 */

public class SpatialSearch {

	private IndexWriter indexWriter;
	private IndexReader indexReader;
	private IndexSearcher searcher;
	private SpatialContext ctx;
	private SpatialStrategy strategy;

	public SpatialSearch(String indexPath) {

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
//Replace with value in Kolkata
		indexWriter.addDocument(newGeoDocument(1, "Bangalore", ctx.makePoint(12.9558, 77.620979)));
		indexWriter.addDocument(newGeoDocument(2, "Cubbon Park", ctx.makePoint(12.974045, 77.591995)));
		indexWriter.addDocument(newGeoDocument(3, "Tipu palace", ctx.makePoint(12.959365, 77.573792)));
		indexWriter.addDocument(newGeoDocument(4, "Bangalore palace", ctx.makePoint(12.998095, 77.592041)));
		indexWriter.addDocument(newGeoDocument(5, "Monkey Bar", ctx.makePoint(12.97018, 77.61219)));
		indexWriter.addDocument(newGeoDocument(6, "Cafe Cofee day", ctx.makePoint(12.992189, 80.2348618)));

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





	public void search(Double lat, Double lng, int distance) throws IOException{

		Point p = ctx.makePoint(lat, lng);
		SpatialArgs args = new SpatialArgs(SpatialOperation.Intersects,
				ctx.makeCircle(lat, lng, DistanceUtils.dist2Degrees(distance, DistanceUtils.EARTH_MEAN_RADIUS_KM)));
		Filter filter = strategy.makeFilter(args);

		ValueSource valueSource = strategy.makeDistanceValueSource(p);
		Sort distSort = new Sort(valueSource.getSortField(false)).rewrite(searcher);

		int limit = 10;
		TopDocs topDocs = searcher.search(new MatchAllDocsQuery(), filter, limit, distSort);
		ScoreDoc[] scoreDocs = topDocs.scoreDocs;

		for(ScoreDoc s: scoreDocs) {

			Document doc = searcher.doc(s.doc);
			Point docPoint = (Point) ctx.readShape(doc.get(strategy.getFieldName()));
			double docDistDEG = ctx.getDistCalc().distance(args.getShape().getCenter(), docPoint);
			double docDistInKM = DistanceUtils.degrees2Dist(docDistDEG, DistanceUtils.EARTH_EQUATORIAL_RADIUS_KM);
			System.out.println(doc.get("id") + "\t" + doc.get("name") + "\t" + docDistInKM + " km ");

		}

	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		String indexPath = "/home/kuntal/indexPath/spatial";

		SpatialSearch s = new SpatialSearch(indexPath);

		//Indexes sample documents
		s.indexDocuments();
		s.setSearchIndexPath(indexPath);

		/*Get Places Within 4 kilometers from cubbon park.*/
		s.search(12.974045,77.591995, 4);



	}

}


