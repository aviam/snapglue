angular.module('agentApp',['ui.router','ngResource','agentApp.controllers','agentApp.services','angular-datepicker','agentApp.agentURL']);
angular.module('agentApp').config(function($stateProvider,$httpProvider){
    $stateProvider.state('configurations',{
        url:'/configurations',
        templateUrl:'/partials/configurations.html',
        controller:'ConfigurationListController'
    }).state('viewConfiguration',{
       url:'/configurations/:id/view',
       templateUrl:'/partials/configuration-view.html',
       controller:'ConfigurationViewController'
    }).state('newConfiguration',{
        url:'/configurations/new',
        templateUrl:'/partials/configuration-add.html',
        controller:'ConfigurationCreateController'
    }).state('editConfiguration',{
        url:'/configurations/:id/edit',
        templateUrl:'/partials/configuration-edit.html',
        controller:'ConfigurationEditController'
    }).state('stopAgentJob',{
    	url:'/configurations',
        templateUrl:'/partials/configurations.html',
        controller:'AgentStopJobController' 
    }).state('startAgentJob',{
    	url:'/configurations',
        templateUrl:'/partials/configurations.html',
        controller:'AgentStartJobController'       	
   });
}).run(function($state){
   $state.go('configurations');
});