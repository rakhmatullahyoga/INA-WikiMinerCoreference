/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package coreference;

/**
 *
 * @author Rakhmatullah Yoga S
 */
public class CoreferenceConstants {
    // coreference parameter constants
    public static final double TOPIC_THRESHOLD = 0.25;
    public static final double COREFERENCE_THRESHOLD = 0.65;
    
    // path constants
    public static final String TRAIN_RAW = "./data/train/raw/";
    public static final String TRAIN_ANNOTATED = "./data/train/corefannotated/";
    public static final String TRAIN_RESPONSE = "./data/train/responsechain/";
    public static final String TRAIN_KEY = "./data/train/key/";
    public static final String TRAIN_PATH = "./data/train/";
    
    public static final String TOPICS_PATH = "./data/train/topics/";
    public static final String TEST_RAW = "./data/testing/raw/";
    public static final String TEST_ANNOTATED = "./data/testing/corefannotated/";
    public static final String TEST_RESPONSE = "./data/testing/responsechain/";
    // path for development
    public static final String PATH_DEMO = "./data/demo/";
}
