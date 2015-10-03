package com.lucene.example.indexing;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class SimpleFileIndexer {
    
    public static void main(String[] args) throws Exception {
        
        File indexDir = new File("C:/index/");
        File dataDir = new File("C:/programs/eclipse/workspace/");
        String suffix = "java";
        
        SimpleFileIndexer indexer = new SimpleFileIndexer();
        
        int numIndex = indexer.index(indexDir, dataDir, suffix);
        
        System.out.println("Total files indexed " + numIndex);
        
    }
    
    private int index(File indexDir, File dataDir, String suffix) throws Exception {
        
        /*IndexWriter indexWriter = new IndexWriter(
                FSDirectory.open(indexDir), 
                new SimpleAnalyzer(Version.LUCENE_4_9),
                true,
                IndexWriter.MaxFieldLength.LIMITED);
        indexWriter.setUseCompoundFile(false);
        
        indexDirectory(indexWriter, dataDir, suffix);
        
        int numIndexed = indexWriter.maxDoc();
        indexWriter.optimize();
        indexWriter.close();*/
        
        return 0;// numIndexed;
        
    }
    
    private void indexDirectory(IndexWriter indexWriter, File dataDir, 
           String suffix) throws IOException {

        File[] files = dataDir.listFiles();
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (f.isDirectory()) {
                indexDirectory(indexWriter, f, suffix);
            }
            else {
                indexFileWithIndexWriter(indexWriter, f, suffix);
            }
        }

    }
    
    private void indexFileWithIndexWriter(IndexWriter indexWriter, File f, 
            String suffix) throws IOException {

        if (f.isHidden() || f.isDirectory() || !f.canRead() || !f.exists()) {
            return;
        }
        if (suffix!=null && !f.getName().endsWith(suffix)) {
            return;
        }
        System.out.println("Indexing file " + f.getCanonicalPath());
        
        Document doc = new Document();
        doc.add(new Field("contents", new FileReader(f)));        
        doc.add(new Field("filename", f.getCanonicalPath(), 
           Field.Store.YES, Field.Index.ANALYZED));
        
        indexWriter.addDocument(doc);

    }

}
