package cz.evolvedbinary.search;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.cz.CzechAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.AttributeImpl;
import org.w3c.dom.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Random;
import java.util.UUID;

public class SearchTest {

    public static final Path DIR = Paths.get("./index-dir");

    /**
     * Basic configuration, Directory where index is stored. And primary analyser.
     * @throws IOException
     */
    public void index() throws IOException {

        FileUtils.deleteDirectory(DIR.toFile());

        Files.createDirectory(DIR);

        Directory dir = FSDirectory.open(DIR);
        Analyzer analyzer = new EnglishAnalyzer(EnglishAnalyzer.getDefaultStopSet());

        analyserTest(analyzer);
        analyserTest(new CzechAnalyzer());


        IndexWriterConfig config = new IndexWriterConfig(analyzer); //Index configuration and also default analyzer!!!!



        try(IndexWriter writer = new IndexWriter(dir, config)) {
            writeData(writer);  //Check this method
        }

        try(IndexReader r = DirectoryReader.open(dir)) {
            readData(r);
            //IndexSearcher searcher = new IndexSearcher(r);

        }

    }


    public void analyserTest(Analyzer analyzer) {
        System.out.println("\n\n\n ");
        try(TokenStream res = analyzer.tokenStream("test-text", "This is a english text")) {
            res.reset();
            do {
                System.out.println("token attr: ");

                for (Iterator<AttributeImpl> it = res.getAttributeImplsIterator(); it.hasNext(); ) {
                    AttributeImpl attr = it.next();
                    System.out.println(" attr: " + attr.reflectAsString(false));
                }

            }while (res.incrementToken());
            res.end();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void readData(IndexReader reader) throws IOException {
        Document doc = reader.document(0);

        System.out.println("fields:  ");
        for (IndexableField field : doc) {
            System.out.println(" " + field);
        }

        System.out.println("\nTerm vectors");

        Fields termVectors = reader.getTermVectors(0);
        if (termVectors != null) {
            for (String term : termVectors) {
                System.out.println(" " + term);
            }
        }else {
            System.out.println(" no term vectors");
        }

//        Terms t = reader.getTermVector(0, "longText");
//
//        System.out.println("t: " + t.getStats());

    }

    /**
     * Create document with few field, "_id", "password","term-tok", "value.raw", "fieldWithCustomAnalyser".
     *
     * fieldWithCustomAnalyser - Is crated as "generic" field. And utilise tokenStream property from Filed.
     * This allows to set up custom tokenized data.
     *
     * @param writer
     * @throws IOException
     */
    public void writeData(IndexWriter writer) throws IOException {
        String docId = UUID.randomUUID().toString();
        StringField id = new StringField("_id", docId, Field.Store.YES);
        TextField hesloTok = new TextField("term-tok", "Tajne heslo keter je tokenizovano", Field.Store.YES);

        FieldType fieldType = new FieldType();
        fieldType.setStored(true);
        fieldType.setTokenized(true);
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        fieldType.setStoreTermVectors(true);

        Analyzer analyzer = new EnglishAnalyzer(EnglishAnalyzer.getDefaultStopSet());


        Field longText = new Field("fieldWithCustomAnalyser", LONG_TEXT,  fieldType ); //Create field, generic as possible, with original long text and additional field type specifications.

        longText.setTokenStream(analyzer.tokenStream(longText.name(), LONG_TEXT)); //Setup custom tokenized data. This is the trick. It will not use the default one from IndexWriter.
        // We can use any analyzer which we want. I think this may even allow us to index custom types, like IPv6 IP address, or Post code. For each we can use different analyzer
        // Don't know what will happen if fields with same name are analyzed with different analyzer. -> I think Search will be broken.
        // from different analyzers.

        IntPoint intPoint = new IntPoint("value.raw", 100);

        Document doc = new Document();
        doc.add(id);
        doc.add(new StringField("password", "Tajne heslo", Field.Store.YES));
        doc.add(hesloTok);
        doc.add(intPoint);
        doc.add(longText);

        writer.addDocument(doc);
    }


    public static final String LONG_TEXT = "Lucene Analyzers are processing pipelines that" +
            " break up text into indexed tokens, a.k.a. terms, and optionally perform" +
            "other operations on these tokens, e.g. downcasing, synonym insertion," +
            " filtering out unwanted tokens, etc. The Analyzer we are using is" +
            " StandardAnalyzer, which creates tokens using the Word Break rules " +
            "from the Unicode Text Segmentation algorithm specified in Unicode " +
            "Standard Annex #29; converts tokens to lowercase; and then filters o" +
            "ut stopwords. Stopwords are common language words such as articles (a," +
            " an, the, etc.) and other tokens that may have less value for searching. " +
            "It should be noted that there are different rules for every language, and " +
            "you should use the proper analyzer for each. Lucene currently provides " +
            "Analyzers for a number of different languages (see the javadocs under " +
            "lucene/analysis/common/src/java/org/apache/lucene/analysis).";
}