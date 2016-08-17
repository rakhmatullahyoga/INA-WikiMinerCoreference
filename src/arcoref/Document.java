/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arcoref;

import IndonesianNLP.IndonesianNETagger;
import IndonesianNLP.IndonesianPhraseChunker;
import IndonesianNLP.IndonesianSentenceDetector;
import IndonesianNLP.IndonesianSentenceTokenizer;
import IndonesianNLP.TreeNode;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author rakhmatullahyoga
 */
public class Document {
    private String rawText;
    private ArrayList<RuleInstance> ruleList;
    private ArrayList<ArrayList<Mention>> mentionDocumentList;
    private ArrayList<String> NEList;
    private ArrayList<String> tokenList;
    
    private static final IndonesianSentenceDetector detector = new IndonesianSentenceDetector();
    private static final IndonesianNETagger tagger = new IndonesianNETagger();
    
    public class Mention {
        String mentionStr;
        String NEtype;
        boolean isPronoun;
        
        public Mention(String str, String NE, boolean pronoun) {
            mentionStr = str;
            if(NE.equalsIgnoreCase(RuleInstance.LOCATION)||
                    NE.equalsIgnoreCase(RuleInstance.ORGANIZATION)||
                    NE.equalsIgnoreCase(RuleInstance.PERSON))
                NEtype = NE;
            else
                NEtype = RuleInstance.UNKNOWN;
            isPronoun = pronoun;
        }
    }
    
    public Document(String path) {
        NEList = new ArrayList<>();
        tokenList = new ArrayList<>();
        FileInputStream fis = null;
        try {
            File file = new File(path);
            fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();
            rawText = new String(data, "UTF-8");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Document.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Document.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fis.close();
            } catch (IOException ex) {
                Logger.getLogger(Document.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public void writeRules(String path) {
        try (PrintWriter writer = new PrintWriter(path, "UTF-8")) {
            ruleList = new ArrayList<>();
            for(int i=0; i<mentionDocumentList.size(); i++) {
                ArrayList<Mention> mentionSentence = mentionDocumentList.get(i); // mention sentence ke-i
                for(int j=0; j<mentionSentence.size(); j++) {
                    Mention mentionJ = mentionSentence.get(j);
                    for(int k=j+1; k<mentionSentence.size(); k++) {
                        // cek dgn mention satu kalimat
                        Mention mentionK = mentionSentence.get(k);
                        ruleList.add(new RuleInstance(mentionJ, mentionK, true));
                    }
                    for(int k=i+1; k<mentionDocumentList.size(); k++) {
                        ArrayList<Mention> mentionSentenceK = mentionDocumentList.get(k);
                        for(int l=0; l<mentionSentenceK.size(); l++) {
                            Mention mentionL = mentionSentenceK.get(l);
                            ruleList.add(new RuleInstance(mentionJ, mentionL, false));
                        }
                    }
                }
            }
            writer.println("\"[(mention1)-(mention2)]\",isStrMatch,"
                    + "isMatchNoCasePunc,isAbbrev,isFirstPronoun,isScndPronoun,"
                    + "isOnOneSentence,isMatchPartial,firstNameClass,"
                    + "scndNameClass");
            for(RuleInstance rule:ruleList) {
                writer.println(rule);
            }
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            Logger.getLogger(Document.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    public void extractNE(String sentence) {
        tagger.clearTagger();
        tagger.setSentence(sentence);
        tagger.tokenization();
        tagger.extractContextualFeature();
        tagger.extractMorphologicalFeature();
        tagger.extractPOSFeature();
        tagger.extractNamedEntity();
        NEList.addAll(tagger.getNE());
        tokenList.addAll(tagger.getToken());
    }
    
    public String getFirstToken(String sentence) {
        IndonesianSentenceTokenizer token = new IndonesianSentenceTokenizer();
        ArrayList<String> str = token.tokenizeSentenceWithCompositeWords(sentence);
        return str.get(0);
    }
    
    public void extractMentions() {
        ArrayList<String> splitSentence = detector.splitSentence(rawText);
        mentionDocumentList = new ArrayList<>();
        for(String str:splitSentence) {
            extractNE(str);
            IndonesianPhraseChunker chunker = new IndonesianPhraseChunker(str);
            chunker.extractPhrase();
            ArrayList<TreeNode> nodes = chunker.getPhraseTree();
            mentionDocumentList.add(getMention(nodes));
        }
    }
    
    public boolean hasNPChild(TreeNode node){
        boolean found = false;
        if(node.getChildList() == null){
            return false;
        }else{
            for(TreeNode child : node.getChildList()){
                if(child.getType().equals("NP") || child.getType().equals("PRP") || hasNPChild(child)){
                    found = true;
                    break;
                }
            }
        }
        return found;
    }
    
    public ArrayList<Mention> getMention(List<TreeNode> sentenceNodes) {
        ArrayList<Mention> mentionList = new ArrayList<>();
        for(TreeNode node : sentenceNodes){
            if(node.getChildList() == null){
                if((node.getType().equals("NP") || node.getType().equals("PRP")) && !hasNPChild(node)){
                    String phrase = node.getPhrase();
                    int idxNE = tokenList.indexOf(getFirstToken(phrase));
                    boolean isPronoun = node.getType().equals("PRP");
                    mentionList.add(new Mention(phrase, NEList.get(idxNE), isPronoun));
                }
            }
            else{
                if((node.getType().equals("NP") || node.getType().equals("PRP")) && !hasNPChild(node)){
                    String phrase = node.getPhrase();
                    int idxNE = tokenList.indexOf(getFirstToken(phrase));
                    boolean isPronoun = node.getType().equals("PRP");
                    mentionList.add(new Mention(phrase, NEList.get(idxNE), isPronoun));
                }else{
                    mentionList.addAll(getMention(node.getChildList()));
                }
            }
        }
        return mentionList;
    }
}
