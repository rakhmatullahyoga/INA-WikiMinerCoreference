/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wikiminer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import org.wikipedia.miner.model.Article;
import org.wikipedia.miner.model.Label;
import org.wikipedia.miner.model.Wikipedia;
import org.wikipedia.miner.util.WikipediaConfiguration;

/**
 *
 * @author rakhmatullahyoga
 */
public class Wikisaurus {
    BufferedReader _input ;
    Wikipedia _wikipedia ;

    public Wikisaurus(WikipediaConfiguration conf) {
        _input = new BufferedReader(new InputStreamReader(System.in)) ;
        _wikipedia = new Wikipedia(conf, false) ; 
    }
    
    protected void displayDefinition(Label.Sense sense) throws Exception {
        System.out.println(sense.getSentenceMarkup(0)) ;
    }
    
    protected void displayAlternativeLabels(Label.Sense sense) throws Exception {
        System.out.println("\nAlternative labels:") ;
        for (Article.Label label:sense.getLabels()) 
            System.out.println(label.getText()) ;
    }
    
    protected void displayRelatedTopics(Label.Sense sense) throws Exception {
        System.out.println("\nRelated topics:") ;
        for (Article art:sense.getLinksOut()) 
            System.out.println(" - " + art.getTitle()) ;
    }
    
    protected void displaySense(Label.Sense sense) throws Exception {
        System.out.println("==" + sense.getTitle() + "==") ;
        displayDefinition(sense) ;
        displayAlternativeLabels(sense) ;
        displayRelatedTopics(sense) ;
    }

    public void run() throws IOException, Exception {
        String term ;
        while ((term = getString("Please enter a term to look up in Wikipedia"))!= null) {

            Label label = _wikipedia.getLabel(term) ;

            if (!label.exists()) {
                System.out.println("I have no idea what '" + term + "' is") ;
            } else {
                Label.Sense[] senses = label.getSenses() ;
                if (senses.length == 1) {
                    displaySense(senses[0]) ;
                } else {
                    System.out.println("'" + term + "' could mean several things:") ;
                    for (int i=0 ; i<senses.length ; i++) {
                        System.out.println(" - [" + (i+1) + "] " + senses[i].getTitle() + " " + senses[i].getPriorProbability()) ;
                    }
                    Integer senseIndex = getInt("So which do you want?", 1, senses.length) ;
                    if (senseIndex != null)
                        displaySense(senses[senseIndex-1]) ;
                }
            }
        }
    }
    
    private String getString(String prompt) throws IOException {

        System.out.println(prompt + "(or ENTER for none)") ;

        String line = _input.readLine() ;

        if (line.trim().equals(""))
            line = null ;

        return line ;       
    }
    
    private Integer getInt(String prompt, int min, int max) throws IOException {

        while (true) {

            System.out.println(prompt + " (" + min + " - " + max + " or ENTER for none)") ;

            String line = _input.readLine() ;
            if (line.trim().equals(""))
                return null ;

            try { 
                Integer val = Integer.parseInt(line) ;
                if (val >= min && val <= max)
                    return val ;
            } catch (Exception e) {

            }

            System.out.println("Invalid input, try again") ;
        }

    }
    
    public static void main(String args[]) throws Exception {
        WikipediaConfiguration conf = new WikipediaConfiguration(new File(WikiConstants.WIKI_CONFIG_PATH)) ;

        Wikisaurus thesaurus = new Wikisaurus(conf) ;
        thesaurus.run() ;
    }
}
