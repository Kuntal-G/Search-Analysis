package com.lucene.example.indexing;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
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
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;


/**
 * Sample application that extract content + metadata from PDF files using Tika and index writes to a file store
 *   
 * @author kuntal
 *
 */
public class WriteContentIndex {

	public static final String INDEX_DIRECTORY = "/home/kuntal/Downloads/index";
	public static final String FILES_DIRECTORY ="/home/kuntal/Downloads/test";
	
	
	public static void main(String[] args)  {
		try {
			WriteContentIndex.writeIndex(INDEX_DIRECTORY, FILES_DIRECTORY);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private static void writeIndex(String indexPath,String fileDir) throws IOException{
		
		File docs = new File(fileDir);
		System.out.println("Starts");		
		Directory directory = FSDirectory.open(new File(indexPath));
		
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_4_9);
		IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_4_9, analyzer);
		IndexWriter writer = new IndexWriter(directory, conf);
		writer.deleteAll();
		
		for (File file : docs.listFiles()) {
			System.out.println("Looping inside file dir");
			Metadata metadata = new Metadata();
			ContentHandler handler = new BodyContentHandler();
			ParseContext context = new ParseContext();
			Parser parser = new AutoDetectParser();
			InputStream stream = new FileInputStream(file);
			try {
				parser.parse(stream, handler, metadata, context);
			}
			catch (TikaException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			}
			finally {
				stream.close();
			}
			
			String text = handler.toString();
			String fileName = file.getName();		
			
			Document doc = new Document();
			doc.add(new Field("file", fileName, Store.YES, Index.NO));
			
			
			for (String key : metadata.names()) {
				String name = key.toLowerCase();
				String value = metadata.get(key);
				
				if (StringUtils.isBlank(value)) {
					continue;
				}
				
				if ("keywords".equalsIgnoreCase(key)) {
					for (String keyword : value.split(",?(\\s+)")) {
						doc.add(new Field(name, keyword, Store.YES, Index.NOT_ANALYZED));
					}
				}
				else if ("title".equalsIgnoreCase(key)) {
					doc.add(new Field(name, value, Store.YES, Index.ANALYZED));
				}
				else {
					doc.add(new Field(name, fileName, Store.YES, Index.NOT_ANALYZED));
				}
			}
			doc.add(new Field("text", text, Store.NO, Index.ANALYZED));
			writer.addDocument(doc);
			
		}
		
		writer.commit();
		writer.deleteUnusedFiles();
		
		System.out.println(writer.maxDoc() + " documents written");
		
	}

}
