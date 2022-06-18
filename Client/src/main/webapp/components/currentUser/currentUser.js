'use strict';

angular.module('myApp')
    .factory('currentUser', function () {
        var currentUser = undefined;
        return {
            setCurrentUser : function(user) {
                currentUser = user
            },
            getCurrentUser : function() {
                return currentUser
            }
        }
    });
