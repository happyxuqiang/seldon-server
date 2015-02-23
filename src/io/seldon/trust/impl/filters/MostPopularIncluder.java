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

package io.seldon.trust.impl.filters;

import io.seldon.general.ItemRetriever;
import io.seldon.general.jdo.SqlItemPeer;
import io.seldon.trust.impl.ItemIncluder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author firemanphil
 *         Date: 19/11/14
 *         Time: 11:49
 */
@Component
public class MostPopularIncluder implements ItemIncluder {

    @Autowired
    private ItemRetriever retriever;


    @Override
    public List<Long> generateIncludedItems(String client, int dimension, int numItems) {
        // first stab at this: lets return, say, the top 200 items.
        List<SqlItemPeer.ItemAndScore> itemsToConsider = retriever.retrieveMostPopularItems(client,numItems,dimension);
        List<Long> toReturn = new ArrayList<>();
        for (SqlItemPeer.ItemAndScore itemAndScore : itemsToConsider){
            toReturn.add(itemAndScore.item);
        }
        return toReturn.size() >= numItems ?
                new ArrayList<>(toReturn).subList(0,numItems) :
                new ArrayList<>(toReturn);
    }
}
