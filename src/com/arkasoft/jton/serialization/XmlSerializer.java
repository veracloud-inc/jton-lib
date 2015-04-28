package com.arkasoft.jton.serialization;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import com.arkasoft.jton.JtonArray;
import com.arkasoft.jton.JtonElement;
import com.arkasoft.jton.JtonNull;
import com.arkasoft.jton.JtonObject;
import com.arkasoft.jton.JtonPrimitive;

public class XmlSerializer implements Serializer<JtonObject> {
	private Charset charset = null;
	private String localName = null;

	public static final String DEFAULT_LOCALNAME = "jton-object";

	public static final String XMLNS_ATTRIBUTE_PREFIX = "xmlns";

	public static final String DEFAULT_CHARSET_NAME = "UTF-8";
	public static final String XML_EXTENSION = "xml";
	public static final String MIME_TYPE = "text/xml";
	public static final int BUFFER_SIZE = 2048;

	public XmlSerializer() {
		this(Charset.forName(DEFAULT_CHARSET_NAME));
	}

	public XmlSerializer(Charset charset) {
		if (charset == null) {
			throw new IllegalArgumentException("charset is null.");
		}

		this.charset = charset;
	}

	public Charset getCharset() {
		return charset;
	}

	@Override
	public JtonObject readObject(InputStream inputStream)
			throws IOException, SerializationException {
		if (inputStream == null) {
			throw new IllegalArgumentException("inputStream is null.");
		}

		Reader reader = new BufferedReader(new InputStreamReader(inputStream, charset), BUFFER_SIZE);
		JtonObject element = readObject(reader);

		return element;
	}

	public JtonObject readObject(Reader reader) throws SerializationException {
		if (reader == null) {
			throw new IllegalArgumentException("reader is null.");
		}

		// Parse the XML stream
		XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
		xmlInputFactory.setProperty("javax.xml.stream.isCoalescing", true);

		Element document = null;

		try {
			XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(reader);

			Element current = null;

			while (xmlStreamReader.hasNext()) {
				int event = xmlStreamReader.next();

				switch (event) {
				case XMLStreamConstants.CHARACTERS: {
					if (!xmlStreamReader.isWhiteSpace()) {
						if (current != null) {
							current.setText(xmlStreamReader.getText());
						}
					}
					break;
				}
				case XMLStreamConstants.START_ELEMENT: {
					// Create the element
					String prefix = xmlStreamReader.getPrefix();
					if (prefix != null && prefix.length() == 0) {
						prefix = null;
					}

					String localName = xmlStreamReader.getLocalName();

					Element element = new Element(localName);

					// Get the element's attributes
					for (int i = 0, n = xmlStreamReader.getAttributeCount(); i < n; i++) {
						String attributePrefix = xmlStreamReader.getAttributePrefix(i);
						if (attributePrefix != null && attributePrefix.length() == 0) {
							attributePrefix = null;
						}

						String attributeLocalName = xmlStreamReader.getAttributeLocalName(i);

						if ("type".equalsIgnoreCase(attributeLocalName)) {
							String type = xmlStreamReader.getAttributeValue(i);
							element.setType(type);
							break;
						}
					}

					if (current == null) {
						document = element;
					} else {
						current.add(element);
					}

					current = element;

					break;
				}
				case XMLStreamConstants.END_ELEMENT: {

					// Move up the stack
					if (current != null) {
						current = current.getParent();
					}

					break;
				}
				default: {
					break;
				}
				}
			}
		} catch (XMLStreamException exception) {
			throw new SerializationException(exception);
		}

		return (JtonObject) document.toJton();
	}

	@Override
	public void writeObject(JtonObject object, OutputStream outputStream)
			throws IOException, SerializationException {
		if (outputStream == null) {
			throw new IllegalArgumentException("outputStream is null.");
		}

		Writer writer = new BufferedWriter(new OutputStreamWriter(outputStream, charset), BUFFER_SIZE);
		writeObject(object, writer);
		writer.flush();
	}

	public void writeObject(JtonObject object, Writer writer)
			throws IOException, SerializationException {
		if (writer == null) {
			throw new IllegalArgumentException("writer is null.");
		}

		XMLOutputFactory output = XMLOutputFactory.newInstance();
		try {
			XMLStreamWriter xmlStreamWriter = output.createXMLStreamWriter(writer);
			xmlStreamWriter.writeStartDocument();
			writeObject(this.localName != null ? this.localName : DEFAULT_LOCALNAME, object, xmlStreamWriter);
			xmlStreamWriter.writeEndDocument();
		} catch (XMLStreamException exception) {
			throw new SerializationException(exception);
		}
	}

