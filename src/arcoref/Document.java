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

    public ArrayList<RuleInstance> getRuleList() {
        return ruleList;
    }
    
    public void extractRuleList() {
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
    }
    
    public void writeRules(String path) {
        try (PrintWriter writer = new PrintWriter(path, "UTF-8")) {
            writer.println("mentions;isStrMatch;"
                    + "isMatchNoCasePunc;isAbbrev;isFirstPronoun;isScndPronoun;"
                    + "isOnOneSentence;isMatchPartial;firstNameClass;"
                    + "scndNameClass");
            for(RuleInstance rule:ruleList) {
                writer.println("\"[("+rule.getMention1()+")-("+rule.getMention2()+")]\";"+rule);
            }
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            Logger.getLogger(Document.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void writeMentions(String path) {
        try (PrintWriter writer = new PrintWriter(path, "UTF-8")) {
            for(ArrayList<Mention> mentionPerSentence:mentionDocumentList) {
                for(Mention mention:mentionPerSentence) {
                    writer.println(mention.mentionStr);
                }
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
    
    public String getAvailableToken(String phrase) {
        IndonesianSentenceTokenizer token = new IndonesianSentenceTokenizer();
        ArrayList<String> str = token.tokenizeSentenceWithCompositeWords(phrase);
        int i=0;
        boolean found=false;
        while(!found&&(i<str.size())) {
            if(tokenList.contains(str.get(i)))
                found = true;
            else
                i++;
        }
        if(found)
            return str.get(i);
        return str.get(0);
    }
    
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
        if(node.getChildList() == null) {
            return false;
        }
        else {
            for(TreeNode child : node.getChildList()) {
                if(child.getType().equals("NP") || child.getType().equals("PRP") || hasNPChild(child)) {
                    found = true;
                    break;
                }
            }
        }
        return found;
    }
    
    public ArrayList<Mention> getMention(List<TreeNode> sentenceNodes) {
        ArrayList<Mention> mentionList = new ArrayList<>();
        for(TreeNode node : sentenceNodes) {
            if(node.getChildList() != null) {
                if((node.getType().equals("NP") || node.getType().equals("PRP")) && !hasNPChild(node)) {
                    String phrase = node.getPhrase();
                    if(phrase.charAt(phrase.length()-1)==' ')
                        phrase = phrase.substring(0, phrase.length()-1);
                    ArrayList<String> splitParaenthesis = splitParaenthesis(phrase);
                    ArrayList<String> splitAppositive = new ArrayList<>();
                    for(String str:splitParaenthesis) {
                        splitAppositive.addAll(splitAppositive(str));
                    }
                    for(String str:splitAppositive) {
                        int idxNE = tokenList.indexOf(getAvailableToken(str.trim()));
                        boolean isPronoun = node.getType().equals("PRP");
                        if(idxNE!=-1)
                            mentionList.add(new Mention(str.trim(), NEList.get(idxNE), isPronoun));
                        else
                            mentionList.add(new Mention(str.trim(), RuleInstance.UNKNOWN, isPronoun));
                    }
                }
                else {
                    mentionList.addAll(getMention(node.getChildList()));
                }
            }
        }
        return mentionList;
    }
}
