(function () {
    'use strict';

    angular.module('myApp.agent', ['ngRoute'])

        .config(['$routeProvider', function ($routeProvider) {
            $routeProvider.when('/agent', {
                templateUrl: 'agent/agent.html',
                controller: 'AgentCtrl'
            });
        }])

        .controller('AgentCtrl', ['$scope','$location', function ($scope, $location) {
            var user= localStorage.getItem("user");
            var userObjJSON = angular.fromJson(eval("(function(){return " + user + ";})()"));
            $scope.userID=userObjJSON._id.$oid;
            $scope.username=userObjJSON.username;
            //handle logout
            $scope.logout= function($event) {

                $location.path('/login');
                localStorage.removeItem("DashboardInfo");
                localStorage.removeItem("sessionIdSnapglue");
                localStorage.removeItem("user");



            }
        	    
        }]);

})();