	private void writeObject(String localName, JtonObject object, XMLStreamWriter xmlStreamWriter)
			throws XMLStreamException, SerializationException {

		if (object.size() == 0) {
			xmlStreamWriter.writeEmptyElement(localName);
		} else {
			xmlStreamWriter.writeStartElement(localName);
		}

		// Write out the child nodes
		for (Map.Entry<String, JtonElement> entry : object.entrySet()) {
			String key = entry.getKey();
			JtonElement element = entry.getValue();

			// ---
			// boolean identifier = true;
			// StringBuilder keyStringBuilder = new StringBuilder();
			//
			// for (int j = 0, n = key.length(); j < n; j++) {
			// char cj = key.charAt(j);
			// identifier &= Character.isJavaIdentifierPart(cj);
			//
			// if (cj == '"') {
			// keyStringBuilder.append('\\');
			// }
			//
			// keyStringBuilder.append(cj);
			// }
			//
			// key = keyStringBuilder.toString();
			// ---

			if (element.isJtonNull()) {
				xmlStreamWriter.writeEmptyElement(key);
				xmlStreamWriter.writeAttribute("type", "null");
			} else {
				if (element.isJtonPrimitive()) {
					xmlStreamWriter.writeStartElement(key);
					String type;

					Object value = element.getValue();
					if (value instanceof Boolean) {
						type = "bool";
					} else if (value instanceof Integer) {
						type = "int";
					} else if (value instanceof BigInteger) {
						type = "bigint";
					} else if (value instanceof java.sql.Date) {
						type = "sqldate";
					} else if (value instanceof java.sql.Time) {
						type = "sqltime";
					} else if (value instanceof java.sql.Timestamp) {
						type = "sqltstamp";
					} else {
						type = value.getClass().getSimpleName().toLowerCase();
					}

					xmlStreamWriter.writeAttribute("type", type);
					writeTextNode(element.getAsJtonPrimitive(), xmlStreamWriter);

				} else if (element.isJtonArray()) {
					writeArray(key, element.getAsJtonArray(), xmlStreamWriter);
				} else {
					writeObject(key, element.getAsJtonObject(), xmlStreamWriter);
				}

				xmlStreamWriter.writeEndElement();
			}
		}

	}

	private void writeArray(String key, JtonArray object, XMLStreamWriter xmlStreamWriter)
			throws XMLStreamException, SerializationException {
		if (object.size() == 0) {
			xmlStreamWriter.writeEmptyElement(key);
		}

		// Write out the child nodes
		for (JtonElement element : object) {
			if (element.isJtonNull()) {
				xmlStreamWriter.writeEmptyElement(key);
				xmlStreamWriter.writeAttribute("type", "null");
			} else {
				if (element.isJtonPrimitive()) {
					xmlStreamWriter.writeStartElement(key);
					String type;

					Object value = element.getValue();
					if (value instanceof Boolean) {
						type = "bool";
					} else if (value instanceof Integer) {
						type = "int";
					} else if (value instanceof BigInteger) {
						type = "bigint";
					} else if (value instanceof java.sql.Date) {
						type = "sqldate";
					} else if (value instanceof java.sql.Time) {
						type = "sqltime";
					} else if (value instanceof java.sql.Timestamp) {
						type = "sqltstamp";
					} else {
						type = value.getClass().getSimpleName().toLowerCase();
					}

					xmlStreamWriter.writeAttribute("type", type);
					writeTextNode(element.getAsJtonPrimitive(), xmlStreamWriter);

				} else if (element.isJtonArray()) {
					writeArray(key, element.getAsJtonArray(), xmlStreamWriter);
				} else {
					writeObject(key, element.getAsJtonObject(), xmlStreamWriter);
				}

				xmlStreamWriter.writeEndElement();
			}
		}
	}

