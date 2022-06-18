(function () {
    'use strict';

    angular.module('myApp.logout', ['ngRoute'])

        .config(['$routeProvider', function ($routeProvider) {
            $routeProvider.when('/logout', {
                templateUrl: 'logout/logout.html',
                controller: 'LogoutCtrl'
            });
        }])

        .controller('LogoutCtrl', ['$scope', '$location', '$rootScope', '$http', 'baseURL', 'currentUser',
            function ($scope, $location, $rootScope, $http, baseURL, currentUser) {
        	 localStorage.removeItem("DashboardInfo");
   		     localStorage.removeItem("sessionIdSnapglue");
   		     localStorage.removeItem("user");
            }]);

})();