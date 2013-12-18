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

package org.sonatype.nexus.extdirect.ux.model;

import java.util.List;

/**
 * Ext.Direct response.
 *
 * @since 2.8
 */
public class Response<E>
{

  private boolean success;

  private boolean shouldRefresh;

  private List<E> entries;

  public Response(boolean success, List<E> entries) {
    this.success = success;
    this.entries = entries;
  }

  public Response shouldRefresh() {
    shouldRefresh = true;
    return this;
  }

}
