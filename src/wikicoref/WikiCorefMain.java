/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wikicoref;

import helper.ChainHelper;
import helper.CoreferenceConstants;
import helper.CoreferenceScoring;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.wikipedia.miner.annotation.Disambiguator;
import org.wikipedia.miner.annotation.Topic;
import org.wikipedia.miner.annotation.TopicDetector;
import org.wikipedia.miner.annotation.preprocessing.DocumentPreprocessor;
import org.wikipedia.miner.annotation.preprocessing.PreprocessedDocument;
import org.wikipedia.miner.annotation.preprocessing.WikiPreprocessor;
import org.wikipedia.miner.annotation.tagging.DocumentTagger;
import org.wikipedia.miner.annotation.weighting.LinkDetector;
import org.wikipedia.miner.model.Wikipedia;
import org.wikipedia.miner.util.WikipediaConfiguration;
import wikiminer.TextFolder;
import wikiminer.WikiConstants;

/**
 *
 * @author Rakhmatullah Yoga S
 */
public class WikiCorefMain {
    private static DocumentPreprocessor _preprocessor;
    private static Disambiguator _disambiguator ;
    private static TopicDetector _topicDetector ;
    private static LinkDetector _linkDetector ;
    private static CoreferenceTagger _tagger ;
    private static Wikipedia _wikipedia;
    
    private static String newMarkup;
    private static ArrayList<Set<String>> keyChain;
    private static ArrayList<Set<String>> responseChain;
    private static Collection<Topic> allTopics;
    private static ArrayList<Topic> bestTopics;
    private static PreprocessedDocument doc;

    public static void init() throws Exception {
        WikipediaConfiguration conf = new WikipediaConfiguration(new File(WikiConstants.WIKI_CONFIG_PATH));
        conf.setDefaultTextProcessor(new TextFolder());
        _wikipedia = new Wikipedia(conf, false);
        _preprocessor = new WikiPreprocessor(_wikipedia);
        _disambiguator = new Disambiguator(_wikipedia);
        _disambiguator.loadClassifier(new File(WikiConstants.DISAMBIGUATION_PATH));
        _topicDetector = new TopicDetector(_wikipedia, _disambiguator);
        _linkDetector = new LinkDetector(_wikipedia);
        _linkDetector.loadClassifier(new File(WikiConstants.DETECTION_PATH));
        _tagger = new CoreferenceTagger();
    }
    
    public static void clearData() {
        newMarkup = null;
        keyChain = null;
        responseChain = null;
        allTopics = null;
        bestTopics = null;
        doc = null;
    }
    
