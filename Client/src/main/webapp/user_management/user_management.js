(function () {
    'use strict';

    angular.module('myApp.user_management', ['ngRoute'])

        .config(['$routeProvider', function ($routeProvider) {
            $routeProvider.when('/user-management', {
                templateUrl: 'user_management/user_management.html',
                controller: 'UserManagementCtrl'
            });
        }])
        
        
        .factory('User',function($resource){

             return $resource('/rest/usermanagement/users/:id',{id:'@_id.$oid'},{
  
            

       
    });
})

        .controller('UserManagementCtrl', ['$scope', '$http', 'baseURL', '$location', function ($scope, $http, baseURL, $location) {

            $scope.toggleEnable = function(user) {
                user.enabled = !user.enabled;
                update(user);
            };
            $scope.toggleCredentialsNonExpired = function(user) {
                user.credentialsNonExpired = !user.credentialsNonExpired;
                update(user);
            };
            $scope.toggleAccountNonExpired = function(user) {
                user.accountNonExpired = !user.accountNonExpired;
                update(user);
            };
            $scope.toggleAccountNonLocked = function(user) {
                user.accountNonLocked = !user.accountNonLocked;
                update(user);
            };
            $scope.deleteUser = function(user) {
         	   console.log("going to delete user: "+user._id.$oid);
            	   $http({
                       
                       method: 'DELETE',
                       url: baseURL + '/rest/usermanagement/users/'+user._id.$oid,
                       headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                   }).then(function (response) {
                       console.log(response);

                   }, function errorCallback(response) {
                       console.error('error', response);
                   });
                 $location.path("/user-management")
            };
            this.loadUsers = function() {
                $http({
                    method: 'GET',
                    url: baseURL + '/rest/usermanagement/users',
                    headers: { 'Content-Type': 'application/json' }
                
                }).then(function successCallback(response) {
                   
                    $scope.users = response.data
                }, function errorCallback(response) {
                   
                    console.error('error', response);
                });
            };
            this.loadUsers();
        }]);

})();