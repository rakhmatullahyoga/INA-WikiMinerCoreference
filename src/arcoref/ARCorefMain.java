/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arcoref;

import helper.ChainHelper;
import helper.CoreferenceConstants;
import helper.CoreferenceScoring;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import wikicoref.CoreferenceTagger;
import wikicoref.WikiCorefMain;

/**
 *
 * @author rakhmatullahyoga
 */
public class ARCorefMain {
    private static ArrayList<Set<String>> keyChain;
    private static ArrayList<Set<String>> responseChain;
    private String annotated;
    
    public static void generateTrainingRules(int nbTrain) {
        for(int i=0; i<nbTrain; i++) {
            Document doc = new Document(CoreferenceConstants.RAW_TRAINING+"artikel"+i+".txt");
            doc.extractMentions();
            doc.extractRuleList();
            doc.writeRules(CoreferenceConstants.AR_RULES+"rule"+i+".csv");
        }
    }
    
    public static void buildModel(int nbTrain, double threshold) {
        DecimalFormat df = new DecimalFormat("0.0");
        df.setMaximumFractionDigits(6);
        AssociationRuleModel rules = new AssociationRuleModel();
        for(int i=0; i<nbTrain; i++)
            rules.loadRules(CoreferenceConstants.AR_LABELED+"labeled"+i+".csv");
        rules.buildModel(threshold);
        rules.writeModel(CoreferenceConstants.BASELINE_PATH+"model("+df.format(threshold)+").csv");
    }
    
