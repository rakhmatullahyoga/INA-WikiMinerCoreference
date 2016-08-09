/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package coreference;

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
        responseChain = null;
        allTopics = null;
        bestTopics = null;
    }

    public void annotate(String originalMarkup, boolean exactLink) throws Exception {
        // preprocess the input
        PreprocessedDocument doc = _preprocessor.preprocess(originalMarkup) ;
        doc.getPreprocessedText();
        // detect all topic mentioned in the input
        allTopics = _topicDetector.getTopics(doc, null) ;
        bestTopics = _linkDetector.getBestTopics(allTopics, CoreferenceConstants.TOPIC_THRESHOLD) ;
//        System.out.println("\nAll detected topics:") ;
        for(Topic t:allTopics) {
//            System.out.println(" - " + t.getTitle() + " (" + t.getAverageLinkProbability() + ")") ;
            if(t.getRelatednessToContext()> CoreferenceConstants.TOPIC_THRESHOLD && !bestTopics.contains(t)) {
                bestTopics.add(t);
            }
        }
        
//        System.out.println("\nTopics that are probably good links:") ;
//        for (Topic t:bestTopics)
//            System.out.println(" - " + t.getTitle() + "[" + t.getWeight() + "]" ) ;
        
        // tagging for coreference resolution
        _tagger.tag(doc, bestTopics, DocumentTagger.RepeatMode.ALL, _wikipedia, exactLink) ;
        newMarkup = _tagger.getAnnotatedCoref();
        responseChain = _tagger.getMentionCluster();
        System.out.println("\nAugmented markup (Entity linking + Coreference):\n" + newMarkup + "\n");
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
            for(Topic t:bestTopics) {
                writer.print(t.getTitle());
                writer.println();
            }
            writer.close();
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            Logger.getLogger(CoreferenceMain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void showResultAnalysys() {
        double recall = compute(keyChain,responseChain);
        double precision = compute(responseChain,keyChain);
        double fMeasure = 2*precision*recall/(precision+recall);
        System.out.println("\n*********************");
        System.out.println("Result score");
        System.out.println("*********************");
        System.out.println("Recall: "+recall);
        System.out.println("Precision: "+precision);
        System.out.println("F-measure: "+fMeasure);
    }

    private double compute(ArrayList<Set<String>> chain1, ArrayList<Set<String>> chain2) {
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
//            System.out.println(pembilang+"/"+penyebut);
        }
        return (double)pembilang/(double)penyebut;
    }

    public void readKeyChain(String path) {
        try {
            File inputFile = new File(path);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName("Entity");
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
            Document doc = dBuilder.newDocument();
            
            // root element
            Element rootElement = doc.createElement("CoreferenceChain");
            doc.appendChild(rootElement);
            // entity element
            for(int i=0; i<responseChain.size(); i++) {
                Element entity = doc.createElement("Entity");
                rootElement.appendChild(entity);
                // setting attribute to element
                Attr attr = doc.createAttribute("ClusterId");
                attr.setValue(""+(i+1));
                entity.setAttributeNode(attr);
                for(String mention : responseChain.get(i)) {
                    // mention element
                    Element carname = doc.createElement("Mention");
                    carname.appendChild(doc.createTextNode(mention));
                    entity.appendChild(carname);
                }
            }
            
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            
            DOMSource source = new DOMSource(doc);
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

    public static void main(String args[]) throws Exception {
        WikipediaConfiguration conf = new WikipediaConfiguration(new File(WikiConstants.WIKI_CONFIG_PATH)) ;
        conf.setDefaultTextProcessor(new TextFolder()) ;
        Wikipedia wikipedia = new Wikipedia(conf, false) ;
        CoreferenceMain annotator = new CoreferenceMain(wikipedia) ;
        
        // evaluate on 30 documents
//        for(int i=0; i<30; i++) {
//            File file = new File(CoreferenceConstants.RAW_PATH+"artikel"+i+".txt");
//            FileInputStream fis = new FileInputStream(file);
//            byte[] data = new byte[(int) file.length()];
//            fis.read(data);
//            fis.close();
//            String input = new String(data, "UTF-8");
//            System.out.println("Input raw text:\n"+input);
//
//            annotator.init();
//            annotator.annotate(input,false);
////            annotator.writeTopics(CoreferenceConstants.TOPICS_PATH+"topics"+i+".txt");
//            annotator.writeAnnotated(CoreferenceConstants.ANNOTATED_PATH+"annotated"+i+".txt");
//            annotator.writeCorefChain(CoreferenceConstants.CHAIN_PATH+"response"+i+".xml");
//        }
        
        // development mode only 1 document
        File file = new File(CoreferenceConstants.PATH_DEV+"raw.txt");
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();
        
        String input = new String(data, "UTF-8");
        System.out.println("Input raw text:\n"+input);
        annotator.readKeyChain(CoreferenceConstants.PATH_DEV+"key.xml");
        annotator.annotate(input,false);
        annotator.writeAnnotated(CoreferenceConstants.PATH_DEV+"annotated.txt");
        annotator.writeCorefChain(CoreferenceConstants.PATH_DEV+"response.xml");
        annotator.showResultAnalysys();
    }
}
