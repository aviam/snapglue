
//	http://movieapp-sitepointdemos.rhcloud.com/api/movies
angular.module('agentApp.services',[]).factory('Configuration',function($resource,agentURL){
//    return $resource('http://movieapp-13434.onmodulus.net/api/movies/:id',{id:'@_id'},{
      //  return $resource('http://movieapp-sitepointdemos.rhcloud.com/api/movies/:id',{id:'@_id'},{
        	// return $resource('/api/configurations/:id',{id:'@_id'},{
            	return $resource(agentURL + '/rest/mongo-api/configurations/:id',{id:'@_id.$oid'},{
	 
           	//return $resource('../configurations.json/{id}',{id:'@_id'},{

        update: {
            method: 'PUT'
        }
    });
}).service('popupService',function($window){
    this.showPopup=function(message){
        return $window.confirm(message);
    }
});