    public static void training() {
        DecimalFormat df = new DecimalFormat("0.0");
        df.setMaximumFractionDigits(6);
        PrintWriter writer = null;
        try {
            File file = new File(CoreferenceConstants.RAW_TRAINING);
            File[] listFiles = file.listFiles();
            int nbTrain = listFiles.length;
            AssociationRuleModel rules = new AssociationRuleModel();
            rules.loadModel(CoreferenceConstants.BASELINE_PATH+"model-confidence.csv");
            writer = new PrintWriter(CoreferenceConstants.BASELINE_PATH+"trainresults.csv", "UTF-8");
            writer.println("Confidence;Precision;Recall;F-measure");
            for(int i=0; i<134; i++) {
                double sumPrecision = 0.0;
                double sumRecall = 0.0;
                double sumFmeasure = 0.0;
                for(int j=0; j<nbTrain; j++) {
                    System.out.println("Confidence-"+df.format(rules.getModelConfidence())+" Document-"+j);
                    Document doc = new Document(CoreferenceConstants.RAW_TRAINING+"artikel"+j+".txt");
                    doc.extractMentions();
                    doc.extractRuleList();
                    ArrayList<RuleInstance> ruleList = doc.getRuleList();
                    responseChain = rules.classifyCoreference(ruleList);
                    keyChain = ChainHelper.readKeyChain(CoreferenceConstants.KEY_TRAINING+"key"+j+".xml");
                    CoreferenceScoring.init();
                    CoreferenceScoring.computeCEAFmScore(keyChain, responseChain);
                    double precision = CoreferenceScoring.getPrecision();
                    double recall = CoreferenceScoring.getRecall();
                    double fMeasure = CoreferenceScoring.getfMeasure();
                    sumPrecision += precision;
                    sumRecall += recall;
                    sumFmeasure += fMeasure;
                }
                double avgPrecision = sumPrecision/(double)nbTrain;
                double avgRecall = sumRecall/(double)nbTrain;
                double avgFmeasure = sumFmeasure/(double)nbTrain;
                writer.println(rules.getModelConfidence()+";"+avgPrecision+";"+avgRecall+";"+avgFmeasure);
                rules.removeLastInstanceModel();
            }
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            Logger.getLogger(ARCorefMain.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            writer.close();
        }
    }
    
    public static void testing() {
        PrintWriter writer = null;
        try {
            File file = new File(CoreferenceConstants.RAW_TESTING);
            File[] listFiles = file.listFiles();
            int nbTest = listFiles.length;
            double sumPrecision = 0.0;
            double sumRecall = 0.0;
            double sumFmeasure = 0.0;
            AssociationRuleModel rules = new AssociationRuleModel();
            rules.loadModel(CoreferenceConstants.BASELINE_PATH+"model.csv");
            writer = new PrintWriter(CoreferenceConstants.BASELINE_PATH+"testresults.csv", "UTF-8");
            writer.println("Document;Precision;Recall;F-measure");
            for(int i=0; i<nbTest; i++) {
                Document doc = new Document(CoreferenceConstants.RAW_TESTING+"artikel"+i+".txt");
                doc.extractMentions();
                doc.extractRuleList();
                ArrayList<RuleInstance> ruleList = doc.getRuleList();
                responseChain = rules.classifyCoreference(ruleList);
                ChainHelper.writeCorefChain(responseChain, CoreferenceConstants.BASELINE_PATH
                        +"testresponses/response"+i+".xml");
                keyChain = ChainHelper.readKeyChain(CoreferenceConstants.KEY_TESTING+"key"+i+".xml");
                CoreferenceScoring.init();
                CoreferenceScoring.computeCEAFmScore(keyChain, responseChain);
                double precision = CoreferenceScoring.getPrecision();
                double recall = CoreferenceScoring.getRecall();
                double fMeasure = CoreferenceScoring.getfMeasure();
                sumPrecision += precision;
                sumRecall += recall;
                sumFmeasure += fMeasure;
                writer.println("Document"+i+";"+precision+";"+recall+";"+fMeasure);
            }   double avgPrecision = sumPrecision/(double)nbTest;
            double avgRecall = sumRecall/(double)nbTest;
            double avgFmeasure = sumFmeasure/(double)nbTest;
            System.out.println("\n*********************");
            System.out.println("Result score");
            System.out.println("*********************");
            System.out.println("Recall: "+avgRecall);
            System.out.println("Precision: "+avgPrecision);
            System.out.println("F-measure: "+avgFmeasure);
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            Logger.getLogger(ARCorefMain.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            writer.close();
        }
    }
    
    public static void computePerformanceMeasure() {
        PrintWriter writer = null;
        try {
            File file = new File(CoreferenceConstants.RAW_TESTING);
            File[] listFiles = file.listFiles();
            int nbTest = listFiles.length;
            double sumPrecision = 0.0;
            double sumRecall = 0.0;
            double sumFmeasure = 0.0;
            writer = new PrintWriter(CoreferenceConstants.BASELINE_PATH+"adaptedtestresults.csv", "UTF-8");
            writer.println("Document;Precision;Recall;F-measure");
            for(int i=0; i<nbTest; i++) {
                responseChain = ChainHelper.readKeyChain(CoreferenceConstants.BASELINE_PATH+"adaptedresponses/response"+i+".xml");
                keyChain = ChainHelper.readKeyChain(CoreferenceConstants.KEY_TESTING+"key"+i+".xml");
                CoreferenceScoring.init();
                CoreferenceScoring.computeCEAFmScore(keyChain, responseChain);
                double precision = CoreferenceScoring.getPrecision();
                double recall = CoreferenceScoring.getRecall();
                double fMeasure = CoreferenceScoring.getfMeasure();
                sumPrecision += precision;
                sumRecall += recall;
                sumFmeasure += fMeasure;
                writer.println("Document"+i+";"+precision+";"+recall+";"+fMeasure);
            }
            double avgPrecision = sumPrecision/(double)nbTest;
            double avgRecall = sumRecall/(double)nbTest;
            double avgFmeasure = sumFmeasure/(double)nbTest;
            System.out.println("\n*********************");
            System.out.println("Result score");
            System.out.println("*********************");
            System.out.println("Recall: "+avgRecall);
            System.out.println("Precision: "+avgPrecision);
            System.out.println("F-measure: "+avgFmeasure);
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            Logger.getLogger(ARCorefMain.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            writer.close();
        }
    }

    private static String annotate(String raw, ArrayList<Set<String>> response) {
        String annotated = raw;
        for(int i=0; i<response.size(); i++) {
            for(String str:response.get(i)) {
                annotated = annotated.replaceAll(str, CoreferenceTagger.getTag(str, null, i));
            }
        }
        return annotated;
    }
    
    public static void writeAnnotated(String newMarkup, String path) {
        try (PrintWriter writer = new PrintWriter(path, "UTF-8")) {
            writer.println(newMarkup);
            writer.close();
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            Logger.getLogger(WikiCorefMain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public static void demo() {
        AssociationRuleModel rules = new AssociationRuleModel();
        rules.loadModel(CoreferenceConstants.BASELINE_PATH+"model.csv");
        Document doc = new Document(CoreferenceConstants.PATH_DEMO+"raw.txt");
        doc.extractMentions();
        doc.extractRuleList();
        doc.writeRules(CoreferenceConstants.PATH_DEMO+"baseline/rules.csv");
        ArrayList<RuleInstance> ruleList = doc.getRuleList();
        responseChain = rules.classifyCoreference(ruleList);
        ChainHelper.writeCorefChain(responseChain, CoreferenceConstants.PATH_DEMO+"baseline/response.xml");
        System.out.println("\nInput raw text:\n"+doc.getRawText());
        String annotated = annotate(doc.getRawText(),responseChain);
        System.out.println("\nAnnotated text:\n"+annotated);
        writeAnnotated(annotated, CoreferenceConstants.PATH_DEMO+"baseline/annotated.txt");
//        keyChain = ChainHelper.readKeyChain(CoreferenceConstants.PATH_DEMO+"key.xml");
//        CoreferenceScoring.init();
//        CoreferenceScoring.computeCEAFmScore(keyChain, responseChain);
//        System.out.println("\n*********************");
//        System.out.println("Result score");
//        System.out.println("*********************");
//        System.out.println("Recall: "+CoreferenceScoring.getRecall());
//        System.out.println("Precision: "+CoreferenceScoring.getPrecision());
//        System.out.println("F-measure: "+CoreferenceScoring.getfMeasure());
    }
    
    public static void main(String[] args) {
//        File file = new File(CoreferenceConstants.AR_LABELED);
//        File[] listFiles = file.listFiles();
//        int nbTrain = listFiles.length;
//        buildModel(nbTrain,0.230769);
        
        demo();
        
//        training();
        
//        testing();
        
//        computePerformanceMeasure();
    }
}
