/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wikiminer;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.wikipedia.miner.comparison.ArticleComparer;
import org.wikipedia.miner.model.Article;
import org.wikipedia.miner.model.Category;
import org.wikipedia.miner.model.Label;
import org.wikipedia.miner.util.WikipediaConfiguration;

/**
 *
 * @author rakhmatullahyoga
 */
public class ComparingWikisaurus extends Wikisaurus {
    
    ArticleComparer _comparer ;
    DecimalFormat _df = new DecimalFormat("#0%") ;
    
    public ComparingWikisaurus(WikipediaConfiguration conf) throws Exception {
        super(conf);
        _comparer = new ArticleComparer(_wikipedia);
    }
    
    private List<Article> gatherRelatedTopics(Article art) {
        HashSet<Integer> relatedIds = new HashSet<>() ;
        relatedIds.add(art.getId()) ;

        ArrayList<Article> relatedTopics = new ArrayList<>() ;

        //gather from out-links
        for (Article outLink:art.getLinksOut()) {
            if (!relatedIds.contains(outLink.getId())) {
                relatedIds.add(outLink.getId()) ;
                relatedTopics.add(outLink) ;
            }
        }

        //gather from in-links
        for (Article inLink:art.getLinksIn()) {
            if (!relatedIds.contains(inLink.getId())) {
                relatedIds.add(inLink.getId()) ;
                relatedTopics.add(inLink) ;
            }
        }   

        //gather from category siblings
        for (Category cat:art.getParentCategories()){
            for (Article sibling:cat.getChildArticles()) {
                if (!relatedIds.contains(sibling.getId())) {
                    relatedIds.add(sibling.getId()) ;
                    relatedTopics.add(sibling) ;
                }
            }
        }

        return relatedTopics ;
    }
    
    @Override
    protected void displayRelatedTopics(Label.Sense sense) throws Exception {
        List<Article> relatedTopics = gatherRelatedTopics(sense) ;
        relatedTopics = sortTopics(sense, relatedTopics) ;

        //now trim the list if necessary
        if (relatedTopics.size() > 25)
            relatedTopics = relatedTopics.subList(1,25) ;

        System.out.println("\nRelated topics:") ;
        for (Article art:relatedTopics) 
            System.out.println(" - " + art.getTitle() + " " + _df.format(art.getWeight())) ;
    }
    
    private List<Article> sortTopics(Article queryTopic, List<Article>relatedTopics) throws Exception {
        //weight the related articles according to how strongly they relate to sense article
        for (Article art:relatedTopics) 
            art.setWeight(_comparer.getRelatedness(art, queryTopic)) ;

        //Now that the weight attribute is set, sorting will be in descending order of weight.
        //If weight was not set, it would be in ascending order of id.  
        Collections.sort(relatedTopics) ;
        return relatedTopics ;
    }
    
    public static void main(String args[]) throws Exception {
        WikipediaConfiguration conf = new WikipediaConfiguration(new File(WikiConstants.WIKI_CONFIG_PATH)) ;

        ComparingWikisaurus thesaurus = new ComparingWikisaurus(conf) ;
        thesaurus.run() ;
    }
    
}
