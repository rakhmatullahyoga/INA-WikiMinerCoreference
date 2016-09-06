/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package helper;

/**
 *
 * @author Rakhmatullah Yoga S
 */
public class CoreferenceConstants {
    // coreference parameter constants
    public static final double TOPIC_THRESHOLD = 0.16;
    public static final double COREFERENCE_THRESHOLD = 0.85;
    
    // path constants
    public static final String RAW_TRAINING = "./data/corefdata/raw/training/";
    public static final String KEY_TRAINING = "./data/corefdata/key/training/";
    public static final String RAW_TESTING = "./data/corefdata/raw/testing/";
    public static final String KEY_TESTING = "./data/corefdata/key/testing/";
    
    public static final String WIKI_TRAINING = "./data/wikicoref/training/";
    public static final String WIKI_TESTING = "./data/wikicoref/testing/";
    
    public static final String BASELINE_PATH = "./data/baseline/";
    public static final String AR_RULES = BASELINE_PATH+"rules/";
    public static final String AR_LABELED = BASELINE_PATH+"labeled/";
    
    // path for demo
    public static final String PATH_DEMO = "./data/corefdata/demo/";
}
