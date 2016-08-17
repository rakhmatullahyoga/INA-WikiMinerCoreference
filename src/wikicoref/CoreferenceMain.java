/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wikicoref;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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
import org.xml.sax.SAXException;
import wikiminer.TextFolder;
import wikiminer.WikiConstants;

/**
 *
 * @author Rakhmatullah Yoga S
 */
public class CoreferenceMain {
    DocumentPreprocessor _preprocessor;
    Disambiguator _disambiguator ;
    TopicDetector _topicDetector ;
    LinkDetector _linkDetector ;
    CoreferenceTagger _tagger ;
    Wikipedia _wikipedia;
    
    private String newMarkup;
    private ArrayList<Set<String>> keyChain;
    private ArrayList<Set<String>> responseChain;
    private Collection<Topic> allTopics;
    private ArrayList<Topic> bestTopics;
    private PreprocessedDocument doc;
    private double recall;
    private double precision;
    private double fMeasure;

    public CoreferenceMain(Wikipedia wikipedia) throws Exception {
        _wikipedia = wikipedia;
        _preprocessor = new WikiPreprocessor(_wikipedia) ;
        _disambiguator = new Disambiguator(_wikipedia) ;
        _disambiguator.loadClassifier(new File(WikiConstants.DISAMBIGUATION_PATH));
        _topicDetector = new TopicDetector(_wikipedia, _disambiguator) ;
        _linkDetector = new LinkDetector(_wikipedia) ;
        _linkDetector.loadClassifier(new File(WikiConstants.DETECTION_PATH));
        _tagger = new CoreferenceTagger() ;
    }
    
    public void init() {
        newMarkup = null;
        keyChain = null;
        responseChain = null;
        allTopics = null;
        bestTopics = null;
        doc = null;
        recall = 0.0;
        precision = 0.0;
        fMeasure = 0.0;
    }
    
