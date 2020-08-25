package com.buldreinfo.jersey.jaxb.pdf;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.JPEGTranscoder;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.model.Svg;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

public class TopoGenerator {
	public static Path generateTopo(int mediaId, int width, int height, List<Svg> svgs) throws FileNotFoundException, IOException, TranscoderException, TransformerException {
		Path dst = Files.createTempFile("topo", "jpg");
		try (Reader reader = new StringReader(generateDocument(mediaId, width, height, svgs))) {
			TranscoderInput ti = new TranscoderInput(reader);
			try (OutputStream os = new FileOutputStream(dst.toString())) {
				TranscoderOutput to = new TranscoderOutput(os);
				JPEGTranscoder t = new JPEGTranscoder();
				t.addTranscodingHint(JPEGTranscoder.KEY_QUALITY, new Float(1));
				t.addTranscodingHint(JPEGTranscoder.KEY_WIDTH, new Float(width));
				t.addTranscodingHint(JPEGTranscoder.KEY_HEIGHT, new Float(height));
				t.addTranscodingHint(JPEGTranscoder.KEY_ALLOWED_SCRIPT_TYPES, "*");
				t.addTranscodingHint(JPEGTranscoder.KEY_CONSTRAIN_SCRIPT_ORIGIN, new Boolean(true));
				t.addTranscodingHint(JPEGTranscoder.KEY_EXECUTE_ONLOAD, new Boolean(true));
				t.transcode(ti, to);
			}
		}
		return dst;
	}

	private static String generateDocument(int mediaId, int width, int height, List<Svg> svgs) throws TransformerException {
		DOMImplementation impl = SVGDOMImplementation.getDOMImplementation();
		String svgNS = SVGDOMImplementation.SVG_NAMESPACE_URI;
		Document doc = impl.createDocument(svgNS, "svg", null);
		Element svgRoot = doc.getDocumentElement();
		svgRoot.setAttributeNS(null, "viewBox", "0 0 " + width + " " + height);
		svgRoot.setAttributeNS(null, "preserveAspectRatio", "xMidYMid meet");

		// Image
		Element image = doc.createElementNS(null, "image");
		String url = GlobalFunctions.getUrlJpgToImage(mediaId);
		image.setAttributeNS(null, "xlink:href", url);
		image.setAttributeNS(null, "href", url);
		image.setAttributeNS(null, "height", "100%");
		image.setAttributeNS(null, "width", "100%");
		svgRoot.appendChild(image);

		List<Element> texts = Lists.newArrayList(); // Text always on top
		for (Svg svg : svgs) {
			final int imgMax = Math.max(width, height);
			List<String> parts = Lists.newArrayList(Splitter.on("L").omitEmptyStrings().trimResults().split(svg.getPath().replace("M", "L").replace("C", "L")));
			float x0 = Float.parseFloat(parts.get(0).split(" ")[0]);
			float y0 = Float.parseFloat(parts.get(0).split(" ")[1]);
			String[] lastParts = parts.get(parts.size()-1).split(" ");
			float x1 = Float.parseFloat(lastParts[lastParts.length-2]);
			float y1 = Float.parseFloat(lastParts[lastParts.length-1]);
			boolean firstIsLowest = y0 > y1;
			float xMin = firstIsLowest? x0 : x1;
			float yMin = firstIsLowest? y0 : y1;
			float xMax = firstIsLowest? x1 : x0;
			float yMax = firstIsLowest? y1 : y0;
			// Path
			Element path = doc.createElementNS(null, "path");
			path.setAttributeNS(null, "style", "fill: none; stroke: #E2011A;");
			path.setAttributeNS(null, "d", svg.getPath());
			path.setAttributeNS(null, "stroke-width", String.valueOf(0.003 * imgMax)); 
			path.setAttributeNS(null, "stroke-dasharray", String.valueOf(0.006 * imgMax));
			svgRoot.appendChild(path);
			// Anchor-circle
			if (svg.isHasAnchor()) {
				Element circle = doc.createElementNS(null, "circle");
				circle.setAttributeNS(null, "style", "fill: #E2011A;");
				circle.setAttributeNS(null, "cx", String.valueOf(xMax));
				circle.setAttributeNS(null, "cy", String.valueOf(yMax)); 
				circle.setAttributeNS(null, "r", String.valueOf(0.008 * imgMax));
				svgRoot.appendChild(circle);
			}
			// Nr
			Element circle = doc.createElementNS(null, "circle");
			circle.setAttributeNS(null, "style", "fill: #E2011A;");
			circle.setAttributeNS(null, "cx", String.valueOf(xMin));
			circle.setAttributeNS(null, "cy", String.valueOf(yMin)); 
			circle.setAttributeNS(null, "r", String.valueOf(0.008 * imgMax));
			svgRoot.appendChild(circle);
			Element text = doc.createElementNS(null, "text");
			text.setAttributeNS(null, "style", "fill: #FFFFFF; text-anchor: middle; line-height: 1;");
			text.setAttributeNS(null, "x", String.valueOf(xMin));
			text.setAttributeNS(null, "y", String.valueOf(yMin));
			text.setAttributeNS(null, "dy", ".3em");
			text.setAttributeNS(null, "font-size", String.valueOf(0.013 * imgMax));
			text.appendChild(doc.createTextNode(String.valueOf(svg.getNr())));
			texts.add(text);
		}
		for (Element text : texts) {
			svgRoot.appendChild(text);
		}

		DOMSource domSource = new DOMSource(doc);
		Writer writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.transform(domSource, result);
		String res = writer.toString();
		return res;
	}
}