/**
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2012 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.tasks.descriptors;

import org.codehaus.plexus.component.annotations.Component;

@Component( role = ScheduledTaskDescriptor.class, hint = "PurgeApiKeys", description = "Purge Orphaned API Keys" )
public class PurgeApiKeysTaskDescriptor
    extends AbstractScheduledTaskDescriptor
{
    public static final String ID = "PurgeApiKeysTask";

    public String getId()
    {
        return ID;
    }

    public String getName()
    {
        return "Purge Orphaned API Keys";
    }
}
