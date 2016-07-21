/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wikiminertest;

import java.io.File;
import org.wikipedia.miner.util.WikipediaConfiguration;

/**
 *
 * @author rakhmatullahyoga
 */
public class FoldedWikisaurus extends Wikisaurus {
    
    public FoldedWikisaurus(WikipediaConfiguration conf) {
        super(conf);
    }
    
    public static void main(String args[]) throws Exception {
        WikipediaConfiguration conf = new WikipediaConfiguration(new File(WikiConstants.WIKI_CONFIG_PATH)) ;
        conf.setDefaultTextProcessor(new TextFolder()) ;

        FoldedWikisaurus thesaurus = new FoldedWikisaurus(conf) ;
        thesaurus.run() ;
    }
    
}
