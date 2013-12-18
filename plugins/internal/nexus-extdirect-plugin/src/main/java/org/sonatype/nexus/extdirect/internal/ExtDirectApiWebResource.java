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

package org.sonatype.nexus.extdirect.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.web.WebResource;

/**
 * Ext.Direct API {@link WebResource}.
 *
 * @since 2.8
 */
@Named
@Singleton
public class ExtDirectApiWebResource
    implements WebResource
{

  private final String api;

  private final long lastModified;

  @Inject
  public ExtDirectApiWebResource(final ExtDirectConfiguration configuration) {
    api = configuration.getFormattedApi();
    lastModified = System.currentTimeMillis();
  }

  @Override
  public String getPath() {
    return "/js/extdirect.js";
  }

  @Nullable
  @Override
  public String getContentType() {
    return "text/javascript";
  }

  @Override
  public long getSize() {
    return api.getBytes().length;
  }

  @Override
  public long getLastModified() {
    return lastModified;
  }

  @Override
  public boolean isCacheable() {
    return true;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return new ByteArrayInputStream(api.getBytes());
  }

}
