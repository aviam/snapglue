(function () {
    'use strict';

    angular.module('myApp.login', ['ngRoute'])

        .config(['$routeProvider', function ($routeProvider) {
            $routeProvider.when('/login', {
                templateUrl: 'login/login.html',
                controller: 'LoginCtrl'
            });
        }])

        .controller('LoginCtrl', ['$scope', '$location', '$rootScope', '$http', 'baseURL', 'currentUser',
            function ($scope, $location, $rootScope, $http, baseURL, currentUser) {
                if ($rootScope.loggedUser) {
                   // $location.path("/dashboard");
                    //for now redirect to redash
                    $location.path("/agent");
                }
                $scope.login = function () {
                    this.errorMessage = "";
                    function validateEmail(email) {
                        var re = /^([\w-]+(?:\.[\w-]+)*)@((?:[\w-]+\.)*\w[\w-]{0,66})\.([a-z]{2,6}(?:\.[a-z]{2})?)$/i;
                        return re.test(email);
                    }

                    var self = this;
                    var email = $scope.email;
                    var password = $scope.password;
                    if (!email || !validateEmail(email)) {
                        this.errorMessage = "Please fill a valid Email address";
                    }
                    if (!password) {
                        this.errorMessage = "Please fill a valid password";
                        return;
                    }

                    var data = $.param({ 'j_username': email, 'j_password':password});

                    $http({
                        data: data,
                        dataType: 'jsonp',
                        method: 'POST',
                        url: baseURL + '/j_spring_security_check',
                        headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                    }).then(function (response) {
                       
                        if (response.statusText == "OK" && response.status == 200) {
                            if (response.data) {
                                var data = response.data;
                                switch (data.login) {
                                    case 'SUCCESS':
                                        var sessionId = data['SESSIONID'];
                                        var user = data['user'];
                                        var field = function(field) {
                                            var value = user[field];
                                            if (!value) console.warn('field ', field, ' is missing, undefined or null');
                                            return value;
                                        };
                                        currentUser.setCurrentUser({
                                            id: field['id'],
                                            username: field['username'],
                                            password: field['password'],
                                            role: field['role'],
                                            email: field['email'],
                                            pictureLink: field['pictureLink'],
                                            data : field['data'],
                                            enabled: field['enabled'],
                                            accountNonExpired: field['accountNonExpired'],
                                            credentialsNonExpired: field['credentialsNonExpired'],
                                            accountNonLocked: field['accountNonLocked'],
                                            tenant: field['tenant']
                                        });
                                        $rootScope.sessionId = sessionId;
                                        localStorage.sessionIdSnapglue=sessionId;
                                        $rootScope.loggedUser = $scope.email;
                                        console.log(user);
                                        localStorage.user=JSON.stringify(user);
                                       // $location.path("/dashboard");
                                        //for now redirect to agent configuration page
                                        $location.path("#/agent");
                                        break;
                                    case 'FAILURE':
                                        self.errorMessage = "Can't login using current email/password pair";
                                        break;
                                    default:
                                        break;
                                }
                            }
                        }
                    }, function errorCallback(response) {
                        
                        console.error('error', response);
                    });
                }
            }]);

})();