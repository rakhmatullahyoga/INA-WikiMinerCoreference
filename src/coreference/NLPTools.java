/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package coreference;

import IndonesianNLP.IndonesianPhraseChunker;
import IndonesianNLP.TreeNode;
import java.util.ArrayList;

/**
 *
 * @author Rakhmatullah Yoga S
 */
public class NLPTools {
    public static boolean isPhrase(String term) {
        IndonesianPhraseChunker chunker = new IndonesianPhraseChunker(term);
        chunker.extractPhrase();
        ArrayList<TreeNode> phraseTree = chunker.getPhraseTree();
        return !phraseTree.isEmpty();
    }
    
    public static String getPhraseType(String term) {
        ArrayList<String> phraseType = new ArrayList<>();
        IndonesianPhraseChunker chunker = new IndonesianPhraseChunker(term);
        chunker.extractPhrase();
        ArrayList<TreeNode> phraseTree = chunker.getPhraseTree();
        phraseTree.stream().forEach((node) -> {
            phraseType.add(node.getType());
        });
        return phraseType.toString();
    }
}
