/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

/** @module up-utils-outbound-js/persistence_query_service */
var utils = require('vertx-js/util/utils');

var io = Packages.io;
var JsonObject = io.vertx.core.json.JsonObject;
var JPersistenceQueryService = de.urbanpulse.outbound.PersistenceQueryService;

/**
 service interface for querying persisted events
 <p>
 @class
*/
var PersistenceQueryService = function(j_val) {

  var j_persistenceQueryService = j_val;
  var that = this;

  /**
   Query a persistence module's event storage. This can be invoked locally or across the cluster.
   <p>
   when execution has started, we periodically send to uniqueMessageHandle a JsonObject like this:
   <pre>
   {
      "batch": [ ... events, may be an empty array ... ]
   }
   </pre>
   <p>
   the end of a successful query is indicated by sending to uniqueMessageHandle a JsonObject like this:
   <pre>
   {
      "batch": [ ... remaining events, may be an empty array ... ],
      "isLast": true
   }
   </pre>
   <p>
   any exception will abort the query and we try sending to uniqueMessageHandle a JsonObject like this:
   <pre>
   {
      "abortingException": "RuntimeException: blabla ...."
   }
   </pre>

   @public
   @param sinceTimestamp {string} must use the following format: yyyy-MM-dd'T'HH:mm:ss.SSSZ 
   @param untilTimestamp {string} must use the following format: yyyy-MM-dd'T'HH:mm:ss.SSSZ 
   @param sid {string} sensor ID 
   @param batchSize {number} max. number of events to group in one batch that is sent as one single eventbus message 
   @param uniqueRequestHandle {string} unique address on the vert.x eventbus for this query which will receive the batches 
   @param resultHandler {function} called with a successful  if query execution was started successfully, otherwise called with a failed one 
   */
  this.query = function(sinceTimestamp, untilTimestamp, sid, batchSize, uniqueRequestHandle, resultHandler) {
    var __args = arguments;
    if (__args.length === 6 && typeof __args[0] === 'string' && typeof __args[1] === 'string' && typeof __args[2] === 'string' && typeof __args[3] ==='number' && typeof __args[4] === 'string' && typeof __args[5] === 'function') {
      j_persistenceQueryService["query(java.lang.String,java.lang.String,java.lang.String,int,java.lang.String,io.vertx.core.Handler)"](sinceTimestamp, untilTimestamp, sid, batchSize, uniqueRequestHandle, function(ar) {
      if (ar.succeeded()) {
        resultHandler(null, null);
      } else {
        resultHandler(null, ar.cause());
      }
    });
    } else throw new TypeError('function invoked with invalid arguments');
  };

  // A reference to the underlying Java delegate
  // NOTE! This is an internal API and must not be used in user code.
  // If you rely on this property your code is likely to break if we change it / remove it without warning.
  this._jdel = j_persistenceQueryService;
};

// We export the Constructor function
module.exports = PersistenceQueryService;