'use strict';

angular.module('myApp.baseURL', [
  'myApp.baseURL.baseURL-directive'
])

.value('baseURL', 'http://46.101.198.128:8080/server');
