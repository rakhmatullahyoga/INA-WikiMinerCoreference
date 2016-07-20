/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package coreferencetest;

import gnu.trove.map.hash.TIntDoubleHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import org.wikipedia.miner.annotation.Topic;
import org.wikipedia.miner.annotation.TopicReference;
import org.wikipedia.miner.annotation.preprocessing.PreprocessedDocument;
import org.wikipedia.miner.annotation.tagging.DocumentTagger;
import org.wikipedia.miner.comparison.ArticleComparer;
import org.wikipedia.miner.model.Wikipedia;
import org.wikipedia.miner.util.Position;

/**
 *
 * @author Rakhmatullah Yoga S
 */
public class CoreferenceTagger {
    public String getTag(String anchor, Topic topic) {
//        if (topic.getTitle().compareToIgnoreCase(anchor) == 0)
//            return "[[" + anchor + "]]" ;
//        else
            return "[[" + topic.getTitle() + "|" + anchor + "|" + topic.getId() + "]]" ;
    }
    
    public String tag(PreprocessedDocument doc, Collection<Topic> topics, DocumentTagger.RepeatMode repeatMode, Wikipedia wikipedia) throws Exception {
        doc.resetRegionTracking() ;

        HashMap<Integer,Topic> topicsById = new HashMap<Integer, Topic>() ;
        HashMap<Integer,Integer> topicCorefCluster = new HashMap<Integer, Integer>();
        ArticleComparer _comparer = new ArticleComparer(wikipedia);
        ArrayList<Topic> topicList = new ArrayList<>(topics);

        // topic coreference clustering
        System.out.println("Topic comparing for coreference");
        for(int i=0; i<topicList.size(); i++) {
            for(int j=0; j<i; j++) {
                double relatedness = _comparer.getRelatedness(wikipedia.getArticleByTitle(topicList.get(i).getTitle()), wikipedia.getArticleByTitle(topicList.get(j).getTitle()));
                System.out.println(topicList.get(j).getTitle()+" - "+topicList.get(i).getTitle()+" = "+relatedness);
            }
            System.out.println("----------------");
        }

        for (Topic topic: topics) 
            topicsById.put(topic.getId(), topic) ;

        ArrayList<TopicReference> references = resolveCollisions(topics) ;

        String originalText = doc.getOriginalText() ;
        StringBuffer wikifiedText = new StringBuffer() ;
        int lastIndex = 0 ;

        HashSet<Integer> doneIds = new HashSet<Integer>() ;

        for (TopicReference reference:references) {
            int start = reference.getPosition().getStart() ; 
            int end = reference.getPosition().getEnd() ;
            int id = reference.getTopicId() ;

            Topic topic = topicsById.get(id) ;	

            if (repeatMode == DocumentTagger.RepeatMode.FIRST_IN_REGION)
                doneIds = doc.getDoneIdsInCurrentRegion(start) ;

            if (topic != null && (repeatMode == DocumentTagger.RepeatMode.ALL || !doneIds.contains(id))) {
                doneIds.add(id) ;
                wikifiedText.append(originalText.substring(lastIndex, start)) ;
                wikifiedText.append(getTag(originalText.substring(start, end), topic)) ;

                lastIndex = end ;
            }
        }

        wikifiedText.append(originalText.substring(lastIndex)) ;
        return wikifiedText.toString() ;
    }
    
    private ArrayList<TopicReference> resolveCollisions(Collection<Topic> topics) {

        //build up a list of topic references and hashmap of topic weights
        ArrayList<TopicReference> references = new ArrayList<TopicReference>() ;
        TIntDoubleHashMap topicWeights = new TIntDoubleHashMap() ;

        for(Topic topic: topics) {	
            for (Position pos: topic.getPositions()) {
                topicWeights.put(topic.getId(), topic.getWeight()) ;

                TopicReference tr = new TopicReference(null, topic.getId(), pos) ;
                references.add(tr) ;
            }
        }
        //sort references
        Collections.sort(references) ;

        for (int i=0 ; i<references.size(); i++) {
            TopicReference outerRef = references.get(i) ;

            //identify weight of this reference
            double outerWeight = topicWeights.get(outerRef.getTopicId());

            //identify references overlapped by this one, and their total weight
            Vector<TopicReference> innerReferences = new Vector<TopicReference>() ;
            double maxInnerWeight = 0 ;
            for (int j=i+1 ; j<references.size(); j++){
                TopicReference innerRef = references.get(j) ;

                if (outerRef.overlaps(innerRef)) {
                    innerReferences.add(innerRef) ;	

                    double innerWeight = topicWeights.get(innerRef.getTopicId());
                    if (innerWeight > maxInnerWeight)
                        maxInnerWeight = innerWeight ;
                } else {
                    break ;
                }
            }

            if ((maxInnerWeight*0.8) > outerWeight) {
                // want to keep the inner references
                references.remove(i) ;
                i = i-1 ;				
            } else {
                //want to keep the outer reference
                for (int j=0 ; j<innerReferences.size() ; j++) {
                    references.remove(i+1) ;
                }
            }
        }

        return references ;
    }
}
