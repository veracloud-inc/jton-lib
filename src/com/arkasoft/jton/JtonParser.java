/*
 * Copyright (C) 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.arkasoft.jton;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import com.arkasoft.jton.internal.Streams;
import com.arkasoft.jton.stream.JsonReader;
import com.arkasoft.jton.stream.JsonToken;
import com.arkasoft.jton.stream.MalformedJsonException;

/**
 * A parser to parse Json into a parse tree of {@link JtonElement}s
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 * @since 1.3
 */
public final class JtonParser {

	/**
	 * Parses the specified JSON string into a parse tree
	 *
	 * @param json
	 *          JSON text
	 * @return a parse tree of {@link JtonElement}s corresponding to the specified
	 *         JSON
	 * @throws JtonParseException
	 *           if the specified text is not valid JSON
	 * @since 1.3
	 */
	public static JtonElement parse(String json) throws JtonSyntaxException {
		return parse(new StringReader(json));
	}

	/**
	 * Parses the specified JSON string into a parse tree
	 *
	 * @param json
	 *          JSON text
	 * @return a parse tree of {@link JtonElement}s corresponding to the specified
	 *         JSON
	 * @throws JtonParseException
	 *           if the specified text is not valid JSON
	 * @since 1.3
	 */
	public static JtonElement parse(Reader json) throws JtonIOException, JtonSyntaxException {
		try {
			JsonReader jsonReader = new JsonReader(json);
			JtonElement element = parse(jsonReader);
			if (!element.isJtonNull() && jsonReader.peek() != JsonToken.END_DOCUMENT) {
				throw new JtonSyntaxException("Did not consume the entire document.");
			}
			return element;
		} catch (MalformedJsonException e) {
			throw new JtonSyntaxException(e);
		} catch (IOException e) {
			throw new JtonIOException(e);
		} catch (NumberFormatException e) {
			throw new JtonSyntaxException(e);
		}
	}

	/**
	 * Returns the next value from the JSON stream as a parse tree.
	 *
	 * @throws JtonParseException
	 *           if there is an IOException or if the specified text is not valid
	 *           JSON
	 * @since 1.6
	 */
	public static JtonElement parse(JsonReader json) throws JtonIOException, JtonSyntaxException {
		boolean lenient = json.isLenient();
		json.setLenient(true);
		try {
			return Streams.parse(json);
		} catch (StackOverflowError e) {
			throw new JtonParseException("Failed parsing JSON source: " + json + " to Json", e);
		} catch (OutOfMemoryError e) {
			throw new JtonParseException("Failed parsing JSON source: " + json + " to Json", e);
		} finally {
			json.setLenient(lenient);
		}
	}

	private JtonParser() {
	}
}