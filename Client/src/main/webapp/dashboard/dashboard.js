'use strict';

angular.module('myApp.dashboard', ['ngRoute', 'gridster','ui.bootstrap','smart-table','chart.js','googlechart'])

    .config(['$routeProvider', function ($routeProvider) {
        $routeProvider.when('/dashboard', {
            templateUrl: 'dashboard/dashboard.html',
            controller: 'DashboardCtrl'
        });
    }])

    .controller('DashboardCtrl', ['$scope','$rootScope', '$location','$timeout', '$modal', '$http', 'baseURL','notify', function ($scope,$rootScope, $location, $timeout, $modal, $http, baseURL,notify) {
     
    			$scope.gridsterOptions = {	
  
   		    	margins: [20, 20],
   			    columns: 8,
    			helper: 'clone',
    			//width: 'auto',
    			//autosize:true,
    			draggable: {
    				handle: 'h3',
    				stop: function (event, ui) {
    					$(window).trigger('resize');
    					$scope.saveDashboard();

		             }
    			},
    			 resize: {
			             enabled: true,
			             stop: function (event, ui,chart) {
			            	 chart.resize();
			            	$scope.saveDashboard();

			             }
			             
			        }
			      
    		};
    			
    			var user= localStorage.getItem("user");
        		var userObjJSON = angular.fromJson(eval("(function(){return " + user + ";})()"));
        		$scope.userID=userObjJSON._id.$oid;
        		
        		  $scope.isRoleAdmin=false;
        			if ( userObjJSON.role == "ROLE_ADMIN" ){
        				$scope.isRoleAdmin=true;
        				
        			}

               
    		 //handle daily report 
    			$scope.$on('create', function (event, chart) {
    				
    				chart.resize();
    				});
                $scope.$on('update', function (event, chart) {
    				
    				chart.resize();
    				});
    		    $scope.dailyFetchContent = function() {
    		    	$scope.rowCollection = [];
    		    	$http({
                        method: 'GET',
                        url: baseURL + '/rest/report/daily',
                        // url: 'dashboard/daily.json',
                        headers: { 'Content-Type': 'application/json' }
               
                    }).then(function successCallback(response) {
                        
                    	$scope.rowCollection = JSON.parse(response.data)
                    }, function errorCallback(response) {
                      
                        console.error('error', response);
                    });
    		    	 $scope.dailycontent = [].concat($scope.rowCollection);
    		    	 $scope.itemsByPage=6;
    		     }
    		     
    		     $scope.dailyFetchContent();
    		 //end handling daily report    
    		     
    		//handle user activities
    		    
    		     $scope.fetchContentUserActivities = function() {
     		    	
     		    	$http({
                         method: 'GET',
                         url: 'dashboard/userActivities.json',
                          //url: baseURL + '/rest/usersinfo/all',
                         headers: { 'Content-Type': 'application/json' }
                
                     }).then(function successCallback(response) {
                    	 
                     $scope.labelsUserActivities = response.data.labelsUserActivities;
                     $scope.seriesUserActivities = response.data.seriesUserActivities; 
                  	 $scope.dataUserActivities = response.data.dataUserActivities;
                     }, function errorCallback(response) {
                       
                         console.error('error', response);
                     });
     		    	 $scope.optionUserActivities = {
                    		 responsive: true,
                    		 width: '100%',
                    		 scaleShowGridLines : false,
                    		 tooltipFontSize: 10,
                    		 barStrokeWidth : 8,
                    		
                            // height: 150,
                    		// maintainAspectRatio: false,
                    		 
                     };
     		     }
    		     $scope.addAlerts = function($event) {
    		    	// $event.stopPropagation();
    					$scope.dashboard.widgets.push({
    					 content: "<div class='box' ng-controller='CustomWidgetCtrl'>"
    							+"<div class='box-header'>"
    							+"<h3>{{ widget.name }}</h3>"
    			   				+"<div class='box-header-btns pull-right'>"
    								+"<a title='settings' ng-click='openSettings(widget)'><i class='glyphicon glyphicon-cog'></i></a>"
    								+"<a title='Remove widget' ng-click='remove(widget)'><i class='glyphicon glyphicon-trash'></i></a>"
    							+"</div>"
    						+"</div>"
    						+"<div class='box-content'>"
    					
    						   +"<table st-table='alertscontent' st-safe-src='rowCollectionAlerts' class='table table-striped'>"+
    						  	"<thead>"+
    							"<tr>"+
    								
    								"<th st-sort='sprintName'>SprintName<i class='fa fa-caret-down'></i></th>"+
    								 "<th st-sort='messagee'>Message<i class='fa fa-caret-down'></i></th>"+
    								
    							 "</tr>"+
    							 "<tr>"+
    								"<th colspan='2'>"+
    									"<input st-search placeholder='global search...' class='input-sm form-control'>"+
    												 
    								"</th>"+
    							"</tr>"+
    							"</thead>"+
    							"<tbody>"+
    							"<tr ng-repeat='row in alertscontent' ng-class='rowClassAlerts()'>"+
    								"<td>{{row.sprintName}}</td>"+
    								"<td>{{row.message}}</td>"+
    								
    							"</tr>"+
    							"</tbody>"+
    							"<tfoot>"+
    							"<tr>"+
    								"<td colspan='2' class='text-center'>"+
    									"<div st-pagination='' st-items-by-page='itemsByPage' st-displayed-pages='7'></div>"+
    								"</td>"+
    							"</tr>"+
    						   "</tfoot>"+
    						"</table>"
    						
    						
    						
    						
    					   +"</div>"
    						
    					+"</div>",
    						name: "Alerts",
    						sizeX: 3,
    						sizeY: 2,
    					});
    					 
    					 $scope.saveDashboard();
    					 
    				};
    		     $scope.onClickBar = function (points, evt) {
    		    	 console.log(points, evt);


    		    	    //alert(activeBars);
    		    	//    alert(points[2].value); // 0 -> Series A, 1 -> Series B
    		    	    
    		    	  };
    		     Chart.defaults.global.responsive = true;	
    		     $scope.fetchContentUserActivities();
    		     
    		 //end handling user Activities
    		     
    		   //handle Changes Trend
    		  
    		     $scope.fetchChengesTrend = function() {
     		    	
     		    	$http({
                         method: 'GET',
                         url: 'dashboard/trend.json',
                       //url: baseURL + '/rest/usersinfo/all',
                         headers: { 'Content-Type': 'application/json' }
                
                     }).then(function successCallback(response) {
                     $scope.labelsChangesTrend = response.data.labelsChangesTrend;
                     $scope.seriesChangesTrend = response.data.seriesChangesTrend; 
                  	 $scope.dataChangesTrend = response.data.dataChangesTrend;
                     }, function errorCallback(response) {
                       
                         console.error('error', response);
                      });
     		        	$scope.optionChangesTrend = {
                   		 responsive: true,
                   		 maintainAspectRatio: true,
                         width: '100%',
                		 scaleShowGridLines : false,
                		 tooltipFontSize: 10,         		
                		 barStrokeWidth : 8,
                    };
     		     }

                 

    		     $scope.fetchChengesTrend();
    		     
    		 //end handling Changes Trend
    		     
    		//handle Sprint Status Gauge
    		     
    		     $scope.fetchContentForGauges = function() {
     		    	
     		    	$http({
                         method: 'GET',
                         //url: baseURL + '/rest/gauges/all',,
                         url: 'dashboard/gauges.json',
                         headers: { 'Content-Type': 'application/json' }
                
                     }).then(function successCallback(response) {
                         
                    	$scope.chartObject.data =response.data;
                    	
                     }, function errorCallback(response) {
                       
                         console.error('error', response);
                     });
     		    	  
       		     $scope.chartObject = {};
       		     $scope.chartObject.type = "Gauge";

       		     $scope.chartObject.options = {
       		       
       		       width : '100%',
       		       greenFrom : 0,
       		       greenTo: 75,
       		       redFrom: 90,
       		       redTo: 100,
       		       yellowFrom: 75,
       		       yellowTo: 90,
       		       minorTicks: 5
       		     };
     		     }
    		   
    		     $scope.fetchContentForGauges();
    		    //end handling gauges
    		     //handle fetch alerts
    		     $scope.notification=0;
    		     $scope.fetchAlerts = function() {
    		    	
      		    	$http({
                          method: 'GET',
                          url: baseURL + '/rest/alertsmanagement/alerts',
                          //url: 'dashboard/alerts.json',
                          headers: {'Content-Type': 'application/json'}
                 
      		    	 }).then(function successCallback(response) {
      		    		$scope.rowCollectionAlerts = JSON.parse(response.data);
      		    	    //$scope.rowCollectionAlerts=response.data;
                    	 //$scope.alerts=response.data;
                    	 $scope.alerts=JSON.parse(response.data);
                    	 $scope.notification=$scope.alerts.length;
                    	
                    	  for(var i = 0; i < $scope.alerts.length; i++)
			                {	
                    		  notify({ message:$scope.alerts[i].message, position:'center',duration:4000,classes:'alert-danger notifyMessage'} );

			                }                                              	 
                     	
      		    	 }, function errorCallback(response) {
                        
                          console.error('error', status);
                      });
      		    	 $scope.alertscontent = [].concat($scope.rowCollectionAlerts);
        		   
      		     }
    		     $scope.fetchAlerts();
    		     //finish fetch alerts
    		    
    		$scope.dashboards = {
    			'1': {
    				id: '1',
    				name: 'Home',
				widgets: []
    			},
    			'2': {
    				id: '2',
    				name: 'Other',
    				widgets: [{
    					col: 1,
    					row: 1,
    					sizeY: 1,
    					sizeX: 2,
    					name: "Other Widget 1"
    				}, {
    					col: 1,
    					row: 3,
    					sizeY: 1,
    					sizeX: 1,
    					name: "Other Widget 2"
    				}]
    			}
    		};
    		
    		//save dashboard
    	  $scope.saveDashboard=function(){	
    		$scope.dashboardJSON = angular.toJson($scope.dashboard.widgets);
    		var user= localStorage.getItem("user");
    		var userObjJSON = angular.fromJson(eval("(function(){return " + user + ";})()"));
    		localStorage.DashboardInfo=JSON.stringify($scope.dashboardJSON);
    		
    	    var data = JSON.stringify({
    	    	"id":userObjJSON._id.$oid,
                "username":userObjJSON.username,
                "role": userObjJSON.role,
                "email": userObjJSON.email,
                "pictureLink" : userObjJSON.pictureLink,
                "data" : $scope.dashboardJSON,
                "password": userObjJSON.password,
                "enabled": userObjJSON.enabled,
                "accountNonExpired": userObjJSON.accountNonExpired,
                "credentialsNonExpired": userObjJSON.credentialsNonExpired,
                "accountNonLocked": userObjJSON.accountNonLocked,
                "tenant": userObjJSON.tenant
            });
          
    	    $http({
    	        method: 'PUT',
    	        url: baseURL + '/rest/usermanagement/users/'+userObjJSON._id.$oid,
    	        data: JSON.stringify(data),
    	        headers: {'Content-Type': 'application/json' }
    	        }).success(function(data, status, headers, config)
    	        {

    	        }).error(function(data, status, headers, config)
    	        {

    	        }); 
    	   }
    	  
    	  
    	 
    	  //handle logout
    	  $scope.logout=function($event){


			  $location.path('/login');
			//  $location.reload();
    		  localStorage.removeItem("DashboardInfo");
    		  localStorage.removeItem("sessionIdSnapglue");
    		  localStorage.removeItem("user");

    	  }
    	  $scope.loadWidgets=function(){
				
  			var dashboardInfo = localStorage.getItem("DashboardInfo");
  			var user= localStorage.getItem("user");
      		var userObjJSON = angular.fromJson(eval("(function(){return " + user + ";})()"));
      		
  			if (dashboardInfo == null)
  			{
  			    console.log("Loading from database");
  			    //Make an http request to get the current logged in user's saved dashboard
  			        $http({
  			        method: 'get',
  			        url: baseURL + '/rest/usermanagement/users/'+userObjJSON._id.$oid,
  			        headers: {'Content-Type': 'application/json' }
  			        }).success(function(data, status, headers, config)
  			        		
  			        		
  			        { 			        			
  			             var parsedUser = angular.fromJson(data);
  			        	 
  			        	     
  			          if(parsedUser.data!=null ) {    
  			        	     
  			        	 // var parsedDashboard = Object.keys(parsedUser.data).map(function(k) { return (parsedUser.data)[k] });
			                
  			        	   var parsedDashboard = angular.fromJson(eval("(function(){return " + parsedUser.data + ";})()"));
			                    console.log(parsedDashboard);
			           
			                	for(var i = 0; i < parsedDashboard.length; i++)
	  			                {
			                		
	  			                    console.log(parsedDashboard[i]);
	  			                	$scope.dashboard.widgets.push(parsedDashboard[i]);
	  			                	
	  			                   
	  			                }                           
	  			              
	  			                $scope.dashboardJSON = angular.toJson($scope.dashboard.widgets);
	  			                localStorage.DashboardInfo=$scope.dashboardJSON;
			                }
  			        	
  			        	
  			               else
  			                  {
  			                
  			            	  console.log("no saved widgets in db");
  			                
  			               
  			                 }

  			        }).error(function(data, status, headers, config)
  			        {

  			        });
  			}
  			else
  			{
  			   
  			    console.log("Loading from Local Storage");
  			   var parsedDashboard = angular.fromJson(eval("(function(){return " + dashboardInfo + ";})()"));
  			    $scope.dailyFetchContent();
  			    for(var i = 0; i < parsedDashboard.length; i++)
  			    {
  			        console.log(parsedDashboard[i]);
  			       
  			        $scope.dashboard.widgets.push(parsedDashboard[i]);
  			        
  			       
  			    }
  			   
  			}
  				
  		}
    		//color daily report table
    	  
    		$scope.rowClass = function(row){
    		if (row.type == "BUILDS")
    	        {
    			    
    	        	return row.status;
    	        }
    	    if (row.type == "ISSUES")
    	        	{
    	    	    
    	    	     return "issues";
    	        	}
    	    if (row.type == "COMMIT"){
    	      
    		   return "commits";
    	     } 	 
    	        	
    	        
    	    };
    	    $scope.rowClassAlerts = function(){
        		
        			    
        	        	return 'FAILURE';
        	     
        	        
        	    };
    	    $scope.isCurrentLocation = function(path){
    	        return path === $location.path()
    	    }
    		$scope.clear = function() {
    			$scope.dashboard.widgets = [];
    			$scope.saveDashboard();
    		};

    		//open window to choose gadget
    		$scope.addGadget = function() {
    			$modal.open({
    				scope: $scope,
    				templateUrl: 'dashboard/addGadget.html',
    				controller: 'AddGadgetCtrl',

    			});
    		};
    		$scope.$watch('selectedDashboardId', function(newVal, oldVal) {
    			if (newVal !== oldVal) {
    				$scope.dashboard = $scope.dashboards[newVal];
    			} else {
    				$scope.dashboard = $scope.dashboards[1];
    			}
    		   $scope.loadWidgets();
    		});
    		//$scope.dashboard = $scope.dashboards[1];
    		// init dashboard
    		//$scope.loadWidgets();
    		$scope.selectedDashboardId = '1';
    		
    	}
    ])

    .controller('CustomWidgetCtrl', ['$scope', '$modal',
    	function($scope, $modal) {

    		$scope.remove = function(widget) {
    			$scope.dashboard.widgets.splice($scope.dashboard.widgets.indexOf(widget), 1);
    			$scope.saveDashboard();
    		};

    		$scope.openSettings = function(widget) {
    			$modal.open({
    				scope: $scope,
    				templateUrl: 'dashboard/widget_settings.html',
    				controller: 'WidgetSettingsCtrl',
    				resolve: {
    					widget: function() {
    						return widget;
    					}
    				}
    			});
    		};

    	}
    ])
    .controller('WidgetSettingsCtrl', ['$scope', '$timeout', '$rootScope', '$modalInstance', 'widget',
    	function($scope, $timeout, $rootScope, $modalInstance, widget) {
    		$scope.widget = widget;

    		$scope.form = {
    			name: widget.name,
    			sizeX: widget.sizeX,
    			sizeY: widget.sizeY,
    			col: widget.col,
    			row: widget.row
    		};

    		$scope.sizeOptions = [{
    			id: '1',
    			name: '1'
    		}, {
    			id: '2',
    			name: '2'
    		}, {
    			id: '3',
    			name: '3'
    		}, {
    			id: '4',
    			name: '4'
    		}];

    		$scope.dismiss = function() {
    			$modalInstance.dismiss();
    			//$scope.saveDashboard();
    		};

    		$scope.remove = function() {
    			$scope.dashboard.widgets.splice($scope.dashboard.widgets.indexOf(widget), 1);
    			$modalInstance.close();
    			$scope.saveDashboard();
    		};

    		$scope.submit = function() {
    			angular.extend(widget, $scope.form);
    			$scope.saveDashboard();
    			$modalInstance.close(widget);
    		};
    	   
//    		$scope.isTrend=false;
//    		if ( widget.name == "Changes Trend Line" || widget.name == "Changes Trend Bar"	 ){
//    			$scope.isTrend=true;
//    		}
//    		$scope.data = {
//				    sprintSelect: null,
//				    availableOptions: [
//				      {id: '1', name: 'Option A'},
//				      {id: '2', name: 'Option B'},
//				      {id: '3', name: 'Option C'}
//				    ],
//				   };
//    	   alert($scope.data.sprintSelect);
    	
    }]).controller('AddGadgetCtrl', ['$scope', '$timeout', '$rootScope', '$modalInstance',
                                      	function($scope, $timeout, $rootScope, $modalInstance) {

		$scope.dismiss = function() {
			$modalInstance.dismiss();
		};
		
		$scope.addDaily = function() {
			$scope.dashboard.widgets.push({
			    content: "<div class='box' ng-controller='CustomWidgetCtrl'>"
					+"<div class='box-header'>"
				+"<h3>{{ widget.name }}</h3>"
   				+"<div class='box-header-btns pull-right'>"
					+"<a title='settings' ng-click='openSettings(widget)'><i class='glyphicon glyphicon-cog'></i></a>"
					+"<a title='Remove widget' ng-click='remove(widget)'><i class='glyphicon glyphicon-trash'></i></a>"
				+"</div>"
			+"</div>"
			
			+"<div class='box-content'>"
		
			      +"<table st-table='dailycontent' st-safe-src='rowCollection' class='table table-striped'>"+
			  	"<thead>"+
				"<tr>"+
					"<th st-sort='projectId'>Project<i class='fa fa-caret-down'></i></th>"+
					"<th st-sort='sprintName'>SprintName<i class='fa fa-caret-down'></i></th>"+
					 "<th st-sort='type'>Type<i class='fa fa-caret-down'></i></th>"+
					 "<th st-sort='owner'>Owner<i class='fa fa-caret-down'></i></th>"+
					 "<th st-sort='status'>Status<i class='fa fa-caret-down'></i></th>"+	  
//			         "<th st-sort='version'>Version<i class='fa fa-caret-down'></i></th>"+
					
			         "<th st-sort='lastUpdate'>LastUpdate<i class='fa fa-caret-down'></i></th>"+
			         "<th>Url</th>"+
					"<th>Description</th>"+
				 "</tr>"+
				 "<tr>"+
					"<th colspan='5'>"+
						"<input st-search placeholder='global search...' class='input-sm form-control'>"+
									 
					"</th>"+
				"</tr>"+
				"</thead>"+
				"<tbody>"+
				"<tr ng-repeat='row in dailycontent | orderBy : -lastUpdate' ng-class='rowClass(row)'>"+
					"<td>{{row.projectName}}</td>"+
					"<td>{{row.sprintName}}</td>"+
					 "<td>{{row.type}}</td>"+
					"<td>{{row.executor}}</td>"+
					"<td>{{row.status}}</td>"+       
//			        "<td>{{row.version}}</td>"+
			        "<td>{{row.lastUpdate | date:'dd-MMM-yyyy hh:mm:a'}}</td>"+
			        "<td><button class='btn btn-sm' popover-placement='top' popover='{{row.url}}' type='button'>"+
					"<i class='glyphicon glyphicon-eye-open'></i>"+
				    "</button>"+
				    "<a ng-href='{{row.url}}''>URL</a></td>"+
//				"<td><a ng-href='{{row.url}}'><b>{{row.url}}</b></a></td>"+						
			    "<td>{{row.description}}</td>"+
				"</tr>"+
				"</tbody>"+
				"<tfoot>"+
				"<tr>"+
					"<td colspan='5' class='text-center'>"+
						"<div st-pagination='' st-items-by-page='itemsByPage' st-displayed-pages='5'></div>"+
					"</td>"+
				"</tr>"+
			   "</tfoot>"+
			"</table>"
			 
			+"</div>"		
		+"</div>",
				name: "Daily Report",
				sizeX: 8,
				sizeY: 5,
				
			});
			$scope.saveDashboard();
			
		};
		
		$scope.addUserActivities = function() {
			$scope.dashboard.widgets.push({
			 content: "<div class='box' ng-controller='CustomWidgetCtrl'>"
					+"<div class='box-header'>"
				+"<h3>{{ widget.name }}</h3>"
   				+"<div class='box-header-btns pull-right'>"
					+"<a title='settings' ng-click='openSettings(widget)'><i class='glyphicon glyphicon-cog'></i></a>"
					+"<a title='Remove widget' ng-click='remove(widget)'><i class='glyphicon glyphicon-trash'></i></a>"
				+"</div>"
			+"</div>"
			+"<div class='box-content'>"
		      +"<canvas id='userActivities' class='chart chart-bar'	"+
		    	  "chart-data='dataUserActivities' chart-labels='labelsUserActivities' chart-series='seriesUserActivities' chart-options='optionUserActivities' chart-click='onClickBar'>"+
		    		  "</canvas>"
		+"</div>"
			
		+"</div>",
				name: "User Activities",
			    sizeX: 2,
				sizeY: 2,
			});
		$scope.saveDashboard();
		
		 
		};
		 
		$scope.addChangesTrendBar = function() {
			$scope.dashboard.widgets.push({
			 content: "<div class='box' ng-controller='CustomWidgetCtrl'>"
					+"<div class='box-header'>"
				+"<h3>{{ widget.name }}</h3>"
   				+"<div class='box-header-btns pull-right'>"
					+"<a title='settings' ng-click='openSettings(widget)'><i class='glyphicon glyphicon-cog'></i></a>"
					+"<a title='Remove widget' ng-click='remove(widget)'><i class='glyphicon glyphicon-trash'></i></a>"
				+"</div>"
			+"</div>"
			+"<div class='box-content'>"
		      +"<canvas id='changesTrendBar' class='chart chart-bar'"+
		    	  "chart-data='dataChangesTrend' chart-labels='labelsChangesTrend' chart-series='seriesChangesTrend' chart-options='optionChangesTrend'>"+
		    		  "</canvas>"
		    		 
		      +"</div>"
			
		      +"</div>",
				name: "Changes Trend Bar",
    			sizeX: 3,
				sizeY: 2,
			});
			$scope.saveDashboard();
			 
		};
		$scope.addChangesTrendLine = function() {
			$scope.dashboard.widgets.push({
			 content: "<div class='box' ng-controller='CustomWidgetCtrl'>"
					+"<div class='box-header'>"
				+"<h3>{{ widget.name }}</h3>"
   				+"<div class='box-header-btns pull-right'>"
					+"<a title='settings' ng-click='openSettings(widget)'><i class='glyphicon glyphicon-cog'></i></a>"
					+"<a title='Remove widget' ng-click='remove(widget)'><i class='glyphicon glyphicon-trash'></i></a>"
				+"</div>"
			+"</div>"
			+"<div class='box-content'>"
		      		    +"<canvas id='changesTrendLine' class='chart chart-line'"+
			    	  "chart-data='dataChangesTrend' chart-labels='labelsChangesTrend' chart-series='seriesChangesTrend' chart-options='optionChangesTrend'>"+
			    		  "</canvas>"
		+"</div>"
			
		+"</div>",
				name: "Changes Trend Line",
    			sizeX: 3,
				sizeY: 2,
			});
			$scope.saveDashboard();
			 
		};
		$scope.addGaugeStatus = function() {
			$scope.dashboard.widgets.push({
			 content: "<div class='box' ng-controller='CustomWidgetCtrl'>"
					+"<div class='box-header'>"
				+"<h3>{{ widget.name }}</h3>"
   				+"<div class='box-header-btns pull-right'>"
					+"<a title='settings' ng-click='openSettings(widget)'><i class='glyphicon glyphicon-cog'></i></a>"
					+"<a title='Remove widget' ng-click='remove(widget)'><i class='glyphicon glyphicon-trash'></i></a>"
				+"</div>"
			+"</div>"
			+"<div class='box-content'>"
			+"<div google-chart chart='chartObject' style='height:100%; width:100%;'></div>"
		   +"</div>"
			
		+"</div>",
				name: "Sprint Status By Gauge",
				sizeX: 2,
		     	sizeY: 2,
			});
			 $scope.saveDashboard();
			 
		};
		$scope.addAlerts = function() {
			$scope.dashboard.widgets.push({
			 content: "<div class='box' ng-controller='CustomWidgetCtrl'>"
					+"<div class='box-header'>"
					+"<h3>{{ widget.name }}</h3>"
	   				+"<div class='box-header-btns pull-right'>"
						+"<a title='settings' ng-click='openSettings(widget)'><i class='glyphicon glyphicon-cog'></i></a>"
						+"<a title='Remove widget' ng-click='remove(widget)'><i class='glyphicon glyphicon-trash'></i></a>"
					+"</div>"
				+"</div>"
				+"<div class='box-content'>"
			
				   +"<table st-table='alertscontent' st-safe-src='rowCollectionAlerts' class='table table-striped'>"+
				  	"<thead>"+
					"<tr>"+
						
						"<th st-sort='sprintName'>SprintName<i class='fa fa-caret-down'></i></th>"+
						 "<th st-sort='messagee'>Message<i class='fa fa-caret-down'></i></th>"+
						
					 "</tr>"+
					 "<tr>"+
						"<th colspan='2'>"+
							"<input st-search placeholder='global search...' class='input-sm form-control'>"+
										 
						"</th>"+
					"</tr>"+
					"</thead>"+
					"<tbody>"+
					"<tr ng-repeat='row in alertscontent' ng-class='rowClassAlerts()'>"+
						"<td>{{row.sprintName}}</td>"+
						"<td>{{row.message}}</td>"+
						
					"</tr>"+
					"</tbody>"+
					"<tfoot>"+
					"<tr>"+
						"<td colspan='2' class='text-center'>"+
							"<div st-pagination='' st-items-by-page='itemsByPage' st-displayed-pages='7'></div>"+
						"</td>"+
					"</tr>"+
				   "</tfoot>"+
				"</table>"
				
				
				
				
			   +"</div>"
				
			+"</div>",
				name: "Alerts",
				sizeX: 3,
				sizeY: 2,
			});
			 
			 $scope.saveDashboard();
			 
		};
		
	
}]).directive('widgetBody', ['$compile',
  function($compile) {
    return {
     restrict: 'E',
      link: function(scope, element, attrs) {
        // create a new angular element from the resource in the
        // inherited scope object so it can compile the element 
        // the item element represents the custom widgets
       var newEl = angular.element(scope.widget.content);
    	
        // using jQuery after new element creation, to append element
        element.append(newEl);
        // returns a function that is looking for scope
        // use angular compile service to instanitate a new widget element
        $compile(newEl)(scope);


      }

    }

  }
])
.directive('stRatio',function(){
        return {
          link:function(scope, element, attr){
            var ratio=+(attr.stRatio);
            
            element.css('width',ratio+'%');
            
          }
        };
    })
.directive('a', function() {
    return {
        restrict: 'E',
        link: function(scope, elem, attrs) {
            if(attrs.ngClick || attrs.href === '' || attrs.href === '#'){
                elem.on('click', function(e){
                    e.preventDefault();
                });
            }
        }
   };
})
.filter('object2Array', function() {
	return function(input) {
		var out = [];
		for (i in input) {
			out.push(input[i]);
		}
		return out;
	}
});