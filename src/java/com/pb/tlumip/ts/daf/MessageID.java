/*
 * Copyright  2005 PB Consult Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.pb.tlumip.ts.dafv3;

import java.io.Serializable;

/**
 * @author Hicks
 *
 * Message ID information
 */
public final class MessageID implements Serializable {

	
	public static final String TS_SOLVE_FW_ID = "SolveFwId";
	public static final String TS_PROPERTYMAP_KEY = "TSPropertyMapKey";
	public static final String NODE_KEY = "NodeKey";
	public static final String NETWORK_ID = "NetworkId";
	public static final String NETWORK_KEY = "NetworkKey";
	public static final String TRIPTABLE_KEY = "TripTableKey";
	public static final String FIRST_TAZ_NUMBER_KEY = "FirstTazNumber";
	public static final String LAST_TAZ_NUMBER_KEY = "LastTazNumber";
	public static final String MAX_FW_ITERATION_NUMBER_KEY = "FrankWolfeMaxIterationNumber";
	
	public static final String FW_BUILD_LOAD_START_ID = "StartAonBuildLoadId";
	public static final String FW_ASSIGNMENT_INFORMATION_ID = "FrankWolfeAssignmentInfoId";
	public static final String FW_ITERATION_NUMBER_KEY = "FrankWolfeIterationNumber";
	
	public static final String BUILDLOAD_WORK_ID = "rootOriginTazWorkId";
	public static final String BUILDLOAD_ROOT_ORIGIN_TAZ_KEY = "rootOriginTripsKey";
	public static final String BUILDLOAD_USER_CLASS_KEY = "userClassKey";
	public static final String BUILDLOAD_ROOT_ORIGIN_TRIPTABLE_ROW_KEY = "rootOriginTripsKey";

	public static final String RETURN_AON_LINK_FLOWS_ID = "returnAonLinkFlows";
	public static final String AON_FLOW_RESULTS_ID = "AonFlowResultsiD";
	public static final String AON_FLOW_RESULT_VALUES_KEY = "AonFlowResultValues";
	public static final String FINAL_AON_FLOW_RESULTS_ID = "FinalAonFlowResultsiD";
	public static final String FINAL_AON_FLOW_RESULT_VALUES_KEY = "FinalAonFlowResultValues";
	
	public static final String RESET_WORK_ELEMENTS_COMPLETED_ID = "ResetNumberOfWorkElementsCompletedByWorkerTasksOnNode";
	public static final String NUMBER_OF_WORK_ELEMENTS_COMPLETED_ID = "NumberOfWorkElementsCompletedByWorkerTasksOnNode";
	public static final String NUMBER_OF_WORK_ELEMENTS_COMPLETED_KEY = "NumberOfWorkElementsCompletedByWorkerTasksOnNode";
	public static final String RETURN_WORK_ELEMENTS_COMPLETED_ID = "ReturnNumberOfWorkElementsCompletedByWorkerTasksOnNode";
	
	
	public static final String FW_ALGORITHM_QUEUE = "FWAlgorithmQueue";
	
	public static final String AON_BUILD_LOAD_WORKER_QUEUE = "BuildLoadWorkerQueue"; 

	public static final String AON_BUILD_LOAD_CONTROLLER_QUEUE = "BuildLoadControllerQueue"; 

	public static final String[] AON_BUILD_LOAD_COMMON_QUEUES = { 
		"BuildLoadCommonQueue_1", 
		"BuildLoadCommonQueue_2", 
		"BuildLoadCommonQueue_3", 
		"BuildLoadCommonQueue_4", 
		"BuildLoadCommonQueue_5"
	};
	
	
	
}