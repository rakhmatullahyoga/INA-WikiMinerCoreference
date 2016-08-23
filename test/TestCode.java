
import arcoref.RuleInstance;
import java.util.ArrayList;

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
        RuleInstance rule = new RuleInstance();
        if(rule.isCoref())
            System.out.println(rule);
        
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
