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

/**
 * Defines the expected format for a {@code long} or {@code Long} type when its serialized.
 *
 * @since 1.3
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 */
public enum LongSerializationPolicy {
  /**
   * This is the "default" serialization policy that will output a {@code long} object as a JSON
   * number.  For example, assume an object has a long field named "f" then the serialized output
   * would be:
   * {@code {"f":123}}.
   */
  DEFAULT() {
    public JtonElement serialize(Long value) {
      return new JtonPrimitive(value);
    }
  },
  
  /**
   * Serializes a long value as a quoted string.  For example, assume an object has a long field 
   * named "f" then the serialized output would be:
   * {@code {"f":"123"}}.
   */
  STRING() {
    public JtonElement serialize(Long value) {
      return new JtonPrimitive(String.valueOf(value));
    }
  };
  
  /**
   * Serialize this {@code value} using this serialization policy.
   *
   * @param value the long value to be serialized into a {@link JtonElement}
   * @return the serialized version of {@code value}
   */
  public abstract JtonElement serialize(Long value);
}