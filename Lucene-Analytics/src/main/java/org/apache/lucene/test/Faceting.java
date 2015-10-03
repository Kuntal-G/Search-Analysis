package org.apache.lucene.test;

import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.facet.DrillDownQuery;
import org.apache.lucene.facet.DrillSideways;
import org.apache.lucene.facet.FacetField;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;


public class Faceting {

	public static void main(String[] args) throws IOException {

		StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_4_9);
		Directory indexDirectory = new RAMDirectory();
		Directory facetDirectory = new RAMDirectory();
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_9,analyzer);
		IndexWriter indexWriter = new IndexWriter(indexDirectory, config);
		
		DirectoryTaxonomyWriter directoryTaxonomyWriter = new	DirectoryTaxonomyWriter(facetDirectory);
		FacetsConfig facetsConfig = new FacetsConfig();
		
		Document doc = new Document();
		doc.add(new FacetField("BookId", "B1"));
		doc.add( new FacetField("Author", "Author 1"));
		doc.add(new FacetField("Category","Cat 1"));
		indexWriter.addDocument(facetsConfig.build(directoryTaxonomyWriter, doc));
		
		doc = new Document();
		doc.add(new FacetField("BookId", "B2"));
		doc.add(new FacetField("Author", "Author 2"));
		doc.add(new FacetField("Category", "Cat 1"));
		indexWriter.addDocument(facetsConfig.build(directoryTaxonomyWriter, doc));
		
		doc = new Document();
		doc.add(new FacetField("BookId", "B3"));
		doc.add(new FacetField("Author", "Author 3"));
		doc.add(new FacetField("Category", "Cat 2"));
		indexWriter.addDocument(facetsConfig.build(directoryTaxonomyWriter, doc));
		indexWriter.commit();
		directoryTaxonomyWriter.commit();
		
		
		IndexReader indexReader = DirectoryReader.open(indexDirectory);
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);
		
		DirectoryTaxonomyReader directoryTaxonomyReader = new	DirectoryTaxonomyReader(facetDirectory);
		FacetsCollector facetsCollector = new FacetsCollector();
		facetsCollector.search(indexSearcher, new MatchAllDocsQuery(), 10,	facetsCollector);
		Facets facets = new	FastTaxonomyFacetCounts(directoryTaxonomyReader, facetsConfig,facetsCollector);
		FacetResult facetResult = facets.getTopChildren(10, "Category");
		
		for (LabelAndValue labelAndValue : facetResult.labelValues) {
			System.out.println(labelAndValue.label + ":" +	labelAndValue.value);
		}
		
		facetResult = facets.getTopChildren(10, "Author");
		
		for (LabelAndValue labelAndValue : facetResult.labelValues) {
			System.out.println(labelAndValue.label + ":" +	labelAndValue.value);
		}
		DrillDownQuery drillDownQuery = new DrillDownQuery(facetsConfig);
		drillDownQuery.add("Category", "Cat 1");
		
		DrillSideways drillSideways = new DrillSideways(indexSearcher,facetsConfig, directoryTaxonomyReader);
		DrillSideways.DrillSidewaysResult drillSidewaysResult =	drillSideways.search(drillDownQuery, 10);
		facetResult = drillSidewaysResult.facets.getTopChildren(10,	"Category");
		
		for (LabelAndValue labelAndValue : facetResult.labelValues) {
			System.out.println(labelAndValue.label + ":" +	labelAndValue.value);
		}
		
		facetResult = drillSidewaysResult.facets.getTopChildren(10,	"Author");
		for (LabelAndValue labelAndValue : facetResult.labelValues) {
			System.out.println(labelAndValue.label + ":" +	labelAndValue.value);
		}
	}

}
