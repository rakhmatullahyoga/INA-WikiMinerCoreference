/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arcoref;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author rakhmatullahyoga
 */
public class AssociationRuleModel {
    private ArrayList<RuleInstance> model;
    private ArrayList<RuleInstance> labeledRuleList;
    
    public AssociationRuleModel() {
        model = new ArrayList<>();
        labeledRuleList = new ArrayList<>();
    }
    
    public ArrayList<Set<String>> classifyCoreference(ArrayList<RuleInstance> ruleList) {
        ArrayList<Set<String>> responseChain = new ArrayList<>();
        for(RuleInstance rule:ruleList) {
            String m1 = rule.getMention1();
            String m2 = rule.getMention2();
            if(getClusterIdx(responseChain,m1)==-1) { // m1 belum masuk cluster
                TreeSet<String> newCluster = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                newCluster.add(m1);
                responseChain.add(newCluster);
            }
            if(model.indexOf(rule)!=-1) { // rule terdapat pada model
                if(model.get(model.indexOf(rule)).isCoref()) { // corefer berdasarkan model
                    if(!responseChain.get(getClusterIdx(responseChain, m1)).contains(m2)) {
                        if(getClusterIdx(responseChain,m2)==-1)
                            responseChain.get(getClusterIdx(responseChain, m1)).add(m2);
                        else {
                            Set<String> removed = responseChain.remove(getClusterIdx(responseChain,m1));
                            responseChain.get(getClusterIdx(responseChain, m2)).addAll(removed);
                        }
                    }
                }
            }
        }
        return responseChain;
    }

    private int getClusterIdx(ArrayList<Set<String>> responseChain, String mention) {
        boolean found = false;
        int i=0;
        while(i<responseChain.size()&&!found) {
            if(responseChain.get(i).contains(mention))
                found = true;
            else
                i++;
        }
        if(!found)
            return -1;
        else
            return i;
    }
    
    public void buildModel() {
        int positiveLabel, negativeLabel;
        Set<RuleInstance> uniqueRules = new HashSet<>(labeledRuleList);
        for(RuleInstance rule:uniqueRules) {
            positiveLabel = 0;
            negativeLabel = 0;
            for(RuleInstance ruleInst:labeledRuleList) {
                if(ruleInst.equals(rule)) {
                    if(ruleInst.isCoref())
                        positiveLabel++;
                    else
                        negativeLabel++;
                }
            }
            rule.setCorefSupport((double)(positiveLabel+negativeLabel)/(double)labeledRuleList.size());
            rule.setCorefConfidence((double)positiveLabel/(double)(positiveLabel+negativeLabel));
            rule.setLabel(positiveLabel>negativeLabel);
            model.add(rule);
        }
    }
    
    public void buildModel(double threshold) {
        int positiveLabel, negativeLabel;
        Set<RuleInstance> uniqueRules = new HashSet<>(labeledRuleList);
        for(RuleInstance rule:uniqueRules) {
            positiveLabel = 0;
            negativeLabel = 0;
            for(RuleInstance ruleInst:labeledRuleList) {
                if(ruleInst.equals(rule)) {
                    if(ruleInst.isCoref())
                        positiveLabel++;
                    else
                        negativeLabel++;
                }
            }
            rule.setCorefSupport((double)(positiveLabel+negativeLabel)/(double)labeledRuleList.size());
            rule.setCorefConfidence((double)positiveLabel/(double)(positiveLabel+negativeLabel));
            rule.setLabel((double)positiveLabel/(double)(positiveLabel+negativeLabel)>=threshold);
            model.add(rule);
        }
    }
    
    public void loadRules(String path) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(path));
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] rules = line.split(";");
                labeledRuleList.add(new RuleInstance(rules));
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(AssociationRuleModel.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(AssociationRuleModel.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(AssociationRuleModel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public void removeLastInstanceModel() {
        model.remove(model.size()-1);
    }
    
    public double getModelConfidence() {
        return model.get(model.size()-1).getCorefConfidence();
    }
    
    public void loadModel(String path) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(path));
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] rules = line.split(";");
                model.add(new RuleInstance(rules));
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(AssociationRuleModel.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(AssociationRuleModel.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(AssociationRuleModel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public void writeRules(String path) {
        try (PrintWriter writer = new PrintWriter(path, "UTF-8")) {
            writer.println("mentions;isStrMatch;"
                    + "isMatchNoCasePunc;isAbbrev;isFirstPronoun;isScndPronoun;"
                    + "isOnOneSentence;isMatchPartial;firstNameClass;"
                    + "scndNameClass");
            for(RuleInstance rule:labeledRuleList) {
                writer.println("\"[("+rule.getMention1()+")-("+rule.getMention2()+")]\";"+rule);
            }
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            Logger.getLogger(Document.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void writeModel(String path) {
        DecimalFormat df = new DecimalFormat("0.0");
        df.setMaximumFractionDigits(6);
        try (PrintWriter writer = new PrintWriter(path, "UTF-8")) {
            writer.println("mentions;isStrMatch;isMatchNoCasePunc;isAbbrev;isFirstPronoun;"
                    + "isScndPronoun;isOnOneSentence;isMatchPartial;firstNameClass;"
                    + "scndNameClass;isCoref;corefSupport;corefConfidence");
            for(RuleInstance rule:model) {
                writer.println("\"[("+rule.getMention1()+")-("
                        +rule.getMention2()+")]\";"+rule+";"+rule.isCoref()+";"
                        +df.format(rule.getCorefSupport())+";"+df.format(rule.getCorefConfidence()));
            }
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            Logger.getLogger(Document.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
