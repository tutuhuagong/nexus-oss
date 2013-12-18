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

import java.lang.annotation.Annotation;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.extdirect.ExtDirectResource;

import com.director.core.DirectConfiguration;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Key;
import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.inject.BeanLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link DirectConfiguration} pre-configured with available {@link ExtDirectResource}s.
 *
 * @since 2.8
 */
@Named
@Singleton
public class ExtDirectConfiguration
    extends DirectConfiguration
{

  private static final Logger LOG = LoggerFactory.getLogger(ExtDirectConfiguration.class);

  @Inject
  public ExtDirectConfiguration(final BeanLocator beanLocator,
                                final ExtDirectExecutorAdapter executorAdapter)
  {
    setRouterUrl(ExtDirectModule.ROUTER_MOUNT_POINT);
    registerAdapter(checkNotNull(executorAdapter));

    Iterable<? extends BeanEntry<Annotation, ExtDirectResource>> entries = checkNotNull(beanLocator).locate(
        Key.get(ExtDirectResource.class)
    );
    List<Class<?>> apiClasses = Lists.newArrayList(
        Iterables.transform(entries, new Function<BeanEntry<Annotation, ExtDirectResource>, Class<?>>()
        {
          @Nullable
          @Override
          public Class<?> apply(final BeanEntry<Annotation, ExtDirectResource> input) {
            return input.getImplementationClass();
          }
        })
    );
    for (Class<?> entry : apiClasses) {
      LOG.debug("Registering direct resource {}", entry.getName());
      registerClass(
          entry, "NX.direct", entry.getSimpleName().replace("DirectResource", "")
      );
    }
  }

}
