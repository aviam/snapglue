(function () {
    'use strict';

    angular.module('myApp.rule_management')

        .config(['$routeProvider', function ($routeProvider) {
            $routeProvider.when('/rule-management/create', {
                templateUrl: 'rule_management/add_rule/add_rule.html',
                controller: 'RuleAddCtrl'
            });
        }])

        .controller('RuleAddCtrl', ['$scope', '$http', 'baseURL', '$location',
            function ($scope, $http, baseURL, $location) {
           
            // submit (Add) button handler
            $scope.add = function () {
               

                var data = JSON.stringify({
                	"projectId": $scope.projectId,
                    "type": $scope.type,
                    "enabled":$scope.enabled,                   
                    "kpis":$scope.kpis
                });

 
                
                
                $.ajax({
                    url: baseURL + '/rest/rulesmanagement/rules',
                    type: "POST",
                    data: JSON.stringify(data),
                    processData: false,
                    contentType: "application/json; charset=UTF-8",
                   // complete: callback
                }).done(function() {
                	$location.path("/rule-management");
                }).fail(function() {
                	console.error('error', response);
                });

            }
            $scope.choices = [{id: 'choice1'}, {id: 'choice2'}];
            
            $scope.addNewChoice = function() {
              var newItemNo = $scope.choices.length+1;
              $scope.choices.push({'id':'choice'+newItemNo});
            };
              
            $scope.removeChoice = function() {
              var lastItem = $scope.choices.length-1;
              $scope.choices.splice(lastItem);
            };
//            $scope.addNewKpi = function() {
//                var key = $scope.kpis.key;
//                var value = $scope.kpis.value;
//                $scope.kpis.push({key:value});
//              };
//                
//              $scope.removeKpi = function() {
//                var lastItem = $scope.kpis.length-1;
//                $scope.kpis.splice(lastItem);
//              };
        }]);

})();