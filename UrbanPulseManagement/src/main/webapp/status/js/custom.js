var statusPageApp = angular.module("statusPageApp", ['pascalprecht.translate']);
statusPageApp.factory("statusPageService", function ($http) {
    return {
        getStatusPageData: function () {
            return $http({
                url: '../api/kpi',
                method: 'GET',
                transformResponse: [function (data) {
                        return data;
                    }]
            });
        }
    };
});

statusPageApp.config(['$translateProvider', function ($translateProvider) {
        $translateProvider.useStaticFilesLoader({
            prefix: 'locale/',
            suffix: '.json'
        });
        $translateProvider.preferredLanguage('en_US');
    }]);

statusPageApp.controller("StatusPageController", ["$scope", "$interval", "statusPageService", "$translate", function ($scope, $interval, statusPageService, $translate) {
        $scope.displayData = {};
        $scope.selectedModuleType;
        $scope.lastUpdate;
        $scope.supportedLanguages = [
            {
                key: "en_US",
                name: "English"
            },
            {
                key: "de_DE",
                name: "Deutsch"
            }
        ];

        var state = {
            GREEN: "success",
            YELLOW: "warning",
            RED: "danger",
            GRAY: "light"
        };

        $scope.setSelectedModuleType = function (type) {
            if (type !== $scope.selectedModuleType) {
                $scope.selectedModuleType = type;
            }
        };

        var setError = function (message, options) {
            if (angular.isUndefined($scope.errors)) {
                $scope.errors = [];
            }
            $scope.errors.push({
                message: message,
                parameters: options
            });

        };

        var isCountGreaterZero = function (item) {
            return item.count > 0;
        };

        $scope.updateStatusPageData = function (manualTrigger) {
            statusPageService.getStatusPageData().then(
                    function (success) {
                        $scope.errors = undefined;

                        $scope.statusData = JSON.parse(success.data);
                        //insert empty array because if non-existing, there might be errors otherwise
                        $scope.statusData.availableSetups = $scope.statusData.availableSetups.filter(isCountGreaterZero);

                        $scope.displayData = fillDisplayData($scope.statusData);

                        if (manualTrigger === true) {  //restart 60 second timer if triggered by button
                            $interval.cancel(updateTimer);
                            updateTimer = $interval($scope.updateStatusPageData, 60000);
                        }
                    }, function (error) {
                setError("ERROR_UNKNOWN", {message: error.message});
            });
        };

        var checkForErrors = function (module) {
            switch (module.moduleState) {
                case ("UNSTABLE"):
                    setError("ERROR_MODULE_UNSTABLE", {type: module.moduleType, id: module.moduleId});
                    return;
                case ("UNHEALTHY"):
                    setError("ERROR_MODULE_UNHEALTHY", {type: module.moduleType, id: module.moduleId});
                    return;
                case ("UNKNOWN"):
                    setError("ERROR_MODULE_UNKNOWN", {type: module.moduleType, id: module.moduleId});
                    return;
            }
        };

        var fillDisplayData = function (statusData) {
            var summary = {};
            for (var index in statusData.availableSetups) {
                summary[$scope.statusData.availableSetups[index].name] = {
                    instances: [],
                    sumState: state.GRAY
                };
            }
            statusData.registeredModules.forEach(function (registeredModule) {
                let type = registeredModule.moduleType;
                if (angular.isUndefined(summary[type])) {
                    // TODO: What else??
                    setError("ERROR_NO_CONFIG_FOR_TYPE", {type: type});
                } else {
                    checkForErrors(registeredModule);
                    summary[type].instances.push(registeredModule);
                }
            });

            // determine type status
            for (var moduleIndex in summary) {
                let instances = summary[moduleIndex].instances;
                if (instances.some(module => module.moduleState === "UNHEALTHY")) {
                    summary[moduleIndex].sumState = state.RED;
                } else if (instances.some(module => module.moduleState === "UNSTABLE")) {
                    summary[moduleIndex].sumState = state.YELLOW;
                } else if (instances.some(module => module.moduleState === "UNKNOWN")) {
                    summary[moduleIndex].sumState = state.YELLOW;
                } else {
                    var availableModuleSetupsCount = getAvailableSetupsOfModule(moduleIndex).count;
                    var registeredModuleCount = summary[moduleIndex].instances.length || 0;
                    if (registeredModuleCount === availableModuleSetupsCount) {
                        summary[moduleIndex].sumState = state.GREEN;
                    } else if (registeredModuleCount > 0) {
                        setError("ERROR_NOT_EQUALS_SETUPS", {type: moduleIndex});
                        summary[moduleIndex].sumState = state.YELLOW;
                    } else {
                        setError("ERROR_NO_MODULE_OF_TYPE", {type: moduleIndex});
                        summary[moduleIndex].sumState = state.RED;
                    }
                }
            }

            return summary;
        }

        var getAvailableSetupsOfModule = function (type) {
            for (var i in $scope.statusData.availableSetups) {
                if ($scope.statusData.availableSetups[i].name === type) {
                    return $scope.statusData.availableSetups[i];
                }
            }
            return {
                name: type,
                count: 0
            };
        };

        $scope.getModuleState = function (moduleState) {
            switch (moduleState) {
                case ("HEALTHY"):
                    return state.GREEN;
                case ("UNSTABLE"):
                    return state.YELLOW;
                case ("UNHEALTHY"):
                    return state.RED;
                case ("UNKNOWN"):
                    return state.GRAY;
            }
        };

        $scope.getModuleListStyle = function (moduleState) {
            return 'list-group-item-' + moduleState;
        };

        $scope.getMissingModuleCount = function (type) {
            var count = Math.max(0, getAvailableSetupsOfModule(type).count - $scope.displayData[type].instances.length);
            return new Array(count);
        };

        $scope.getPrettyPrinted = function (object) {
            return angular.toJson(object, true);
        };

        $scope.setLanguage = function (language) {
            $translate.use(language).then(function (key) {

            }, function (key) {
                console.error("language " + key + "not supported.");
            });
        };

        var setTimeDiff = function () {
            if (angular.isDefined($scope.statusData)) {
                $scope.lastUpdate = Math.floor((Date.now() - Date.parse($scope.statusData.timestamp)) / 1000);
            }
        };

        // update page every 1  minute
        var updateTimer = $interval($scope.updateStatusPageData, 60000);
        $scope.updateStatusPageData();
        $interval(setTimeDiff, 1000);
    }]);