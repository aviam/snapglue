(function () {
    'use strict';

    angular.module('myApp.rule_management', ['ngRoute'])

        .config(['$routeProvider', function ($routeProvider) {
            $routeProvider.when('/rule-management', {
                templateUrl: 'rule_management/rule_management.html',
                controller: 'RuleManagementCtrl'
            });
        }])
        
        
       .controller('RuleManagementCtrl', ['$scope', '$http', 'baseURL', '$location', function ($scope, $http, baseURL, $location) {
            
            $scope.toggleEnable = function(rule) {
                rule.enabled = !rule.enabled;
                update(rule);
            };
            
            $scope.deleteRule = function(rule) {
         	   console.log("going to delete rule: "+rule._id.$oid);
            	   $http({
                       
                       method: 'DELETE',
                       url: baseURL + '/rest/rulesmanagement/rules/'+rule._id.$oid,
                       headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                   }).then(function (response) {
                       console.log(response);
                       $location.path("/rule-management")
                   }, function errorCallback(response) {
                       console.error('error', response);
                   });
            	  
            };
            this.loadRules = function() {
            
                $http({
                    method: 'GET',
                   url: baseURL + '/rest/rulesmanagement/rules',
                   //url: "/rule_management/rule.json",
                    headers: { 'Content-Type': 'application/json' }
                
                }).then(function successCallback(response) {
                   $scope.rules = JSON.parse(response.data)
                  // $scope.rules = response.data
                }, function errorCallback(response) {
                   
                    console.error('error', response);
                });
            };
            this.loadRules();
        }]);

})();