    public void gatherTopics(String originalMarkup, double topicThreshold) {
        try {
            // preprocess the input
            doc = _preprocessor.preprocess(originalMarkup);
            // detect all topic mentioned in the input
            allTopics = _topicDetector.getTopics(doc, null);
            bestTopics = _linkDetector.getBestTopics(allTopics, topicThreshold) ;
            for(Topic t:allTopics) {
                if(t.getAverageLinkProbability()> topicThreshold && !bestTopics.contains(t)) {
                    bestTopics.add(t);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(CoreferenceMain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void annotate(String originalMarkup, boolean exactLink, double corefThreshold) throws Exception {
        // tagging for coreference resolution
        _tagger.tag(doc, bestTopics, DocumentTagger.RepeatMode.ALL, _wikipedia, exactLink, corefThreshold) ;
        newMarkup = _tagger.getAnnotatedCoref();
        responseChain = _tagger.getMentionCluster();
    }
    
    public void writeAnnotated(String path) {
        try (PrintWriter writer = new PrintWriter(path, "UTF-8")) {
            writer.println(newMarkup);
            writer.close();
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            Logger.getLogger(CoreferenceMain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void writeTopics(String path) {
        try (PrintWriter writer = new PrintWriter(path, "UTF-8")) {
            writer.println("All detected topics:");
            for(Topic t:allTopics) {
                writer.println(t.getTitle() + " (" + t.getWeight()+"/"+t.getAverageLinkProbability()+ " - " + t.getRelatednessToContext()+"/"+t.getRelatednessToOtherTopics() + ")");
            }
            writer.println("\nTopics that are probably good links:");
            for(Topic t:bestTopics) {
                writer.println(t.getTitle() + " (" + t.getWeight()+"/"+t.getAverageLinkProbability()+ " - " + t.getRelatednessToContext()+"/"+t.getRelatednessToOtherTopics() + ")");
            }
            writer.close();
        } catch (Exception ex) {
            Logger.getLogger(CoreferenceMain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void calculateScore() {
        recall = computeMUC(keyChain,responseChain);
        precision = computeMUC(responseChain,keyChain);
        fMeasure = 2*precision*recall/(precision+recall);
    }

    private double computeMUC(ArrayList<Set<String>> chain1, ArrayList<Set<String>> chain2) {
        int pembilang = 0;
        int penyebut = 0;
        for(int i=0; i<chain1.size(); i++) {
            int Ki = chain1.get(i).size();
            int partLeft = Ki;
            int partition = 0;
            for(int j=0; j<chain2.size(); j++) {
                boolean found = false;
                for(String str : chain2.get(j)) {
                    if(chain1.get(i).contains(str)) {
                        found = true;
                        partLeft--;
                    }
                }
                if(found)
                    partition++;
            }
            pembilang += (Ki - (partition+partLeft));
            penyebut += (Ki-1);
        }
        return (double)pembilang/(double)penyebut;
    }

    public void readKeyChain(String path) {
        try {
            File inputFile = new File(path);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document xmlDoc = dBuilder.parse(inputFile);
            xmlDoc.getDocumentElement().normalize();
            NodeList nList = xmlDoc.getElementsByTagName("Entity");
            keyChain = new ArrayList<>();
            for(int i=0; i<nList.getLength(); i++) {
                Node nNode = nList.item(i);
                keyChain.add(new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    NodeList childList = eElement.getElementsByTagName("Mention");
                    for(int j=0; j<childList.getLength(); j++) {
                        String mention = childList.item(j).getTextContent();
                        keyChain.get(i).add(mention);
                    }
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            Logger.getLogger(CoreferenceMain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void writeCorefChain(String path) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document xmlDoc = dBuilder.newDocument();
            
            // root element
            Element rootElement = xmlDoc.createElement("CoreferenceChain");
            xmlDoc.appendChild(rootElement);
            // entity element
            for(int i=0; i<responseChain.size(); i++) {
                Element entity = xmlDoc.createElement("Entity");
                rootElement.appendChild(entity);
                // setting attribute to element
                Attr attr = xmlDoc.createAttribute("ClusterId");
                attr.setValue(""+(i+1));
                entity.setAttributeNode(attr);
                for(String mention : responseChain.get(i)) {
                    // mention element
                    Element carname = xmlDoc.createElement("Mention");
                    carname.appendChild(xmlDoc.createTextNode(mention));
                    entity.appendChild(carname);
                }
            }
            
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            
            DOMSource source = new DOMSource(xmlDoc);
            StreamResult result = new StreamResult(new File(path));
            transformer.transform(source, result);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(CoreferenceMain.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformerConfigurationException ex) {
            Logger.getLogger(CoreferenceMain.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformerException ex) {
            Logger.getLogger(CoreferenceMain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void trainingParameters(int NbTrain) {
        try (PrintWriter writer = new PrintWriter(CoreferenceConstants.TRAIN_PATH+"results.csv", "UTF-8")) {
            writer.println("TopicWeight,CorefThreshold,P,R,F");
            for(int i=0; i<=100; i++) {
                double topicWeight = (double)i/(double)100;
                for(int j=0; j<=100; j++) {
                    double corefTres = (double)j/(double)100;
                    double sumPrecision = 0.0;
                    double sumRecall = 0.0;
                    double sumFmeasure = 0.0;
                    for(int k=0; k<NbTrain; k++) {
                        File file = new File(CoreferenceConstants.TRAIN_RAW+"artikel"+k+".txt");
                        FileInputStream fis = new FileInputStream(file);
                        byte[] data = new byte[(int) file.length()];
                        fis.read(data);
                        fis.close();
                        String input = new String(data, "UTF-8");
                        init();
                        readKeyChain(CoreferenceConstants.TRAIN_KEY+"key"+k+".xml");
                        gatherTopics(input, topicWeight);
                        annotate(input,false,corefTres);
                        calculateScore();
                        sumPrecision += precision;
                        sumRecall += recall;
                        sumFmeasure += fMeasure;
                    }
                    double avgPrecision = sumPrecision/(double)NbTrain;
                    double avgRecall = sumRecall/(double)NbTrain;
                    double avgFmeasure = sumFmeasure/(double)NbTrain;
                    writer.println(topicWeight+","+corefTres+","+avgPrecision+","+avgRecall+","+avgFmeasure);
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(CoreferenceMain.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(CoreferenceMain.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CoreferenceMain.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(CoreferenceMain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void demo() {
        FileInputStream fis = null;
        try {
            File file = new File(CoreferenceConstants.PATH_DEMO+"raw.txt");
            fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();
            String input = new String(data, "UTF-8");
            System.out.println("Input raw text:\n"+input);
            init();
            readKeyChain(CoreferenceConstants.PATH_DEMO+"key.xml");
            gatherTopics(input, CoreferenceConstants.TOPIC_THRESHOLD);
            annotate(input,false,CoreferenceConstants.COREFERENCE_THRESHOLD);
            calculateScore();
            writeTopics(CoreferenceConstants.PATH_DEMO+"topics.txt");
            writeAnnotated(CoreferenceConstants.PATH_DEMO+"annotated.txt");
            writeCorefChain(CoreferenceConstants.PATH_DEMO+"response.xml");
            System.out.println("\n*********************");
            System.out.println("Result score");
            System.out.println("*********************");
            System.out.println("Recall: "+recall);
            System.out.println("Precision: "+precision);
            System.out.println("F-measure: "+fMeasure);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(CoreferenceMain.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CoreferenceMain.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(CoreferenceMain.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fis.close();
            } catch (IOException ex) {
                Logger.getLogger(CoreferenceMain.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void main(String args[]) throws Exception {
        WikipediaConfiguration conf = new WikipediaConfiguration(new File(WikiConstants.WIKI_CONFIG_PATH)) ;
        conf.setDefaultTextProcessor(new TextFolder()) ;
        Wikipedia wikipedia = new Wikipedia(conf, false) ;
        CoreferenceMain annotator = new CoreferenceMain(wikipedia) ;
        
        // evaluate on 30 documents
//        for(int i=0; i<30; i++) {
//            File file = new File(CoreferenceConstants.TRAIN_RAW+"artikel"+i+".txt");
//            FileInputStream fis = new FileInputStream(file);
//            byte[] data = new byte[(int) file.length()];
//            fis.read(data);
//            fis.close();
//            String input = new String(data, "UTF-8");
//            System.out.println("Input raw text:\n"+input);
//
//            annotator.init();
//            annotator.gatherTopics(input, CoreferenceConstants.TOPIC_THRESHOLD);
//            annotator.annotate(input,false,CoreferenceConstants.COREFERENCE_THRESHOLD);
//            annotator.writeTopics(CoreferenceConstants.TRAIN_TOPICS+"topics"+i+".txt");
//            annotator.writeAnnotated(CoreferenceConstants.TRAIN_ANNOTATED+"annotated"+i+".txt");
//            annotator.writeCorefChain(CoreferenceConstants.TRAIN_RESPONSE+"response"+i+".xml");
//        }
        
        // demo mode only for 1 document
        annotator.demo();
    }
}
