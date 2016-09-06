/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arcoref;

import IndonesianNLP.IndonesianSentenceTokenizer;
import arcoref.Document.Mention;
import java.util.ArrayList;
import java.util.Objects;

/**
 *
 * @author rakhmatullahyoga
 */
public class RuleInstance {
    public static final String ORGANIZATION = "ORGANIZATION";
    public static final String PERSON = "PERSON";
    public static final String LOCATION = "LOCATION";
    public static final String UNKNOWN = "OTHER";
    
    private Boolean isStrMatch;
    private Boolean isMatchNoCasePunc;
    private Boolean isAbbrev;
    private Boolean isFirstPronoun;
    private Boolean isScndPronoun;
    private Boolean isOnOneSentence;
    private Boolean isMatchPartial;
    private String firstNameClass;
    private String scndNameClass;
    
    private String mention1;
    private String mention2;
    
    private Boolean isCorefLabel;
    private double corefConfidence;
    private double corefSupport;

    public RuleInstance() { // do nothing, only for testing purpose
        isStrMatch = null;
        isFirstPronoun = null;
        isScndPronoun = null;
        isOnOneSentence = null;
        firstNameClass = null;
        scndNameClass = null;
        isMatchPartial = null;
        isMatchNoCasePunc = null;
        isAbbrev = null;
    }

    public RuleInstance(Mention m1, Mention m2, boolean oneSentence) {
        mention1 = m1.mentionStr;
        mention2 = m2.mentionStr;
        isStrMatch = mention1.equalsIgnoreCase(mention2);
        isFirstPronoun = m1.isPronoun;
        isScndPronoun = m2.isPronoun;
        isOnOneSentence = oneSentence;
        firstNameClass = m1.NEtype;
        scndNameClass = m2.NEtype;
        isMatchPartial = (mention1.contains(mention2)||mention2.contains(mention1));
        isMatchNoCasePunc = !(isHavePunctuation(mention1)||isHavePunctuation(mention2));
        isAbbrev = checkAbbrev(mention1, mention2)||checkAbbrev(mention2, mention1);
    }
    
    public RuleInstance(String[] rulesCsv) {
        isStrMatch = rulesCsv[1].equalsIgnoreCase("true");
        isMatchNoCasePunc = rulesCsv[2].equalsIgnoreCase("true");
        isAbbrev = rulesCsv[3].equalsIgnoreCase("true");
        isFirstPronoun = rulesCsv[4].equalsIgnoreCase("true");
        isScndPronoun = rulesCsv[5].equalsIgnoreCase("true");
        isOnOneSentence = rulesCsv[6].equalsIgnoreCase("true");
        isMatchPartial = rulesCsv[7].equalsIgnoreCase("true");
        firstNameClass = rulesCsv[8];
        scndNameClass = rulesCsv[9];
        isCorefLabel = rulesCsv[10].equalsIgnoreCase("true");
        if(rulesCsv.length==13)
            corefConfidence = Double.parseDouble(rulesCsv[12]);
    }
    
    @Override
    public boolean equals(Object other) {
        if(!(other instanceof RuleInstance))
            return false;
        RuleInstance otherInst = (RuleInstance) other;
        return (Objects.equals(this.isAbbrev, otherInst.isAbbrev) 
                && Objects.equals(this.isFirstPronoun, otherInst.isFirstPronoun)
                && Objects.equals(this.isMatchNoCasePunc, otherInst.isMatchNoCasePunc) 
                && Objects.equals(this.isMatchPartial, otherInst.isMatchPartial)
                && Objects.equals(this.isScndPronoun, otherInst.isScndPronoun) 
                && Objects.equals(this.isStrMatch, otherInst.isStrMatch)
                && Objects.equals(this.isOnOneSentence, otherInst.isOnOneSentence) 
                && this.firstNameClass.equalsIgnoreCase(otherInst.firstNameClass)
                && this.scndNameClass.equalsIgnoreCase(otherInst.scndNameClass));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 43 * hash + (this.isStrMatch ? 1 : 0);
        hash = 43 * hash + (this.isMatchNoCasePunc ? 1 : 0);
        hash = 43 * hash + (this.isAbbrev ? 1 : 0);
        hash = 43 * hash + (this.isFirstPronoun ? 1 : 0);
        hash = 43 * hash + (this.isScndPronoun ? 1 : 0);
        hash = 43 * hash + (this.isOnOneSentence ? 1 : 0);
        hash = 43 * hash + (this.isMatchPartial ? 1 : 0);
        hash = 43 * hash + Objects.hashCode(this.firstNameClass);
        hash = 43 * hash + Objects.hashCode(this.scndNameClass);
        return hash;
    }

