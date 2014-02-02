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
Ext.define('NX.coreui.view.security.Realms', {
  extend: 'Ext.Panel',
  alias: 'widget.nx-coreui-security-realms',
  requires: [
    'NX.ext.form.field.ItemSelector'
  ],

  layout: {
    type: 'vbox',
    align: 'stretch',
    pack: 'start'
  },

  maxWidth: 1024,

  style: {
    margin: '20px'
  },

  defaults: {
    style: {
      margin: '0px 0px 20px 0px'
    }
  },

  items: [
    // basic settings
    {
      xtype: 'form',
      items: [
        {
          xtype: 'label',
          html: '<p>Security realm settings.</p>'
        },
        {
          xtype: 'nx-itemselector',
          buttons: [ 'up', 'add', 'remove', 'down' ],
          fromTitle: 'Available',
          toTitle: 'Selected',
          store: Ext.create('Ext.data.ArrayStore', {
            fields: [
                'id', 'text'
            ],
            data: [
              [ 'default', 'Default Realm' ],
              [ 'fancy', 'Fancy Pants Realm' ]
            ]
          }),
          displayField: 'text'
        }
      ],

      buttonAlign: 'left',
      buttons: [
        { text: 'Save', ui: 'primary' },
        { text: 'Discard' }
      ]
    }
  ]

});