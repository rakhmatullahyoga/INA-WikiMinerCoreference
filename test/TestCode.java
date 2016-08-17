
import arcoref.CorefRuleConstants;
import arcoref.Document;

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
    public static void main(String[] args) {
        Document doc = new Document(CorefRuleConstants.RAW_PATH);
        doc.extractMentions();
        doc.writeRules(CorefRuleConstants.RULES_CSV);
    }
}