    @Override
    public String toString() {
        return isStrMatch+";"+ isMatchNoCasePunc+";"+isAbbrev+";"+
                isFirstPronoun+";"+ isScndPronoun+";"+isOnOneSentence+";"+
                isMatchPartial+";"+ firstNameClass+";"+scndNameClass;
    }
    
    private boolean isHavePunctuation(String phrase) {
        IndonesianSentenceTokenizer token = new IndonesianSentenceTokenizer();
        ArrayList<String> str = token.tokenizeSentenceWithCompositeWords(phrase);
        return str.contains(",")||str.contains(".")||str.contains(":")
                ||str.contains(";")||str.contains("'")||str.contains("\"");
    }
    
    private boolean checkAbbrev(String abbrev, String trueWords) {
        if(abbrev.length()>=trueWords.length())
            return false;
        else {
            IndonesianSentenceTokenizer tokenizer = new IndonesianSentenceTokenizer();
            ArrayList<String> token = tokenizer.tokenizeSentence(trueWords);
            if(token.size()!=abbrev.length())
                return false;
            boolean abbreviation = true;
            int i=0; // index iterasi token
            while(i<token.size()&&abbreviation) {
                if(Character.toLowerCase(token.get(i).charAt(0))!=Character.toLowerCase(abbrev.charAt(i)))
                    abbreviation = false;
                i++;
            }
            return abbreviation;
        }
    }

    public double getCorefSupport() {
        return corefSupport;
    }

    public void setCorefSupport(double corefSupport) {
        this.corefSupport = corefSupport;
    }

    public double getCorefConfidence() {
        return corefConfidence;
    }

    public void setCorefConfidence(double corefConfidence) {
        this.corefConfidence = corefConfidence;
    }

    public String getMention1() {
        return mention1;
    }

    public String getMention2() {
        return mention2;
    }

    public boolean isCoref() {
        return isCorefLabel;
    }

    public void setLabel(boolean isCorefLabel) {
        this.isCorefLabel = isCorefLabel;
    }

    public void setMention1(String mention1) {
        this.mention1 = mention1;
    }

    public void setMention2(String mention2) {
        this.mention2 = mention2;
    }
    
    public void setIsStrMatch(boolean isStrMatch) {
        this.isStrMatch = isStrMatch;
    }

    public void setIsMatchNoCasePunc(boolean isMatchNoCasePunc) {
        this.isMatchNoCasePunc = isMatchNoCasePunc;
    }

    public void setIsAbbrev(boolean isAbbrev) {
        this.isAbbrev = isAbbrev;
    }

    public void setIsFirstPronoun(boolean isFirstPronoun) {
        this.isFirstPronoun = isFirstPronoun;
    }

    public void setIsScndPronoun(boolean isScndPronoun) {
        this.isScndPronoun = isScndPronoun;
    }

    public void setIsOnOneSentence(boolean isOnOneSentence) {
        this.isOnOneSentence = isOnOneSentence;
    }

    public void setIsMatchPartial(boolean isMatchPartial) {
        this.isMatchPartial = isMatchPartial;
    }

    public void setFirstNameClass(String firstNameClass) {
        this.firstNameClass = firstNameClass;
    }

    public void setScndNameClass(String scndNameClass) {
        this.scndNameClass = scndNameClass;
    }
}
