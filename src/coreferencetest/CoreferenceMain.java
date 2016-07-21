/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package coreferencetest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
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
import wikiminertest.TextFolder;
import wikiminertest.WikiConstants;

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
    DecimalFormat _df = new DecimalFormat("#0%");

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

    public void annotate(String originalMarkup) throws Exception {
        PreprocessedDocument doc = _preprocessor.preprocess(originalMarkup) ;
        
        Collection<Topic> allTopics = _topicDetector.getTopics(doc, null) ;
        ArrayList<Topic> bestTopics = _linkDetector.getBestTopics(allTopics, CoreferenceConstants.TOPIC_THRESHOLD) ;
//        System.out.println("\nAll detected topics:") ;
        for(Topic t:allTopics) {
//            System.out.println(" - " + t.getTitle() + " (" + t.getAverageLinkProbability() + ")") ;
            if(t.getAverageLinkProbability() > CoreferenceConstants.TOPIC_THRESHOLD && !bestTopics.contains(t)) {
                bestTopics.add(t);
            }
        }
        
//        System.out.println("\nTopics that are probably good links:") ;
//        for (Topic t:bestTopics)
//            System.out.println(" - " + t.getTitle() + "[" + _df.format(t.getWeight()) + "]" ) ;
        
        _tagger.tag(doc, bestTopics, DocumentTagger.RepeatMode.ALL, _wikipedia) ;
        String newMarkup = _tagger.getAnnotatedCoref();
        ArrayList<ArrayList<String>> corefChain = _tagger.getMentionCluster();
        System.out.println("\nAugmented markup (Entity linking + Coreference):\n" + newMarkup + "\n") ;
        writeAnnotated(newMarkup, CoreferenceConstants.ANNOTATED_PATH+"annotated.txt");
        writeCorefChain(corefChain, CoreferenceConstants.ANNOTATED_PATH+"annotated.txt");
    }
    
    public void writeAnnotated(String annotated, String path) {
        try (PrintWriter writer = new PrintWriter(path, "UTF-8")) {
            writer.println(annotated);
            writer.close();
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            Logger.getLogger(CoreferenceMain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void writeCorefChain(ArrayList<ArrayList<String>> corefChain, String path) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();
            
            // root element
            Element rootElement = doc.createElement("CoreferenceChain");
            doc.appendChild(rootElement);
            // entity element
            for(int i=0; i<corefChain.size(); i++) {
                Element entity = doc.createElement("Entity");
                rootElement.appendChild(entity);
                // setting attribute to element
                Attr attr = doc.createAttribute("ClusterId");
                attr.setValue(""+(i+1));
                entity.setAttributeNode(attr);
                for(String mention : corefChain.get(i)) {
                    // mention element
                    Element carname = doc.createElement("Mention");
                    carname.appendChild(doc.createTextNode(mention));
                    entity.appendChild(carname);
                }
            }
            
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(CoreferenceConstants.CHAIN_PATH+"chain.xml"));
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

        File file = new File("./data/development/raw/raw.txt");
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();
        
        String input = new String(data, "UTF-8");
        System.out.println("Input raw text:\n"+input);
        annotator.annotate(input);
    }
}
