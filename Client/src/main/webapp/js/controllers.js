
angular.module('agentApp.controllers',[]).controller('ConfigurationListController',function($scope,$state,popupService,$window,Configuration){

    $scope.configurations=Configuration.query(function() {
        console.log($scope.configurations   );
    });
    $scope.deleteConfiguration=function(configuration){
        if(popupService.showPopup('Really delete this?')){
        	configuration.$delete(function(){
                $window.location.href='';
            });
        }
    };
}).controller('ConfigurationViewController',function($scope,$stateParams,Configuration){
    $scope.configuration=Configuration.get({id:$stateParams.id});
}).controller('AgentStopJobController',function($scope,popupService,$http,$window,$timeout, agentURL,$stateParams){

	  if(popupService.showPopup('Really Stop Agent Job?')){
		  popupService.showPopup("wait few seconds..");
		  $http.get(agentURL + '/rest/api/stopAgentJob?confId='+$stateParams.id)
		     .success(function (data, status, headers, config) {
		    	 popupService.showPopup(data);
		    	 $window.location.href='';	
			  }).
			  error(function (data, status, headers, config) {
				  popupService.showPopup(data);
			  });
	    }
}).controller('AgentStartJobController',function($scope,popupService,$http,$window,$timeout,agentURL,$stateParams){


			  $http.get(agentURL + '/rest/api/startAgentJob?confId='+$stateParams.id)
			     .success(function (data, status, headers, config) {
			    	 popupService.showPopup(data);
			    	 $window.location.href='';	
				  }).
				  error(function (data, status, headers, config) {
					  popupService.showPopup(data);
				  });
			  
			    
   
}).controller('ConfigurationCreateController',function($scope,$state,$stateParams,Configuration){

    $scope.configuration=new Configuration();

    $scope.addConfiguration=function(){
        $scope.configuration.$save(function(){
            $state.go('configurations');
        });
    }
}).controller('FieldController',function($scope,$state,$stateParams,Configuration){
            var user= localStorage.getItem("user");
            var userObjJSON = angular.fromJson(eval("(function(){return " + user + ";})()"));
            $scope.tenant=userObjJSON.tenant;

}).controller('ConfigurationEditController',function($scope,$state,$stateParams,Configuration){

    $scope.updateConfiguration=function(){
        $scope.configuration.$update(function(){
            $state.go('configurations');
        });
    };

    $scope.loadConfiguration=function(){
        $scope.configuration=Configuration.get({id:$stateParams.id});
    };

    $scope.loadConfiguration();
});
function callAtTimeout() {
	 popupService.showPopup("Wait few seconds..");
}