'use strict';

// Declare app level module which depends on views, and components
angular.module('myApp', [
    'ngRoute',
    'gridster',
    'myApp.login',
    'myApp.dashboard',
    'myApp.user_management',
    'myApp.rule_management',
    'myApp.agent',
    'myApp.version',
    'myApp.baseURL',
    'cgNotify'
]).
    config(['$routeProvider', function ($routeProvider) {
        $routeProvider.otherwise({redirectTo: '/login'});
    }])
    .run( function($rootScope, $location) {
        // register listener to watch route changes
        $rootScope.$on( "$routeChangeStart", function(event, next, current) {
        	// check if the user is still logged in
        	var user= JSON.parse(localStorage.getItem("user"));
        	
        	if (user!=null){
        	  if (user['username']!=null && localStorage.getItem("sessionIdSnapglue")  )
        	
        	     {
        		   $rootScope.loggedUser=user['username'] ;
        	    }
        	}
        	else {
        	
               if ( $rootScope.loggedUser == null ) {
                // no logged user, we should be going to #login
                  if ( next.templateUrl == "/partials/login.html" ) {
                    // already going to #login, no redirect needed
                } else {
                    // not going to #login, we should redirect now
                    $location.path( "/login" );
                }
              }
        	}
        });
    });
