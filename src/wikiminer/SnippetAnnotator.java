/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wikiminer;

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
import org.wikipedia.miner.annotation.tagging.DocumentTagger.RepeatMode;
import org.wikipedia.miner.annotation.tagging.WikiTagger;
import org.wikipedia.miner.annotation.weighting.LinkDetector;
import org.wikipedia.miner.model.Wikipedia;
import org.wikipedia.miner.util.WikipediaConfiguration;

/**
 *
 * @author rakhmatullahyoga
 */
public class SnippetAnnotator {
    DocumentPreprocessor _preprocessor;
    Disambiguator _disambiguator ;
    TopicDetector _topicDetector ;
    LinkDetector _linkDetector ;
    DocumentTagger _tagger ;
    DecimalFormat _df = new DecimalFormat("#0%");

    public SnippetAnnotator(Wikipedia wikipedia) throws Exception {
        _preprocessor = new WikiPreprocessor(wikipedia) ;
        _disambiguator = new Disambiguator(wikipedia) ;
        _disambiguator.loadClassifier(new File(WikiConstants.DISAMBIGUATION_PATH));
        _topicDetector = new TopicDetector(wikipedia, _disambiguator) ;
        _linkDetector = new LinkDetector(wikipedia) ;
        _linkDetector.loadClassifier(new File(WikiConstants.DETECTION_PATH));
        _tagger = new WikiTagger() ;
    }

    public void annotate(String originalMarkup) throws Exception {
        PreprocessedDocument doc = _preprocessor.preprocess(originalMarkup) ;
        
        Collection<Topic> allTopics = _topicDetector.getTopics(doc, null) ;
        ArrayList<Topic> bestTopics = _linkDetector.getBestTopics(allTopics, 0.35) ;
        System.out.println("\nAll detected topics:") ;
        for(Topic t:allTopics) {
            System.out.println(" - " + t.getTitle() + " (" + t.getAverageLinkProbability() + ")") ;
            if(t.getAverageLinkProbability() > 0.35 && !bestTopics.contains(t)) {
                bestTopics.add(t);
            }
        }
        
        System.out.println("\nTopics that are probably good links:") ;
        for (Topic t:bestTopics)
            System.out.println(" - " + t.getTitle() + "[" + _df.format(t.getWeight()) + "]" ) ;
        
        String newMarkup = _tagger.tag(doc, bestTopics, RepeatMode.ALL) ;
        System.out.println("\nAugmented markup:\n" + newMarkup + "\n") ;
    }

    public static void main(String args[]) throws Exception {
        WikipediaConfiguration conf = new WikipediaConfiguration(new File(WikiConstants.WIKI_CONFIG_PATH)) ;
        Wikipedia wikipedia = new Wikipedia(conf, false) ;

        SnippetAnnotator annotator = new SnippetAnnotator(wikipedia) ;

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in)) ;
        while (true) {
            System.out.println("Enter snippet to annotate (or ENTER to quit):") ;
            String line = reader.readLine();

            if (line.trim().length() == 0)
                break ;

            annotator.annotate(line) ;
        }
    }
}
