/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wikiminertest;

import java.io.File;
import java.text.Normalizer;
import java.util.regex.Pattern;
import org.wikipedia.miner.db.WEnvironment;
import org.wikipedia.miner.util.WikipediaConfiguration;
import org.wikipedia.miner.util.text.CaseFolder;
import org.wikipedia.miner.util.text.PorterStemmer;
import org.wikipedia.miner.util.text.TextProcessor;

/**
 *
 * @author rakhmatullahyoga
 */
public class TextFolder extends TextProcessor {
    
    private CaseFolder caseFolder = new CaseFolder() ;
    private PorterStemmer stemmer = new PorterStemmer() ;
    private Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    @Override
    public String processText(String text) {
        String normalizedText = Normalizer.normalize(text, Normalizer.Form.NFD); 
        normalizedText = pattern.matcher(normalizedText).replaceAll("") ;
        normalizedText = caseFolder.processText(normalizedText) ;
        normalizedText = stemmer.processText(normalizedText) ;

        return normalizedText;
    }
    
    public static void main(String args[]) throws Exception {
        TextFolder folder = new TextFolder() ;
        WikipediaConfiguration conf = new WikipediaConfiguration(new File(WikiMinerTest.CONFIG_PATH)) ;
        WEnvironment.prepareTextProcessor(folder, conf, new File("tmp"), true, 3) ;
    }
    
}