    public static void gatherTopics(String originalMarkup, double topicThreshold) {
        try {
            // preprocess the input
            doc = _preprocessor.preprocess(originalMarkup);
            // detect all topic mentioned in the input
            allTopics = _topicDetector.getTopics(doc, null);
            bestTopics = _linkDetector.getBestTopics(allTopics, topicThreshold) ;
            for(Topic t:allTopics) {
                if(t.getMaxLinkProbability() > topicThreshold && !bestTopics.contains(t)) {
                    bestTopics.add(t);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(WikiCorefMain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void annotate(String originalMarkup, boolean exactLink, double corefThreshold) throws Exception {
        // tagging for coreference resolution
        _tagger.tag(doc, bestTopics, DocumentTagger.RepeatMode.ALL, _wikipedia, exactLink, corefThreshold) ;
        newMarkup = _tagger.getAnnotatedCoref();
        responseChain = _tagger.getMentionCluster();
    }
    
    public static void writePreprocessedDoc(String path) {
        try (PrintWriter writer = new PrintWriter(path, "UTF-8")) {
            writer.println("ContextText:");
            writer.println(doc.getContextText());
            writer.println("\nPreprocessedText:");
            writer.println(doc.getPreprocessedText());
            writer.close();
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            Logger.getLogger(WikiCorefMain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void writeAnnotated(String path) {
        try (PrintWriter writer = new PrintWriter(path, "UTF-8")) {
            writer.println(newMarkup);
            writer.close();
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            Logger.getLogger(WikiCorefMain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void writeTopics(String path) {
        try (PrintWriter writer = new PrintWriter(path, "UTF-8")) {
            writer.println("All detected topics:");
            for(Topic t:allTopics)
                writer.println(t.getTitle() + " (" + t.getWeight() + "/" + t.getMaxLinkProbability() + ")");
            writer.println("\nTopics that are probably good links:");
            for(Topic t:bestTopics)
                writer.println(t.getTitle() + " (" + t.getWeight() + "/" + t.getMaxLinkProbability() + ")");
            writer.println("\nMention to topic map:");
            HashMap<String, String> mentionTopicMap = _tagger.getMentionTopicMap();
            for(String str : mentionTopicMap.keySet())
                writer.println(str+" -> "+mentionTopicMap.get(str));
            writer.close();
        } catch (Exception ex) {
            Logger.getLogger(WikiCorefMain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void testing() {
        PrintWriter writer = null;
        try {
            File dir = new File(CoreferenceConstants.RAW_TESTING);
            File[] listFiles = dir.listFiles();
            int NbTrain = listFiles.length;
            double sumPrecision = 0.0;
            double sumRecall = 0.0;
            double sumFmeasure = 0.0;
            writer = new PrintWriter(CoreferenceConstants.WIKI_TESTING+"results.csv", "UTF-8");
            writer.println("Document;Precision;Recall;F-measure");
            for(int i=0; i<NbTrain; i++) {
                File file = new File(CoreferenceConstants.RAW_TESTING+"artikel"+i+".txt");
                FileInputStream fis = new FileInputStream(file);
                byte[] data = new byte[(int) file.length()];
                fis.read(data);
                fis.close();
                String input = new String(data, "UTF-8");
                clearData();
                keyChain = ChainHelper.readKeyChain(CoreferenceConstants.KEY_TESTING+"key"+i+".xml");
                gatherTopics(input, CoreferenceConstants.TOPIC_THRESHOLD);
                annotate(input,false,CoreferenceConstants.COREFERENCE_THRESHOLD);
                writeTopics(CoreferenceConstants.WIKI_TESTING+"topics/topic"+i+".txt");
                writeAnnotated(CoreferenceConstants.WIKI_TESTING+"/annotated/annotated"+i+".txt");
                ChainHelper.writeCorefChain(responseChain,CoreferenceConstants.WIKI_TESTING+"responsechain/response"+i+".xml");
                CoreferenceScoring.init();
                CoreferenceScoring.computeCEAFmScore(keyChain, responseChain);
                double precision = CoreferenceScoring.getPrecision();
                double recall = CoreferenceScoring.getRecall();
                double fMeasure = CoreferenceScoring.getfMeasure();
                sumPrecision += precision;
                sumRecall += recall;
                sumFmeasure += fMeasure;
                writer.println("Document"+i+";"+precision+";"+recall+";"+fMeasure);
                System.out.println("testing doc-"+i);
            }
            double avgPrecision = sumPrecision/(double)NbTrain;
            double avgRecall = sumRecall/(double)NbTrain;
            double avgFmeasure = sumFmeasure/(double)NbTrain;
            System.out.println("\n*********************");
            System.out.println("Result score");
            System.out.println("*********************");
            System.out.println("Recall: "+avgRecall);
            System.out.println("Precision: "+avgPrecision);
            System.out.println("F-measure: "+avgFmeasure);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(WikiCorefMain.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(WikiCorefMain.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(WikiCorefMain.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(WikiCorefMain.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            writer.close();
        }
    }
    
    public static void trainingParameters() {
        File dir = new File(CoreferenceConstants.RAW_TRAINING);
        File[] listFiles = dir.listFiles();
        int NbTrain = listFiles.length;
        try (PrintWriter writer = new PrintWriter(CoreferenceConstants.WIKI_TRAINING
                +"results(maxLink).csv", "UTF-8")) {
            writer.println("TopicWeight;CorefThreshold;Precision;Recall;Fmeasure");
            for(int i=0; i<=100; i++) {
                double topicWeight = (double)i/(double)100;//CoreferenceConstants.TOPIC_THRESHOLD;
//                for(int j=0; j<=100; j++) {
                    double corefTres = 0.0;
                    double sumPrecision = 0.0;
                    double sumRecall = 0.0;
                    double sumFmeasure = 0.0;
                    for(int k=0; k<NbTrain; k++) {
                        File file = new File(CoreferenceConstants.RAW_TRAINING+"artikel"+k+".txt");
                        FileInputStream fis = new FileInputStream(file);
                        byte[] data = new byte[(int) file.length()];
                        fis.read(data);
                        fis.close();
                        String input = new String(data, "UTF-8");
                        clearData();
                        keyChain = ChainHelper.readKeyChain(CoreferenceConstants.KEY_TRAINING+"key"+k+".xml");
                        gatherTopics(input, topicWeight);
                        annotate(input,false,corefTres);
                        CoreferenceScoring.init();
                        CoreferenceScoring.computeCEAFmScore(keyChain, responseChain);
                        sumPrecision += CoreferenceScoring.getPrecision();
                        sumRecall += CoreferenceScoring.getRecall();
                        sumFmeasure += CoreferenceScoring.getfMeasure();
                        System.out.println("topic="+topicWeight+", threshold="+corefTres+", doc-"+k);
                    }
                    double avgPrecision = sumPrecision/(double)NbTrain;
                    double avgRecall = sumRecall/(double)NbTrain;
                    double avgFmeasure = sumFmeasure/(double)NbTrain;
                    writer.println(topicWeight+";"+corefTres+";"+avgPrecision+";"+avgRecall+";"+avgFmeasure);
//                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(WikiCorefMain.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(WikiCorefMain.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(WikiCorefMain.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(WikiCorefMain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void demo() {
        FileInputStream fis = null;
        try {
            File file = new File(CoreferenceConstants.PATH_DEMO+"raw.txt");
            fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();
            String input = new String(data, "UTF-8");
            System.out.println("Input raw text:\n"+input);
            clearData();
            gatherTopics(input, CoreferenceConstants.TOPIC_THRESHOLD);
            writePreprocessedDoc(CoreferenceConstants.PATH_DEMO+"wiki/preprocessed.txt");
            annotate(input,false,CoreferenceConstants.COREFERENCE_THRESHOLD);
            System.out.println("\nAnnotated text:\n"+newMarkup);
            writeTopics(CoreferenceConstants.PATH_DEMO+"wiki/topics.txt");
            writeAnnotated(CoreferenceConstants.PATH_DEMO+"wiki/annotated.txt");
            ChainHelper.writeCorefChain(responseChain,CoreferenceConstants.PATH_DEMO+"wiki/response.xml");
            keyChain = ChainHelper.readKeyChain(CoreferenceConstants.PATH_DEMO+"key.xml");
            CoreferenceScoring.init();
            CoreferenceScoring.computeCEAFmScore(keyChain, responseChain);
            System.out.println("\n*********************");
            System.out.println("Result score");
            System.out.println("*********************");
            System.out.println("Recall: "+CoreferenceScoring.getRecall());
            System.out.println("Precision: "+CoreferenceScoring.getPrecision());
            System.out.println("F-measure: "+CoreferenceScoring.getfMeasure());
        } catch (FileNotFoundException ex) {
            Logger.getLogger(WikiCorefMain.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(WikiCorefMain.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(WikiCorefMain.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fis.close();
            } catch (IOException ex) {
                Logger.getLogger(WikiCorefMain.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void main(String args[]) throws Exception {
        init();
        
        // demo mode only for 1 document
        demo();
    }
}
