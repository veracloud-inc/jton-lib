/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.arkasoft.jton.serialization.util;

import java.util.Iterator;

/**
 * Immutable implementation of the {@link Iterator} interface.
 */
public class ImmutableIterator<T> implements Iterator<T> {
	private Iterator<T> iterator;

	public ImmutableIterator(Iterator<T> iterator) {
		if (iterator == null) {
			throw new IllegalArgumentException("iterator is null.");
		}

		this.iterator = iterator;
	}

	@Override
	public boolean hasNext() {
		return this.iterator.hasNext();
	}

	@Override
	public T next() {
		return this.iterator.next();
	}

	@Override
	public final void remove() {
		throw new UnsupportedOperationException();
	}
}
