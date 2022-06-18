(function () {
    'use strict';

    angular.module('myApp.rule_management')

        .config(['$routeProvider', function ($routeProvider) {
            $routeProvider.when('/rule-management/rule/:id/edit', {
                templateUrl: 'rule_management/edit_rule/edit_rule.html',
                controller: 'RuleEditCtrl'
            });
        }])

        .controller('RuleEditCtrl', ['$scope', '$http', 'baseURL', '$location', '$routeParams',
            function ($scope, $http, baseURL, $location, $routeParams) {
       

                $http({
                    //dataType: 'jsonp',
                    method: 'GET',
                    url: baseURL + '/rest/rulesmanagement/rules/'+$routeParams.id
                   // url: baseURL + '/rule_management/edit_rule/rule.json'
                    //headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                }).then(function (response) {
                	//alert(response)
                  //  console.log(response);
                    if (response && response.data)
                    	{
                    	//alert(response.data);
                    	$scope.rule = JSON.parse(response.data);
                    	//$scope.rule = response.data;
                    	  // alert($scope.user);
                    	}
                }, function errorCallback(response) {
                    // called asynchronously if an error occurs
                    // or server returns response with an error status.
                    console.error('error', response);
                });

            // submit (Update) button handler
            $scope.update = function () {
                this.errorMessage = "";
                
                var self = this;
                               
               

                var data = JSON.stringify({
                    "projectId": $scope.rule.projectId,
                    "type": $scope.rule.type,
                    "enabled":$scope.rule.enabled,                   
                    "kpis":$scope.rule.kpis
                });
               
                //localStorage.user=JSON.stringify($scope.rule);
                
                $.ajax({
                    url: baseURL + '/rest/rulesmanagement/rules/' + $routeParams.id,
                    type: "PUT",
                    data: JSON.stringify(data),
                    processData: false,
                    contentType: "application/json; charset=UTF-8",
                   
                    success: function(){  
                    	console.error('success', response);
                    },
                    error:function() {
                	  console.error('error', response);
                    }
                });

                $location.path("/rule-management");
                
            }
            $scope.cancel=function(){
            	window.history.back();
            }
        }]);

})();