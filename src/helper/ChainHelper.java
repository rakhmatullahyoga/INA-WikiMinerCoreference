/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package helper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import wikicoref.WikiCorefMain;

/**
 *
 * @author rakhmatullahyoga
 */
public class ChainHelper {
    public static ArrayList<Set<String>> readKeyChain(String path) {
        ArrayList<Set<String>> keyChain = null;
        try {
            File inputFile = new File(path);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document xmlDoc = dBuilder.parse(inputFile);
            xmlDoc.getDocumentElement().normalize();
            NodeList nList = xmlDoc.getElementsByTagName("Entity");
            keyChain = new ArrayList<>();
            for(int i=0; i<nList.getLength(); i++) {
                Node nNode = nList.item(i);
                keyChain.add(new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    NodeList childList = eElement.getElementsByTagName("Mention");
                    for(int j=0; j<childList.getLength(); j++) {
                        String mention = childList.item(j).getTextContent();
                        keyChain.get(i).add(mention);
                    }
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            Logger.getLogger(WikiCorefMain.class.getName()).log(Level.SEVERE, null, ex);
        }
        return keyChain;
    }
    
    public static void writeCorefChain(ArrayList<Set<String>> responseChain, String path) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document xmlDoc = dBuilder.newDocument();
            
            // root element
            Element rootElement = xmlDoc.createElement("CoreferenceChain");
            xmlDoc.appendChild(rootElement);
            // entity element
            for(int i=0; i<responseChain.size(); i++) {
                Element entity = xmlDoc.createElement("Entity");
                rootElement.appendChild(entity);
                // setting attribute to element
                Attr attr = xmlDoc.createAttribute("ClusterId");
                attr.setValue(""+(i+1));
                entity.setAttributeNode(attr);
                for(String mention : responseChain.get(i)) {
                    // mention element
                    Element carname = xmlDoc.createElement("Mention");
                    carname.appendChild(xmlDoc.createTextNode(mention));
                    entity.appendChild(carname);
                }
            }
            
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            
            DOMSource source = new DOMSource(xmlDoc);
            StreamResult result = new StreamResult(new File(path));
            transformer.transform(source, result);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(WikiCorefMain.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformerConfigurationException ex) {
            Logger.getLogger(WikiCorefMain.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformerException ex) {
            Logger.getLogger(WikiCorefMain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
