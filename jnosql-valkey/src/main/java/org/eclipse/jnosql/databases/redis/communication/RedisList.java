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

import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.args.ListPosition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

class RedisList<T> extends RedisCollection<T> implements List<T> {


    RedisList(UnifiedJedis jedis, Class<T> clazz, String keyWithNameSpace) {
        super(jedis, clazz, keyWithNameSpace);
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
    public ListIterator<T> listIterator() {
        return toArrayList().listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return toArrayList().listIterator(index);
    }

    @Override
    public Iterator<T> iterator() {
        return toArrayList().iterator();
    }

    @Override
    public boolean add(T e) {
        Objects.requireNonNull(e);
        int index = size();
        if (index == 0) {
            if(isString) {
                jedis.lpush(keyWithNameSpace, e.toString());
            } else {
                jedis.lpush(keyWithNameSpace, JSONB.toJson(e));
            }
        } else {
            String previewValue = jedis.lindex(keyWithNameSpace, index - 1);
            if(isString) {
                jedis.linsert(keyWithNameSpace, ListPosition.AFTER, previewValue, e.toString());
            }else {
                jedis.linsert(keyWithNameSpace, ListPosition.AFTER, previewValue,
                        JSONB.toJson(e));
            }
        }
        return true;
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> elements) {
        Objects.requireNonNull(elements);
        for (T element : elements) {
            add(index++, element);
        }
        return true;
    }

    @Override
    public void clear() {
        jedis.del(keyWithNameSpace);
    }

    @Override
    public T get(int index) {
        return super.get(index);
    }

    @Override
    public T set(int index, T element) {
        Objects.requireNonNull(element);
        if(isString) {
            jedis.lset(keyWithNameSpace, index, element.toString());
        } else {
            jedis.lset(keyWithNameSpace, index, JSONB.toJson(element));
        }

        return element;
    }

    @Override
    public void add(int index, T element) {
        Objects.requireNonNull(element);
        String previewValue = jedis.lindex(keyWithNameSpace, index);
        if (previewValue != null && !previewValue.isEmpty()) {
            if(isString) {
                jedis.linsert(keyWithNameSpace, ListPosition.BEFORE, previewValue, element.toString());
            } else {
                jedis.linsert(keyWithNameSpace, ListPosition.BEFORE, previewValue, JSONB.toJson(element));
            }

        } else {
            add(element);
        }

    }

    @Override
    public T remove(int index) {
        return super.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return super.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        Objects.requireNonNull(o);

        String value = serialize(o);
        for (int index = size(); index > 0; --index) {
            String findedValue = jedis.lindex(keyWithNameSpace, (long) index);
            if (value.equals(findedValue)) {
                return index;
            }
        }
        return -1;
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        List<T> subList = new ArrayList<>();
        List<String> elements = jedis.lrange(keyWithNameSpace, fromIndex, toIndex);
        for (String element : elements) {
            if(isString) {
                subList.add((T) element);
            } else {
                subList.add(JSONB.fromJson(element, clazz));
            }
        }
        return subList;
    }


}
