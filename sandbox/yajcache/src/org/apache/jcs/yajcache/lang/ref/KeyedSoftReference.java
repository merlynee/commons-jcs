
/*
 * Copyright 2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jcs.yajcache.lang.ref;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

import org.apache.jcs.yajcache.lang.annotation.*;

/**
 * Soft reference with an embedded key.
 *
 * @author Hanson Char
 */
@CopyRightApache
public class KeyedSoftReference<K,T> extends SoftReference<T> 
        implements IKey<K> 
{
    private final @NonNullable K key;
    
    public KeyedSoftReference(@NonNullable K key, T referent) 
    {
	super(referent);
        this.key = key;
    }
    public KeyedSoftReference(@NonNullable K key, T referrent, 
            ReferenceQueue<? super T> q) 
    {
        super(referrent, q);
        this.key = key;
    }
    @Implements(IKey.class)
    public @NonNullable K getKey() {
        return this.key;
    }
}
