/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2013 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.analytics.internal;

import java.util.Map;

import javax.inject.Named;

import org.sonatype.nexus.capability.support.CapabilitySupport;
import org.sonatype.nexus.plugins.capabilities.Condition;

// TODO: rename to autosubmit ?

/**
 * Analytics reporting capability.
 *
 * @since 2.8
 */
@Named(ReportingCapabilityDescriptor.TYPE_ID)
public class ReportingCapability
    extends CapabilitySupport<ReportingCapabilityConfiguration>
{
  @Override
  protected ReportingCapabilityConfiguration createConfig(final Map<String, String> properties) throws Exception {
    return new ReportingCapabilityConfiguration(properties);
  }

  @Override
  public Condition activationCondition() {
    return conditions().logical().and(
        // collection capability must be active
        conditions().capabilities().capabilityOfTypeActive(CollectionCapabilityDescriptor.TYPE),
        conditions().capabilities().passivateCapabilityDuringUpdate()
    );
  }
}
