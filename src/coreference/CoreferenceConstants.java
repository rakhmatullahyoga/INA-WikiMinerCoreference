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
    public static final double TOPIC_THRESHOLD = 0.155;
    public static final double COREFERENCE_THRESHOLD = 0.8;
    
    // path constants
    public static final String RAW_PATH = "./data/testing/raw/";
    public static final String ANNOTATED_PATH = "./data/testing/corefannotated/";
    public static final String CHAIN_PATH = "./data/testing/corefchain/";
    // path for development
    public static final String RAW_PATH_DEV = "./data/development/raw/";
    public static final String ANNOTATED_PATH_DEV = "./data/development/corefannotated/";
    public static final String CHAIN_PATH_DEV = "./data/development/corefchain/";
}
