/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package coreferencetest;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
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
public class CoreferenceTest {
    DocumentPreprocessor _preprocessor;
    Disambiguator _disambiguator ;
    TopicDetector _topicDetector ;
    LinkDetector _linkDetector ;
    CoreferenceTagger _tagger ;
    Wikipedia _wikipedia;
    DecimalFormat _df = new DecimalFormat("#0%");

    public CoreferenceTest(Wikipedia wikipedia) throws Exception {
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
        ArrayList<Topic> bestTopics = _linkDetector.getBestTopics(allTopics, CoreferenceParameter.TOPIC_THRESHOLD) ;
//        System.out.println("\nAll detected topics:") ;
        for(Topic t:allTopics) {
//            System.out.println(" - " + t.getTitle() + " (" + t.getAverageLinkProbability() + ")") ;
            if(t.getAverageLinkProbability() > CoreferenceParameter.TOPIC_THRESHOLD && !bestTopics.contains(t)) {
                bestTopics.add(t);
            }
        }
        
//        System.out.println("\nTopics that are probably good links:") ;
//        for (Topic t:bestTopics)
//            System.out.println(" - " + t.getTitle() + "[" + _df.format(t.getWeight()) + "]" ) ;
        
        String newMarkup = _tagger.tag(doc, bestTopics, DocumentTagger.RepeatMode.ALL, _wikipedia) ;
        System.out.println("\nAugmented markup (Entity linking + Coreference):\n" + newMarkup + "\n") ;
    }

    public static void main(String args[]) throws Exception {
        WikipediaConfiguration conf = new WikipediaConfiguration(new File(WikiConstants.WIKI_CONFIG_PATH)) ;
        conf.setDefaultTextProcessor(new TextFolder()) ;
        Wikipedia wikipedia = new Wikipedia(conf, false) ;

        CoreferenceTest annotator = new CoreferenceTest(wikipedia) ;

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in)) ;
//        while (true) {
            System.out.println("Enter snippet to annotate (or ENTER to quit):") ;
            String line = reader.readLine();

//            if (line.trim().length() == 0)
//                break ;

            annotator.annotate(line) ;
//        }
    }
}
