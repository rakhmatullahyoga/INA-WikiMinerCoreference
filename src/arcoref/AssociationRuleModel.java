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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
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
    
    public ArrayList<ArrayList<String>> classifyCoreference(ArrayList<RuleInstance> ruleList) {
        ArrayList<ArrayList<String>> responseChain = new ArrayList<>();
        for(RuleInstance rule:ruleList) {
            String m1 = rule.getMention1();
            String m2 = rule.getMention2();
            if(getClusterIdx(responseChain,m1)==-1) { // m1 belum masuk cluster
                ArrayList<String> newCluster = new ArrayList<>();
                newCluster.add(m1);
                responseChain.add(newCluster);
            }
            if(model.indexOf(rule)!=-1) {
                if(model.get(model.indexOf(rule)).isCoref()) { // corefer berdasarkan model
                    if(!responseChain.get(getClusterIdx(responseChain, m1)).contains(m2))
                        responseChain.get(getClusterIdx(responseChain, m1)).add(m2);
                }
            }
        }
        return responseChain;
    }

    private int getClusterIdx(ArrayList<ArrayList<String>> responseChain, String mention) {
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
            rule.setLabel(positiveLabel>negativeLabel);
            model.add(rule);
        }
    }
    
    public void loadRules(String path) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(path));
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] rules = line.split(",");
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
    
    public void loadModel(String path) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(path));
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] rules = line.split(",");
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
            writer.println("mentions,isStrMatch,"
                    + "isMatchNoCasePunc,isAbbrev,isFirstPronoun,isScndPronoun,"
                    + "isOnOneSentence,isMatchPartial,firstNameClass,"
                    + "scndNameClass");
            for(RuleInstance rule:labeledRuleList) {
                writer.println("\"[("+rule.getMention1()+")-("+rule.getMention2()+")]\","+rule);
            }
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            Logger.getLogger(Document.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void writeModel(String path) {
        try (PrintWriter writer = new PrintWriter(path, "UTF-8")) {
            writer.println("isStrMatch,isMatchNoCasePunc,isAbbrev,isFirstPronoun,"
                    + "isScndPronoun,isOnOneSentence,isMatchPartial,firstNameClass,"
                    + "scndNameClass,isCoref");
            for(RuleInstance rule:model) {
                writer.println(rule+","+rule.isCoref());
            }
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            Logger.getLogger(Document.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
