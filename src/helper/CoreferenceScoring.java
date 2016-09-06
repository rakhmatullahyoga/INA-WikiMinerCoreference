/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 *
 * @author rakhmatullahyoga
 */
public class CoreferenceScoring {
    private static double recall,precision,fMeasure;

    public static double getRecall() {
        return recall;
    }

    public static double getPrecision() {
        return precision;
    }

    public static double getfMeasure() {
        return fMeasure;
    }
    
    public static void init() {
        recall = 0.0;
        precision = 0.0;
        fMeasure = 0.0;
    }
    
    private static double computeCEAFm(ArrayList<Set<String>> chain1, ArrayList<Set<String>> chain2) {
        HashMap<Integer, Integer> keyToValue = new HashMap<>();
        HashMap<Integer, Integer> valueToKey = new HashMap<>();
        HashMap<Integer, Integer> keyToSimilarMention = new HashMap<>();
        int mention1Count = 0;
        for(Set<String> entity1:chain1) {
            int maxSimilarity = 0;
            int idxSimilar = -1;
            for(Set<String> entity2:chain2) {
                int similarMention = 0;
                for(String mention:entity2) {
                    if(entity1.contains(mention)) {
                        similarMention++;
                    }
                }
                if(similarMention>maxSimilarity) {
                    maxSimilarity = similarMention;
                    idxSimilar = chain2.indexOf(entity2);
                }
            }
            if(idxSimilar != -1) {
                if(keyToValue.containsValue(idxSimilar)) {
                    if(maxSimilarity>keyToSimilarMention.get(valueToKey.get(idxSimilar))) {
                        keyToValue.remove(valueToKey.get(idxSimilar));
                        keyToSimilarMention.remove(valueToKey.get(idxSimilar));
                        valueToKey.remove(idxSimilar);
                        keyToValue.put(chain1.indexOf(entity1), idxSimilar);
                        valueToKey.put(idxSimilar, chain1.indexOf(entity1));
                        keyToSimilarMention.put(chain1.indexOf(entity1), maxSimilarity);
                    }
                }
                else {
                    keyToValue.put(chain1.indexOf(entity1), idxSimilar);
                    valueToKey.put(idxSimilar, chain1.indexOf(entity1));
                    keyToSimilarMention.put(chain1.indexOf(entity1), maxSimilarity);
                }
            }
            mention1Count += entity1.size();
        }
        Set<Integer> keySet = keyToValue.keySet();
        int sumSimilar = 0;
        for(Integer idxKey:keySet) {
            sumSimilar += keyToSimilarMention.get(idxKey);
        }
        return (double)sumSimilar/(double)mention1Count;
    }
    
    private static double computeCEAFe(ArrayList<Set<String>> chain1, ArrayList<Set<String>> chain2) {
        HashMap<Integer, Integer> keyToValue = new HashMap<>();
        HashMap<Integer, Integer> valueToKey = new HashMap<>();
        HashMap<Integer, Integer> keyToSimilarMention = new HashMap<>();
        for(Set<String> entity1:chain1) {
            int maxSimilarity = 0;
            int idxSimilar = -1;
            for(Set<String> entity2:chain2) {
                int similarMention = 0;
                for(String mention:entity2) {
                    if(entity1.contains(mention)) {
                        similarMention++;
                    }
                }
                if(similarMention>maxSimilarity) {
                    maxSimilarity = similarMention;
                    idxSimilar = chain2.indexOf(entity2);
                }
            }
            if(idxSimilar != -1) {
                if(keyToValue.containsValue(idxSimilar)) {
                    if(maxSimilarity>keyToSimilarMention.get(valueToKey.get(idxSimilar))) {
                        keyToValue.remove(valueToKey.get(idxSimilar));
                        keyToSimilarMention.remove(valueToKey.get(idxSimilar));
                        valueToKey.remove(idxSimilar);
                        keyToValue.put(chain1.indexOf(entity1), idxSimilar);
                        valueToKey.put(idxSimilar, chain1.indexOf(entity1));
                        keyToSimilarMention.put(chain1.indexOf(entity1), maxSimilarity);
                    }
                }
                else {
                    keyToValue.put(chain1.indexOf(entity1), idxSimilar);
                    valueToKey.put(idxSimilar, chain1.indexOf(entity1));
                    keyToSimilarMention.put(chain1.indexOf(entity1), maxSimilarity);
                }
            }
        }
        Set<Integer> keySet = keyToValue.keySet();
        double sumSimilar = 0.0;
        for(Integer idxKey:keySet) {
            sumSimilar += ((double)(2*keyToSimilarMention.get(idxKey))/(double)(chain1.get(idxKey).size()+chain2.get(keyToValue.get(idxKey)).size()));
        }
        return (double)sumSimilar/(double)chain1.size();
    }

    private static double computeB3(ArrayList<Set<String>> chain1, ArrayList<Set<String>> chain2) {
        double pembilang = 0.0;
        double penyebut = 0.0;
        for(int i=0; i<chain1.size(); i++) {
            for(int j=0; j<chain2.size(); j++) {
                int nbIntersect = 0;
                for(String str : chain2.get(j)) {
                    if(chain1.get(i).contains(str)) {
                        nbIntersect++;
                    }
                }
                pembilang += (double)Math.pow(nbIntersect, 2)/(double)chain1.get(i).size();
            }
            penyebut += chain1.get(i).size();
        }
        return (double)pembilang/(double)penyebut;
    }

    private static double computeMUC(ArrayList<Set<String>> chain1, ArrayList<Set<String>> chain2) {
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
        }
        return (double)pembilang/(double)penyebut;
    }
    
    private static double computeFmeasure(double precision, double recall) {
        return 2*precision*recall/(precision+recall);
    }

    public static void computeMUCScore(ArrayList<Set<String>> keyChain, ArrayList<Set<String>> responseChain) {
        recall = computeMUC(keyChain,responseChain);
        precision = computeMUC(responseChain,keyChain);
        fMeasure = computeFmeasure(precision,recall);
    }

    public static void computeB3Score(ArrayList<Set<String>> keyChain, ArrayList<Set<String>> responseChain) {
        recall = computeB3(keyChain,responseChain);
        precision = computeB3(responseChain,keyChain);
        fMeasure = computeFmeasure(precision,recall);
    }

    public static void computeCEAFmScore(ArrayList<Set<String>> keyChain, ArrayList<Set<String>> responseChain) {
        recall = computeCEAFm(keyChain,responseChain);
        precision = computeCEAFm(responseChain,keyChain);
        fMeasure = computeFmeasure(precision,recall);
    }

    public static void computeCEAFeScore(ArrayList<Set<String>> keyChain, ArrayList<Set<String>> responseChain) {
        recall = computeCEAFe(keyChain,responseChain);
        precision = computeCEAFe(responseChain,keyChain);
        fMeasure = computeFmeasure(precision,recall);
    }
}
