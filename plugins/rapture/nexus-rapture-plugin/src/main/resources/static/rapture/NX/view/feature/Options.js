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
Ext.define('NX.view.feature.Options', {
  extend: 'Ext.Panel',
  requires: [
    'NX.view.BlueButton'
  ],
  alias: 'widget.nx-feature-options',

  title: 'Options',
  width: 100,
  stateful: true,
  stateId: 'nx-feature-options',

  layout: {
    type: 'vbox',
    padding: 4,
    defaultMargins: {top: 0, right: 0, bottom: 4, left: 0}
  },

  defaults: {
    width: '100%'
  },

  items: [
    {
      xtype: 'button',
      text: 'normal'
    },
    {
      xtype: 'button',
      text: 'disabled',
      disabled: true
    },
    {
      xtype: 'button',
      text: 'success',
      ui: 'foo'
    },
    {
      xtype: 'button',
      text: 'primary',
      ui: 'blue'
    },
    {
      xtype: 'button',
      text: 'danger',
      ui: 'red'
    },
    {
      xtype: 'button',
      text: 'warning',
      ui: 'orange'
    }
  ]
});