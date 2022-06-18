(function () {
    'use strict';

    angular.module('myApp.user_management')

        .config(['$routeProvider', function ($routeProvider) {
            $routeProvider.when('/user-management/create', {
                templateUrl: 'user_management/add_user/add_user.html',
                controller: 'UserAddCtrl'
            });
        }])

        .controller('UserAddCtrl', ['$scope', '$http', 'baseURL', '$location',
            function ($scope, $http, baseURL, $location) {
            // default role
            $scope.role = "ROLE_USER";
            // submit (Add) button handler
            $scope.add = function () {
                this.errorMessage = "";
                function validateEmail(email) {
                    var re = /^([\w-]+(?:\.[\w-]+)*)@((?:[\w-]+\.)*\w[\w-]{0,66})\.([a-z]{2,6}(?:\.[a-z]{2})?)$/i;
                    return re.test(email);
                }

                var self = this;
                var email = $scope.email;

                if (!$scope.username) {
                    this.errorMessage = "Please fill in username";
                    return;
                }

                if (!email || !validateEmail(email)) {
                    this.errorMessage = "Please fill a valid Email address";
                    return;
                }

                if (!$scope.password) {
                    this.errorMessage = "Please fill a password";
                    return;
                }

                var data = JSON.stringify({
                    "username": $scope.username,
                    "role": $scope.role,
                    "email": $scope.email,
                    "password": $scope.password,
                    "enabled": true,
                    "accountNonExpired": true,
                    "credentialsNonExpired": true,
                    "accountNonLocked": true,
                    "pictureLink" : "",
                    "data" : "",
                    "tenant": ($scope.tenant) ? $scope.tenant : ""
                });

 
                
                
                $.ajax({
                    url: baseURL + '/rest/usermanagement/users',
                    type: "POST",
                    data: data,
                    processData: false,
                    contentType: "application/json; charset=UTF-8",
                   // complete: callback
                }).done(function() {
                    console.log("Success to ass user")
                }).fail(function() {
                	console.error('error', response);
                });
                $location.path("/user-management");
            }
        }]);

})();