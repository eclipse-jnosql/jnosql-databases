/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Otavio Santana
 */

package org.eclipse.jnosql.databases.redis.communication;

import jakarta.json.bind.Jsonb;
import org.eclipse.jnosql.communication.driver.JsonbSupplier;
import redis.clients.jedis.UnifiedJedis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

abstract class RedisCollection<T> implements Collection<T> {

    protected static final  Jsonb JSONB = JsonbSupplier.getInstance().get();

    protected final Class<T> clazz;

    protected final String keyWithNameSpace;

    protected final UnifiedJedis jedis;

    protected final boolean isString;

    RedisCollection(UnifiedJedis jedis, Class<T> clazz, String keyWithNameSpace) {
        this.clazz = clazz;
        this.keyWithNameSpace = keyWithNameSpace;
        this.jedis = jedis;
        this.isString = String.class.equals(clazz);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        Objects.requireNonNull(c);
        for (T bean : c) {
            if (bean != null) {
                add(bean);
            }
        }
        return true;
    }

    @Override
    public int size() {
        return (int) jedis.llen(keyWithNameSpace);
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) != -1;
    }

    @Override
    public Iterator<T> iterator() {
        return toArrayList().iterator();
    }

    @Override
    public Object[] toArray() {
        return toArrayList().toArray();
    }

    @Override
    public <E> E[] toArray(E[] a) {
        return toArrayList().toArray(a);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("Use add all instead");
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean containsAll(Collection<?> elements) {
        Objects.requireNonNull(elements);
        boolean containsAll = true;
        for (T element : (Collection<T>) elements) {
            if (!contains(element)) {
                containsAll = false;
                break;
            }
        }
        return containsAll;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean removeAll(Collection<?> elements) {
        Objects.requireNonNull(elements);
        boolean result = false;
        for (T element : (Collection<T>) elements) {
            if (remove(element)) {
                result = true;
            }
        }
        return result;
    }

    @Override
    public boolean remove(Object o) {
        if (!clazz.isInstance(o)) {
            throw new ClassCastException("The object required is " + clazz.getName());
        }
        int index = indexOf(o);

        if (index == -1) {
            return false;
        } else {
            remove(index);
        }

        return true;
    }

    protected T remove(int index) {
        String value = jedis.lindex(keyWithNameSpace, (long) index);
        if (value != null && !value.isEmpty()) {
            jedis.lrem(keyWithNameSpace, 1, value);
            return serialize(value);
        }
        return null;
    }


    protected int indexOf(Object o) {
        if (!clazz.isInstance(o)) {
            return -1;
        }

        String value = serialize(o);
        for (int index = 0; index < size(); index++) {
            String findedValue = jedis.lindex(keyWithNameSpace, (long) index);
            if (value.equals(findedValue)) {
                return index;
            }
        }
        return -1;
    }


    protected List<T> toArrayList() {
        List<T> list = new ArrayList<>();
        for (int index = 0; index < size(); index++) {
            T element = get(index);
            if (element != null) {
                list.add(element);
            }
        }
        return list;
    }

    protected T get(int index) {
        String value = jedis.lindex(keyWithNameSpace, index);
        if (value == null || value.isEmpty()) {
            return null;
        }
        return serialize(value);
    }


    protected T serialize(String value) {
        if(isString) {
            return (T) value;
        }
        return JSONB.fromJson(value,clazz);
    }

    protected String serialize(Object value) {
        if(value instanceof String) {
            return value.toString();
        }
        return JSONB.toJson(value);
    }


    @Override
    public int hashCode() {
        return Objects.hashCode(keyWithNameSpace);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (RedisCollection.class.isInstance(obj)) {
            RedisCollection otherRedis = RedisCollection.class.cast(obj);
            return Objects.equals(otherRedis.keyWithNameSpace, keyWithNameSpace);
        }
        return false;
    }

}
