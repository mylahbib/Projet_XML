
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;



public class xmlWrite {
	
	public void convert(String pFile,List<MyShape> shapes, String[] comments, int[] currentTool) throws ParserConfigurationException, Exception {

		
		
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		// root elements
		Document doc = docBuilder.newDocument();
		Element rootElement = doc.createElement("shypes");
		doc.appendChild(rootElement);

		//  elements
		String[] type = new String[currentTool.length] ;

		 if(currentTool[1] == 1){
				
				type[1] = "square";
			}
		 if(currentTool[2] == 2){
				
				type[2] = "rectangle";
			}
		 if(currentTool[3] == 3 ){
				type[3] = "circle";
			}
		 if(currentTool[4] == 4 || currentTool[5] == 5){
				type[4] = "Triangle";
				type[5] = "Triangle";
			}else
			{
				type[0]="polygone"; 
			}
		 
		 int i=0;
			int	g=0;
		for (MyShape sh : shapes) {
			 
			while (currentTool[i]==0 ) {
				i++;
			}
			
			
			Element shape = doc.createElement(type[i]);
			Element color = doc.createElement("color");
			color.appendChild(doc.createTextNode(sh.getColor().toString()));
			 shape.appendChild(color);
				rootElement.appendChild(shape);
				int j= 0;
				for (Point2D  point: sh.points) {
					Element x = doc.createElement("x"+j);
					Element y = doc.createElement("y"+j);
					
						j++;
					y.appendChild(doc.createTextNode(String.valueOf(point.y())));
					x.appendChild(doc.createTextNode(String.valueOf(point.x())));
				
					shape.appendChild(x);
					shape.appendChild(y);
				}
				Element comment = doc.createElement("comment");
				System.out.println(String.valueOf(comments[g]) + ":"+g);
				comment.appendChild(doc.createTextNode(String.valueOf(comments[g])));
				
				shape.appendChild(comment);
				i++;
				g++;
			}
				
				
				
			
		
		
	
		
		// write file xml
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(new File(pFile));
		
		transformer.transform(source, result);
		transformer.setOutputProperty(OutputKeys.VERSION, "1.0");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");


		System.out.println("File saved!");


			
			
		
		
		
	}

}
