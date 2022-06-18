'use strict';

angular.module('myApp.baseURL.baseURL-directive', [])

.directive('baseUrl', ['baseURL', function(baseURL) {
  return function(scope, elm, attrs) {
    elm.text(baseURL);
  };
}]);
