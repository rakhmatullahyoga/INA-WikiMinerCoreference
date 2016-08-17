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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author rakhmatullahyoga
 */
public class RuleInstance {
    public static final String ORGANIZATION = "ORGANIZATION";
    public static final String PERSON = "PERSON";
    public static final String LOCATION = "LOCATION";
    public static final String UNKNOWN = "OTHER";
    
    private boolean isStrMatch;
    private boolean isMatchNoCasePunc;
    private boolean isAbbrev;
    private boolean isFirstPronoun;
    private boolean isScndPronoun;
    private boolean isOnOneSentence;
    private boolean isMatchPartial;
    private String firstNameClass;
    private String scndNameClass;
    
    private String mention1;
    private String mention2;

    public RuleInstance() {
        
    }

    public RuleInstance(Mention m1, Mention m2, boolean oneSentence) {
        mention1 = m1.mentionStr;
        mention2 = m2.mentionStr;
        isStrMatch = m1.mentionStr.equalsIgnoreCase(m2.mentionStr);
        isFirstPronoun = m1.isPronoun;
        isScndPronoun = m2.isPronoun;
        isOnOneSentence = oneSentence;
        firstNameClass = m1.NEtype;
        scndNameClass = m2.NEtype;
        isMatchPartial = (mention1.contains(mention2)||mention2.contains(mention1));
        Pattern p = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
        Matcher match1 = p.matcher(mention1);
        Matcher match2 = p.matcher(mention2);
        isMatchNoCasePunc = !(match1.find()||match2.find());
    }
    
    @Override
    public boolean equals(Object other) {
        if(!(other instanceof RuleInstance))
            return false;
        RuleInstance otherInst = (RuleInstance) other;
        return (this.isAbbrev==otherInst.isAbbrev 
                && this.isFirstPronoun==otherInst.isFirstPronoun
                && this.isMatchNoCasePunc==otherInst.isMatchNoCasePunc 
                && this.isMatchPartial==otherInst.isMatchPartial
                && this.isScndPronoun==otherInst.isScndPronoun 
                && this.isStrMatch==otherInst.isStrMatch
                && this.isOnOneSentence==otherInst.isOnOneSentence 
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
        return "\"[("+mention1+")-("+mention2+")]\","+isStrMatch+","+
                isMatchNoCasePunc+","+isAbbrev+","+isFirstPronoun+","+
                isScndPronoun+","+isOnOneSentence+","+isMatchPartial+","+
                firstNameClass+","+scndNameClass;
    }
    
    public boolean checkAbbrev(String abbrev, String trueWords) {
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
