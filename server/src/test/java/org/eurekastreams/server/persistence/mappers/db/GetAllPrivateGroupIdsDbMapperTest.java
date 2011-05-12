/*
 * Copyright (c) 2010-2011 Lockheed Martin Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eurekastreams.server.persistence.mappers.db;

import java.util.List;

import junit.framework.Assert;

import org.eurekastreams.server.persistence.mappers.MapperTest;
import org.junit.Test;

/**
 * Test fixture for GetAllPrivateGroupIdsDbMapper.
 */
public class GetAllPrivateGroupIdsDbMapperTest extends MapperTest
{
    /**
     * Test execute.
     */
    @Test
    public void testExecute()
    {
        getEntityManager().createQuery("UPDATE DomainGroup SET publicgroup = false WHERE id=4 OR id=5").executeUpdate();

        GetAllPrivateGroupIdsDbMapper sut = new GetAllPrivateGroupIdsDbMapper();
        sut.setEntityManager(getEntityManager());

        List<Long> domainGroupIds = sut.execute(null);

        Assert.assertEquals(2, domainGroupIds.size());
        Assert.assertTrue(domainGroupIds.contains(4L));
        Assert.assertTrue(domainGroupIds.contains(5L));
    }
}
