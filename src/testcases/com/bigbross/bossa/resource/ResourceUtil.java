/*
 * Bossa Workflow System
 *
 * $Id$
 *
 * Copyright (C) 2003 OpenBR Sistemas S/C Ltda.
 *
 * This file is part of Bossa.
 *
 * Bossa is free software; you can redistribute it and/or modify it
 * under the terms of version 2 of the GNU General Public License as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

package com.bigbross.bossa.resource;

import com.bigbross.bossa.wfnet.WFNetUtil;

public class ResourceUtil {

    public static Resource createResource(String id) {
        return createResource(null, id);
    }

    public static Resource createResource(ResourceManager manager, String id) {
        return new Resource(manager, id);
    }

    public static void prepareWorkTest(ResourceManager resourceManager) {
        Resource joe = resourceManager.createResourceImpl("joe");
        Resource mar = resourceManager.createResourceImpl("mary");
        Resource pan = resourceManager.createResourceImpl("pan");

        ResourceRegistry registry = resourceManager.getMainContext().getSubContext("test");
        Resource sol = registry.getResource("requesters");
        Resource com = registry.getResource("sales");
        Resource dir = registry.getResource("directors");

        sol.includeImpl(joe); sol.includeImpl(mar); sol.includeImpl(pan);
        com.includeImpl(joe); com.includeImpl(pan);
        dir.includeImpl(mar);
    }

}