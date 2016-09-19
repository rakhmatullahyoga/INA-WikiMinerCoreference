
import arcoref.Document;
import helper.ChainHelper;
import helper.CoreferenceConstants;
import java.io.File;
import java.util.ArrayList;
import java.util.Set;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author rakhmatullahyoga
 */
public class TestCode {
    public ArrayList<String> splitAppositive(String phrase) {
        ArrayList<String> mentionList = new ArrayList<>();
        String[] splitPhrase = phrase.split(", ");
        for(int i=0; i<splitPhrase.length; i++) {
            if(splitPhrase[i].charAt(0)==' ')
                splitPhrase[i] = splitPhrase[i].substring(1);
            mentionList.add(splitPhrase[i]);
        }
        return mentionList;
    }
    
    public ArrayList<String> splitParaenthesis(String phrase) {
        ArrayList<String> mentionList = new ArrayList<>();
        String[] split = phrase.split(" \\(");
        for(int i=0; i<split.length; i++) {
            if(split[i].contains(")"))
                split[i] = split[i].substring(0,split[i].indexOf(")"));
            mentionList.add(split[i]);
        }
        return mentionList;
    }
    
    public static void main(String[] args) {
        File fileDir = new File("./data/corefdata/raw/training/");
        int nbData = fileDir.listFiles().length;
        int totalMentionsTraining = 0;
        int totalMentionsTesting = 0;
        int nbTrainEntity = 0;
        int nbTestEntity = 0;
        int nbTrainProperNoun = 0;
        int nbTestProperNoun = 0;
        int nbTrainSingletonEntity = 0;
        int nbTestSingletonEntity = 0;
        System.out.println("Processing...");
        for(int i=0; i<nbData; i++) {
            System.out.println("data (train) - "+i);
            Document doc = new Document("./data/corefdata/raw/training/artikel"+i+".txt");
            doc.extractMentions();
            ArrayList<Set<String>> KeyChain = ChainHelper.readKeyChain(CoreferenceConstants.KEY_TRAINING+"key"+i+".xml");
            nbTrainEntity += KeyChain.size();
            for(Set<String> entityMention:KeyChain) {
                nbTrainProperNoun += entityMention.size();
                if(entityMention.size()==1)
                    nbTrainSingletonEntity ++;
            }
            totalMentionsTraining += doc.getNbUniqueMentions();
        }
        fileDir = new File("./data/corefdata/raw/testing/");
        nbData = fileDir.listFiles().length;
        for(int i=0; i<nbData; i++) {
            System.out.println("data (test) - "+i);
            Document doc = new Document("./data/corefdata/raw/testing/artikel"+i+".txt");
            doc.extractMentions();
            ArrayList<Set<String>> KeyChain = ChainHelper.readKeyChain(CoreferenceConstants.KEY_TESTING+"key"+i+".xml");
            nbTestEntity += KeyChain.size();
            for(Set<String> entityMention:KeyChain) {
                nbTestProperNoun += entityMention.size();
                if(entityMention.size()==1)
                    nbTestSingletonEntity ++;
            }
            totalMentionsTesting += doc.getNbUniqueMentions();
        }
        
        System.out.println("\n*********************");
        System.out.println("Mention & entity analytics");
        System.out.println("*********************");
        System.out.println("Mentions:");
        System.out.println("\tTrain: "+totalMentionsTraining);
        System.out.println("\tTest: "+totalMentionsTesting);
        System.out.println("Entity:");
        System.out.println("\tTrain: "+nbTrainEntity);
        System.out.println("\tTest: "+nbTestEntity);
        System.out.println("Proper noun:");
        System.out.println("\tTrain: "+nbTrainProperNoun);
        System.out.println("\tTest: "+nbTestProperNoun);
        System.out.println("Singleton entity:");
        System.out.println("\tTrain: "+nbTrainSingletonEntity);
        System.out.println("\tTest: "+nbTestSingletonEntity);
        
//        TestCode test = new TestCode();
//        String testString = "Partai Demokrasi Indonesia Perjuangan";
//        ArrayList<String> splitParaenthesis = test.splitParaenthesis(testString);
//        ArrayList<String> splitAppositive = new ArrayList<>();
//        for(String str:splitParaenthesis) {
//            System.out.println(str);
//            splitAppositive.addAll(test.splitAppositive(str));
//        }
//        System.out.println();
//        for(String str:splitAppositive) {
//            System.out.println(str);
//        }
//        Document doc = new Document(CorefRuleConstants.RAW_PATH);
//        doc.extractMentions();
//        doc.extractRuleList();
//        doc.writeRules(CorefRuleConstants.RULES_CSV);
//        AssociationRuleModel rules = new AssociationRuleModel();
//        rules.loadRules(CorefRuleConstants.LABEL_CSV);
//        rules.buildModel();
//        rules.writeRules("./data/baseline/rules1.csv");
    }
}
