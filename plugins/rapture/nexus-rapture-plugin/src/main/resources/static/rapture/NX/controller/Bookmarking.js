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
/**
 * Controls bookmarking.
 *
 * @since 2.8
 */
Ext.define('NX.controller.Bookmarking', {
  extend: 'Ext.app.Controller',
  mixins: {
    logAware: 'NX.LogAware'
  },
  requires: [
    'NX.Bookmark'
  ],

  /**
   * @override
   */
  init: function () {
    var me = this;

    // The only requirement for this to work is that you must have a hidden field and
    // an iframe available in the page with ids corresponding to Ext.History.fieldId
    // and Ext.History.iframeId.  See history.html for an example.
    Ext.History.init();

    me.bindToHistory();

    me.addEvents(
        /**
         * @event navigate
         * Fires when user navigates to a new bookmark.
         * @param {String} bookmark value
         */
        'navigate'
    );
  },

  /**
   * @public
   * @returns {NX.Bookmark} current bookmark
   */
  getBookmark: function () {
    return NX.Bookmark.fromToken(Ext.History.getToken());
  },

  /**
   * @public
   * Sets bookmark to a specified value.
   * @param {NX.Bookmark} newBookmark new bookmark
   */
  bookmark: function (newBookmark) {
    var me = this,
        oldValue = me.getBookmark().getToken();

    if (newBookmark && oldValue != newBookmark.getToken()) {
      // unbind first to avoid navigation callback
      Ext.History.bookmark = newBookmark.getToken();
      Ext.History.add(newBookmark.getToken());
    }
  },

  /**
   * @public
   * Sets bookmark to a specified value and navigates to it.
   * @param {NX.Bookmark} bookmark to navigate to
   */
  navigate: function (bookmark) {
    var me = this;

    if (bookmark) {
      me.logDebug('Navigate to: ' + bookmark.getToken());
      me.bookmark(bookmark);
      me.fireEvent('navigate', bookmark);
    }
  },

  /**
   * @override
   * Navigate to current bookmark.
   */
  onLaunch: function () {
    var me = this;

    me.navigate(me.getBookmark());
  },

  /**
   * @private
   * Sets bookmark to a specified value and navigates to it.
   * @param {String} token to navigate to
   */
  onNavigate: function (token) {
    var me = this;

    if (token != Ext.History.bookmark) {
      delete Ext.History.bookmark;
      me.navigate(NX.Bookmark.fromToken(token));
    }
  },

  /**
   * @private
   * Start listening to **{@link Ext.History}** change events.
   */
  bindToHistory: function () {
    var me = this;

    Ext.History.on('change', me.onNavigate, me);
  }

});