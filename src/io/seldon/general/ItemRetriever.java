/*
 * Seldon -- open source prediction engine
 * =======================================
 *
 * Copyright 2011-2015 Seldon Technologies Ltd and Rummble Ltd (http://www.seldon.io/)
 *
 * ********************************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ********************************************************************************************
 */

package io.seldon.general;

import io.seldon.api.resource.service.PersistenceProvider;
import io.seldon.general.jdo.SqlItemPeer;
import io.seldon.memcache.DogpileHandler;
import io.seldon.memcache.MemCacheKeys;
import io.seldon.memcache.UpdateRetriever;
import net.spy.memcached.MemcachedClient;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author firemanphil
 *         Date: 18/11/14
 *         Time: 11:49
 */
@Component
public class ItemRetriever {

    private static Logger logger = Logger.getLogger( ItemRetriever.class.getName() );

    // 5 mins
    private static final int MOST_POPULAR_EXPIRE_TIME = 5 * 60;
    // 15 mins
    private static final int RECENT_ITEMS_EXPIRE_TIME = 15 * 60;

    private final MemcachedClient memcache;
    private final PersistenceProvider provider;
    private final DogpileHandler dogpileHandler;

    @Autowired
    public ItemRetriever(PersistenceProvider provider, MemcachedClient memcache, DogpileHandler dogpileHandler) {
        this.memcache = memcache;
        this.provider = provider;
        this.dogpileHandler = dogpileHandler;
    }

    public List<SqlItemPeer.ItemAndScore> retrieveMostPopularItems(final String client, final int numItems, final int dimension){

        String key = MemCacheKeys.getPopularItems(client, dimension, numItems);
        List<SqlItemPeer.ItemAndScore> retrievedItems = retrieve(key, numItems, new UpdateRetriever<List<SqlItemPeer.ItemAndScore>>() {
            @Override
            public List<SqlItemPeer.ItemAndScore> retrieve() throws Exception {
                return provider.getItemPersister(client).retrieveMostPopularItems(numItems, dimension);
            }
        }, MOST_POPULAR_EXPIRE_TIME);

        return retrievedItems==null? Collections.EMPTY_LIST: retrievedItems;
    }

    public Collection<Long> retrieveRecentlyAddedItems(final String client, final int numItems, final int dimension){
        String key = MemCacheKeys.getRecentItems(client, dimension, numItems);
        Collection<Long> retrievedItems = retrieve(key, numItems, new UpdateRetriever<Collection<Long>>() {
            @Override
            public Collection<Long> retrieve() throws Exception {
                return provider.getItemPersister(client).getRecentItemIds(dimension,numItems,null);
            }
        }, RECENT_ITEMS_EXPIRE_TIME);
        return retrievedItems==null? Collections.EMPTY_LIST : retrievedItems;
    }

    private <T extends Collection> T retrieve(String key, int numItemsRequired, UpdateRetriever<T> retriever, int expireTime) {
        T retrievedItems = (T) memcache.get(key);

        if (retrievedItems==null || retrievedItems.size() < numItemsRequired) retrievedItems = null;
        T newerRetrievedItems = null;
        try {
            newerRetrievedItems = DogpileHandler.get().retrieveUpdateIfRequired(key, retrievedItems, retriever, expireTime);
            if(newerRetrievedItems!=null){
                memcache.set(key, expireTime, newerRetrievedItems);
                return newerRetrievedItems;
            }
        } catch (Exception e) {
            logger.warn("Couldn't retrieve items when using dogpile handler" , e);
        }
        return retrievedItems;
    }

}
