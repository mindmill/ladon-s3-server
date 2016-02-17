/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package com.basho.riakcs.client.impl;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class XMLUtils
{
	public static String xpathToContent(String xpathQuery, Object domObject) throws XPathExpressionException
	{
		Node node= xpathToNode(xpathQuery, domObject);
		
		if (node != null)
			return node.getTextContent();
		
		return null;
	}

	public static Node xpathToNode(String xpathQuery, Object domObject) throws XPathExpressionException
	{
		XPathFactory xpathFactory = XPathFactory.newInstance();
		XPath xpath = xpathFactory.newXPath();
		
		return (Node) xpath.evaluate(xpathQuery, domObject, XPathConstants.NODE);
	}


	public static List<Node> xpathToNodeList(String xpathQuery, Object domObject) throws XPathExpressionException
    {
		List<Node> result= new ArrayList<Node>();
		
		XPathFactory xpathFactory= XPathFactory.newInstance();
		XPath xpath= xpathFactory.newXPath();
		NodeList nodeList= (NodeList) xpath.evaluate(xpathQuery, domObject, XPathConstants.NODESET);

		for (int i = 0; i < nodeList.getLength(); i++)
			result.add(nodeList.item(i));
	
		return result;
	}


	public static Document parseToDocument(InputStream is, boolean debugModeEnabled) throws Exception
	{
		DocumentBuilderFactory factory= DocumentBuilderFactory.newInstance();
		DocumentBuilder builder= factory.newDocumentBuilder();
		Document document= builder.parse(is);
		
		if (debugModeEnabled) System.out.println("Body:\n" + serializeDocument(document) + "\n");

		return document;
	}

	public static String serializeDocument(Document document) throws Exception
	{
		// Serialize XML document to String.
		StringWriter writer= new StringWriter();
		StreamResult streamResult = new StreamResult(writer);

		DOMSource domSource= new DOMSource(document);
		TransformerFactory tf= TransformerFactory.newInstance();
		Transformer serializer= tf.newTransformer();
		serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		serializer.setOutputProperty(OutputKeys.INDENT, "yes");
		serializer.transform(domSource, streamResult);
        
		return writer.toString();
	}


}
