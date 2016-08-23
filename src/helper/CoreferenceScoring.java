/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package helper;

import java.util.ArrayList;

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

    public static double computeMUC(ArrayList<ArrayList<String>> chain1, ArrayList<ArrayList<String>> chain2) {
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

    public static void computeMUCScore(ArrayList<ArrayList<String>> keyChain, ArrayList<ArrayList<String>> responseChain) {
        recall = computeMUC(keyChain,responseChain);
        precision = computeMUC(responseChain,keyChain);
        fMeasure = 2*precision*recall/(precision+recall);
    }
}
