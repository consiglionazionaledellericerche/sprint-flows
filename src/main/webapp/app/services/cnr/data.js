(function() {
	'use strict';

	angular.module('sprintApp')
	.factory('dataService', Data);


	Data.$inject = ['$http'];

	function Data ($http) {

		return {

			authentication: {
				impersonate: function(username) {
					return $http.get("impersonate/start?impersonate_username="+ username);
				},
				cancelImpersonate: function() {
					return $http.get("impersonate/exit");
				}
			},
			tasks: {
				myTasks : function(processDefinition, firstResult, maxResults, order, params) {
					return $http.post('api/tasks/mytasks?processDefinition=' + (processDefinition ? processDefinition.key : 'all') +
							'&firstResult=' + firstResult +
							'&maxResults=' + maxResults +
							'&order=' + order, params);
				},
				myTasksAvailable : function(processDefinition, firstResult, maxResults, order, params) {
					return $http.post('api/tasks/availabletasks?processDefinition=' + (processDefinition ? processDefinition.key : 'all') +
							'&firstResult=' + firstResult +
							'&maxResults=' + maxResults +
							'&order=' + order, params);
				},
				complete : function(data) {
					return $http.post('api/tasks/complete', data);
				},
				claim: function (id, take) {
					return $http({
						url: 'api/tasks/claim/'+ id,
						method: take ? 'PUT' : 'DELETE'
					});
				},
				getTask: function (id) {
					return $http.get('api/tasks/'+ id);
				},
				getTaskCompletedByMe: function (processDefinition, firstResult, maxResults, order, params) {
					return $http.post('api/tasks/taskCompletedByMe?processDefinition=' + (processDefinition ? processDefinition.key : 'all') +
							'&firstResult=' + firstResult +
							'&maxResults=' + maxResults +
							'&order=' + order, params);
				},
				taskAssignedInMyGroups: function (processDefinition, firstResult, maxResults, order, params) {
					return $http.post('api/tasks/taskAssignedInMyGroups?processDefinition=' + (processDefinition ? processDefinition.key : 'all') +
							'&firstResult=' + firstResult +
							'&maxResults=' + maxResults +
							'&order=' + order, params);
				},
				getAttachments: function(taskId) {
					return $http.get('api/attachments/task/'+ taskId);
				},
                search: function (params) {					
                    return $http.post('api/tasks/search/', params);
                },
                exportCsv: function (processInstance, active, params, order, firstResult, maxResults) {
                    var processInstanceId;
                    if (processInstance !== undefined) {
                        processInstanceId = processInstance.key;
                    } else {
                        processInstanceId = 'all';
                    }

                    return $http.post('api/tasks/exportCsv/' + processInstanceId +
                        '?active=' + active +
                        '&order=' + order +
                        '&firstResult=' + firstResult +
                        '&maxResults=' + maxResults, params);
                }
			},
			processInstances: {
				byProcessInstanceId : function(processInstanceId) {
					return $http.get('api/processInstances?processInstanceId=' + processInstanceId);
				},
				myProcessInstances:  function(active, processDefinition, order, firstResult, maxResults) {
					return $http.get('api/processInstances/myProcessInstances?active=' + active +
							'&processDefinition=' + (processDefinition ? processDefinition.key : 'all') +
							'&order=' + order +
							'&firstResult=' + firstResult +
							'&maxResults=' + maxResults);
				},
				getProcessInstances: function(processDefinition, active, firstResult, maxResults, order, params) {
					return $http.post('api/processInstances/getProcessInstances?active=' + active +
							'&processDefinition=' + (processDefinition ? processDefinition.key : 'all') +
							'&order=' + order +
							'&firstResult=' + firstResult +
							'&maxResults=' + maxResults, params);
				},
				getAttachments: function(processInstanceId) {
					return $http.get('api/attachments/'+ processInstanceId);
				},
				attachmentHistory: function(processInstaceId, attachmentName) {
					return $http.get('api/attachments/history/'+ processInstaceId +'/'+ attachmentName);
				},
				search: function (params) {
					return $http.post('api/search/', params);
				},
				exportSummary: function (processInstaceId) {
					return $http.get('api/summaryPdf?processInstanceId='+ processInstaceId);
				},
				exportCsv: function (processInstance, active, params, order, firstResult, maxResults) {
					var processInstanceId;
					if(processInstance !== undefined){
						processInstanceId = processInstance.key;
					} else {
						processInstanceId = 'all';
					}

					return $http.post('api/processInstances/exportCsv/' + processInstanceId +
							'?active=' + active +
							'&order=' + order +
							'&firstResult=' + firstResult +
							'&maxResults=' + maxResults, params);
				},
				setVariable: function(processInstanceId, variableName, value) {
					return $http.post('api/processInstances/variable'+
							'?processInstanceId='+ processInstanceId +
							'&variableName='+ variableName +
							'&value='+ value);
				}
			},
			attachments: {
				pubblicaDocumento: function(processInstanceId, attachmentName, flag) {
					return $http.post('/api/attachments/'+ processInstanceId +'/'+ attachmentName +'/pubblica?pubblica='+ flag);
				}
			},
			definitions : {
				all : function() {
					return $http.get('api/processDefinitions/all');
				},
				get: function(id) {
					return $http.get('api/processDefinitions/', id);
				}
			},
			dynamiclist : {
				byName: function(name) {
					return $http.get('api/dynamiclists/byname/'+ name);
				}
			},
			sigladynamiclist : {
				byName: function(name) {
					return $http.get('api/sigladynamiclist/byname/'+ name);
				}
			},
			view: function(processid, type) {
				return $http.get('api/views/'+ processid +'/'+ type);
			},
			search:{
				users: function(filter) {
					return $http.get('api/users/'+ filter +"/search");
				},
				uo: function(filter) {
					return $http.get('api/users/struttura/'+ filter +"/search");
				}
			},
			mail: {
				isActive: function() {
					return $http.get('api/mail/active');
				},
				setActive: function(active) {
					return $http.post('api/mail/active?active='+ active)
				},
				getUsers: function() {
					return $http.get('api/mail/users');
				},
				setUsers: function(users) {
					return $http.post('api/mail/users?users='+ users)
				}
			}
		};
	}
})();