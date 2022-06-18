'use strict';

angular.module('agentApp.agentURL.agentURL-directive', [])

    .directive('agentUrl', ['agentURL', function(agentURL) {
        return function(scope, elm, attrs) {
            elm.text(agentURL);
        };
    }]);
