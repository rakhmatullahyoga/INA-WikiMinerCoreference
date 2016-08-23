/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arcoref;

import helper.ChainHelper;
import helper.CoreferenceConstants;
import helper.CoreferenceScoring;
import java.util.ArrayList;

/**
 *
 * @author rakhmatullahyoga
 */
public class ARCorefMain {
    private static ArrayList<ArrayList<String>> keyChain;
    private static ArrayList<ArrayList<String>> responseChain;
    
    public static void generateTrainingRules(int nbTrain) {
        for(int i=0; i<nbTrain; i++) {
            Document doc = new Document(CoreferenceConstants.RAW_TRAINING+"artikel"+i+".txt");
            doc.extractMentions();
            doc.extractRuleList();
            doc.writeRules(CoreferenceConstants.AR_RULES+"rules"+i+".csv");
        }
    }
    
    public static void buildModel(int nbTrain) {
        AssociationRuleModel rules = new AssociationRuleModel();
        for(int i=0; i<nbTrain; i++)
            rules.loadRules(CoreferenceConstants.AR_LABELED+"labeled"+i+".csv");
        rules.buildModel();
        rules.writeModel(CoreferenceConstants.BASELINE_PATH);
    }
    
    public static void testing(int nbTest) {
        double sumPrecision = 0.0;
        double sumRecall = 0.0;
        double sumFmeasure = 0.0;
        AssociationRuleModel rules = new AssociationRuleModel();
        rules.loadModel(CoreferenceConstants.BASELINE_PATH+"model.csv");
        for(int i=0; i<nbTest; i++) {
            Document doc = new Document(CoreferenceConstants.RAW_TESTING+"artikel"+i+".txt");
            doc.extractMentions();
            doc.extractRuleList();
            ArrayList<RuleInstance> ruleList = doc.getRuleList();
            ArrayList<ArrayList<String>> corefChain = rules.classifyCoreference(ruleList);
            
        }
    }
    
    public static void demo() {
        AssociationRuleModel rules = new AssociationRuleModel();
        rules.loadModel(CoreferenceConstants.BASELINE_PATH+"model.csv");
        Document doc = new Document(CoreferenceConstants.PATH_DEMO+"raw.txt");
        doc.extractMentions();
        doc.extractRuleList();
        doc.writeRules(CoreferenceConstants.AR_RULES+"rules.csv");
        ArrayList<RuleInstance> ruleList = doc.getRuleList();
        responseChain = rules.classifyCoreference(ruleList);
        ChainHelper.writeCorefChain(responseChain, CoreferenceConstants.BASELINE_PATH+"response.xml");
        keyChain = ChainHelper.readKeyChain(CoreferenceConstants.PATH_DEMO+"key.xml");
        CoreferenceScoring.init();
        CoreferenceScoring.computeMUCScore(keyChain, responseChain);
        System.out.println("\n*********************");
        System.out.println("Result score");
        System.out.println("*********************");
        System.out.println("Recall: "+CoreferenceScoring.getRecall());
        System.out.println("Precision: "+CoreferenceScoring.getPrecision());
        System.out.println("F-measure: "+CoreferenceScoring.getfMeasure());
    }
    
    public static void main(String[] args) {
        demo();
    }
}
