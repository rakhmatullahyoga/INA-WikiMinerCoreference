/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wikicoref;

import gnu.trove.map.hash.TIntDoubleHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import org.wikipedia.miner.annotation.Topic;
import org.wikipedia.miner.annotation.TopicReference;
import org.wikipedia.miner.annotation.preprocessing.PreprocessedDocument;
import org.wikipedia.miner.annotation.tagging.DocumentTagger;
import org.wikipedia.miner.comparison.ArticleComparer;
import org.wikipedia.miner.model.Article;
import org.wikipedia.miner.model.Wikipedia;
import org.wikipedia.miner.util.Position;
import wikiminer.TextFolder;

/**
 *
 * @author Rakhmatullah Yoga S
 */
public class CoreferenceTagger {
    
    private String annotatedCoref;
    private ArrayList<Set<String>> mentionCluster;
    private ArrayList<String> corefLog;

    public ArrayList<Set<String>> getMentionCluster() {
        return mentionCluster;
    }

    public String getAnnotatedCoref() {
        return annotatedCoref;
    }

    public ArrayList<String> getCorefLog() {
        return corefLog;
    }
    
    private String getTag(String anchor, Topic topic, int corefCluster) {
        return "[" + anchor + "|" + (corefCluster+1)/* + "|" + topic.getTitle() */+ "]" ;
    }
    
    public void tag(PreprocessedDocument doc, Collection<Topic> topics, DocumentTagger.RepeatMode repeatMode, Wikipedia wikipedia, boolean exactTopicLink, double corefThreshold) throws Exception {
        doc.resetRegionTracking() ;

        HashMap<Integer,Topic> topicsById = new HashMap<>() ;
        HashMap<Integer,Integer> topicCorefCluster = new HashMap<>();
        ArticleComparer _comparer = new ArticleComparer(wikipedia);
        ArrayList<Topic> topicList = new ArrayList<>(topics);
        int nbCluster = 0;

        for (Topic topic: topics) 
            topicsById.put(topic.getId(), topic) ;

        ArrayList<TopicReference> references = resolveCollisions(topics) ;
        
        // topic coreference clustering
        corefLog = new ArrayList<>();
        for(int i=0; i<topicList.size(); i++) {
            double maxRelatedness = 0.0;
            int jMax = i;
            for(int j=0; j<i; j++) {
                Article artI = wikipedia.getMostLikelyArticle(topicList.get(i).getTitle(), new TextFolder());
                if(artI==null)
                    artI = wikipedia.getArticleByTitle(topicList.get(i).getTitle());
                Article artJ = wikipedia.getMostLikelyArticle(topicList.get(j).getTitle(), new TextFolder());
                if(artJ==null)
                    artJ = wikipedia.getArticleByTitle(topicList.get(j).getTitle());
                double relatedness = _comparer.getRelatedness(artI, artJ);
                if(relatedness>maxRelatedness) {
                    maxRelatedness = relatedness;
                    jMax = j;
                }
                corefLog.add(topicList.get(j).getTitle()+" - "+topicList.get(i).getTitle()+" = "+relatedness);
            }
            // mark as coreferent
            if(!exactTopicLink&&((maxRelatedness>corefThreshold)&&(jMax!=i))) {
                topicCorefCluster.put(topicList.get(i).getId(), topicCorefCluster.get(topicList.get(jMax).getId()));
            }
            // create new cluster
            else {
                nbCluster++;
                topicCorefCluster.put(topicList.get(i).getId(), nbCluster);
            }
        }

        String originalText = doc.getOriginalText() ;
        StringBuilder wikifiedText = new StringBuilder() ;
        int lastIndex = 0 ;

        HashSet<Integer> doneIds = new HashSet<>() ;
        ArrayList<Integer> clusterID = new ArrayList<>();
        mentionCluster = new ArrayList<>();

        for (TopicReference reference:references) {
            int start = reference.getPosition().getStart() ; 
            int end = reference.getPosition().getEnd() ;
            int id = reference.getTopicId() ;

            Topic topic = topicsById.get(id) ;	

            if (repeatMode == DocumentTagger.RepeatMode.FIRST_IN_REGION)
                doneIds = doc.getDoneIdsInCurrentRegion(start) ;

            if (topic != null && (repeatMode == DocumentTagger.RepeatMode.ALL || !doneIds.contains(id))) {
                doneIds.add(id) ;
                // convert topic cluster to mention cluster
                if(!clusterID.contains(topicCorefCluster.get(topic.getId()))) {
                    clusterID.add(topicCorefCluster.get(topic.getId()));
                    mentionCluster.add(new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
                }
                int cluster = clusterID.indexOf(topicCorefCluster.get(topic.getId()));
                wikifiedText.append(originalText.substring(lastIndex, start));
                wikifiedText.append(getTag(originalText.substring(start, end), topic, cluster));
                if(!mentionCluster.get(cluster).contains(originalText.substring(start, end)))
                    mentionCluster.get(cluster).add(originalText.substring(start, end));
                lastIndex = end ;
            }
        }
        wikifiedText.append(originalText.substring(lastIndex)) ;
        this.annotatedCoref = wikifiedText.toString() ;
    }
    
    private ArrayList<TopicReference> resolveCollisions(Collection<Topic> topics) {

        //build up a list of topic references and hashmap of topic weights
        ArrayList<TopicReference> references = new ArrayList<>() ;
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
            ArrayList<TopicReference> innerReferences = new ArrayList<>() ;
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