	private void writeTextNode(JtonPrimitive object, XMLStreamWriter xmlStreamWriter)
			throws XMLStreamException, SerializationException {
		if (object.isString()) {
			String string = object.getAsString();
			StringBuilder stringBuilder = new StringBuilder();

			for (int i = 0, n = string.length(); i < n; i++) {
				char ci = string.charAt(i);

				switch (ci) {
				case '\t': {
					stringBuilder.append("\\t");
					break;
				}

				case '\n': {
					stringBuilder.append("\\n");
					break;
				}

				case '\\':
				case '\"':
				case '\'': {
					stringBuilder.append("\\" + ci);
					break;
				}

				default: {
					if (charset.name().startsWith("UTF")
							|| ci <= 0xFF) {
						stringBuilder.append(ci);
					} else {
						stringBuilder.append("\\u");
						stringBuilder.append(String.format("%04x", (short) ci));
					}
				}
				}

			}
			xmlStreamWriter.writeCharacters(stringBuilder.toString());
		} else if (object.isNumber()) {
			Number number = object.getAsNumber();

			if (number instanceof Float) {
				Float f = (Float) number;
				if (f.isNaN()
						|| f.isInfinite()) {
					throw new SerializationException(number + " is not a valid value.");
				}
			} else if (number instanceof Double) {
				Double d = (Double) number;
				if (d.isNaN()
						|| d.isInfinite()) {
					throw new SerializationException(number + " is not a valid value.");
				}
			}

			xmlStreamWriter.writeCharacters(number.toString());
		} else if (object.isBoolean()) {
			xmlStreamWriter.writeCharacters(String.valueOf(object.getAsBoolean()));
		} else if (object.isDate()) {
			xmlStreamWriter.writeCharacters(object.getAsString());
		} else if (object.isSqlDate()) {
			xmlStreamWriter.writeCharacters(object.getAsString());
		} else if (object.isSqlTime()) {
			xmlStreamWriter.writeCharacters(object.getAsString());
		} else if (object.isSqlTimestamp()) {
			xmlStreamWriter.writeCharacters(object.getAsString());
		}
	}

	@Override
	public String getMIMEType(JtonObject object) {
		return MIME_TYPE + "; charset=" + charset.name();
	}

	private class Element extends ArrayList<Element> {
		private Element parent = null;
		private String type = null;
		private String text = null;

		private final String name;

		private final Map<String, Boolean> members = new HashMap<String, Boolean>();

		public Element(String name) {
			this.name = name;
		}

		String getName() {
			return name;
		}

		Element getParent() {
			return parent;
		}

		void setParent(Element parent) {
			this.parent = parent;
		}

		String getType() {
			return type;
		}

		void setType(String type) {
			this.type = type;
		}

		String getText() {
			return text;
		}

		void setText(String text) {
			this.text = text;
		}

		@Override
		public boolean add(Element element) {
			if (element.getParent() != null) {
				return false;
			}
			if (super.add(element)) {
				element.setParent(this);
				String prop = element.getName();
				if (members.containsKey(prop)) {
					if (!members.get(prop)) {
						members.put(prop, Boolean.TRUE);
					}
				} else {
					members.put(prop, Boolean.FALSE);
				}
				return true;
			}
			return false;
		}

		JtonElement toJton() throws SerializationException {
			if (type == null) { // handle NULL
				JtonObject me = new JtonObject();
				for (Element e : this) {
					if (members.get(e.name)) {
						JtonArray arr = me.get(e.name).getAsJtonArray(null);
						if (arr == null) {
							me.add(e.name, arr = new JtonArray());
						}
						arr.add(e.toJton());
					} else {
						me.add(e.name, e.toJton());
					}
				}
				return me;
			} else {
				try {
					if ("null".equals(type)) {
						return JtonNull.INSTANCE;
					} else if ("string".equals(type)) {
						return new JtonPrimitive(text);
					} else if ("char".equals(type)) {
						return new JtonPrimitive(Character.valueOf(text.charAt(0)));
					} else if ("byte".equals(type)) {
						return new JtonPrimitive(Byte.valueOf(text));
					} else if ("short".equals(type)) {
						return new JtonPrimitive(Short.valueOf(text));
					} else if ("int".equals(type)) {
						return new JtonPrimitive(Integer.valueOf(text));
					} else if ("long".equals(type)) {
						return new JtonPrimitive(Long.valueOf(text));
					} else if ("float".equals(type)) {
						return new JtonPrimitive(Float.valueOf(text));
					} else if ("double".equals(type)) {
						return new JtonPrimitive(Double.valueOf(text));
					} else if ("bigint".equals(type)) {
						return new JtonPrimitive(new BigInteger(text));
					} else if ("bigdecimal".equals(type)) {
						return new JtonPrimitive(new BigDecimal(text));
					} else if ("date".equals(type)) {
						return new JtonPrimitive(DatatypeConverter.parseDateTime(text).getTime());
					} else if ("sqldate".equals(type)) {
						return new JtonPrimitive(new java.sql.Date(DatatypeConverter.parseDate(text).getTime().getTime()));
					} else if ("sqltime".equals(type)) {
						return new JtonPrimitive(new java.sql.Time(DatatypeConverter.parseDate(text).getTime().getTime()));
					} else if ("sqltstamp".equals(type)) {
						return new JtonPrimitive(new java.sql.Timestamp(DatatypeConverter.parseDate(text).getTime().getTime()));
					} else {
						throw new SerializationException("Unknown type: " + type);
					}
				} catch (Exception e) {
					throw new SerializationException(e.getMessage(), e);
				}
			}
		}
	}

}
