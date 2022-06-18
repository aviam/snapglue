(function () {
    'use strict';

    angular.module('myApp.user_management')

        .config(['$routeProvider', function ($routeProvider) {
            $routeProvider.when('/user-management/user/:id/edit', {
                templateUrl: 'user_management/edit_user/edit_user.html',
                controller: 'UserEditCtrl'
            });
        }])

        .controller('UserEditCtrl', ['$scope', '$http', 'baseURL', '$location', '$routeParams',
            function ($scope, $http, baseURL, $location, $routeParams) {
        	     

                $http({
                    //dataType: 'jsonp',
                    method: 'GET',
                    url: baseURL + '/rest/usermanagement/users/'+$routeParams.id
                 
                    //headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                }).then(function (response) {
                	//alert(response)
                  //  console.log(response);
                    if (response && response.data)
                    	{
                    	   $scope.user = response.data;
                    	  // alert($scope.user);
                    	}
                }, function errorCallback(response) {
                    // called asynchronously if an error occurs
                    // or server returns response with an error status.
                    console.error('error', response.status);
                });

            // submit (Update) button handler
            $scope.update = function () {
                this.errorMessage = "";
                function validateEmail(email) {
                    var re = /^([\w-]+(?:\.[\w-]+)*)@((?:[\w-]+\.)*\w[\w-]{0,66})\.([a-z]{2,6}(?:\.[a-z]{2})?)$/i;
                    return re.test(email);
                }

                var self = this;
                var email = $scope.user.email;

                if (!$scope.user.username) {
                    this.errorMessage = "Please fill in username";
                    return;
                }

                if (!email || !validateEmail(email)) {
                    this.errorMessage = "Please fill a valid Email address";
                    return;
                }

                var data = JSON.stringify({
                    "username": $scope.user.username,
                    "role": $scope.user.role,
                    "email": $scope.user.email,
                    "pictureLink" : $scope.user.pictureLink,
                    "data" : $scope.user.data,
                    "password": $scope.user.password,
                    "enabled": true,
                    "accountNonExpired": true,
                    "credentialsNonExpired": true,
                    "accountNonLocked": true,
                    "tenant": $scope.user.tenant
                });
                var user= localStorage.getItem("user");
                var userObjJSON = angular.fromJson(eval("(function(){return " + user + ";})()"));
                $scope.userID=userObjJSON._id.$oid;
                $scope.emailFromLocal=userObjJSON.email;
                if ($scope.emailFromLocal==$scope.user.email) {
                    localStorage.user = JSON.stringify($scope.user);
                }
                $.ajax({
                    url: baseURL + '/rest/usermanagement/users/' + $routeParams.id,
                    type: "PUT",
                    data: data,
                    processData: false,
                    contentType: "application/json; charset=UTF-8",
                   
                    success: function(){
                        console.log("success to edit user")
                    	//console.succ('success', response);
                    },
                    error:function() {
                	  console.error('error', response);
                    }
                });

                $location.path("/user-management");
                
            }
            $scope.cancel=function(){
            	window.history.back();
            }
        }]);

})();