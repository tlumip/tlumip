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
package com.pb.tlumip.pt.survey;

public class TestPattern{
	public static void main(String[] args){

		Pattern testPattern = new Pattern("hrchohshsshorhsoh");
		System.out.println("Test word                  "+testPattern.dayPattern.toString());
		System.out.println("Home activities            "+testPattern.homeActivities);
		System.out.println("Work activities            "+testPattern.workActivities);
		System.out.println("School activities          "+testPattern.schoolActivities);
		System.out.println("Shop activities            "+testPattern.shopActivities);
		System.out.println("Soc-Rec activities         "+testPattern.recreateActivities);
		System.out.println("Other activities           "+testPattern.otherActivities);
		System.out.println("WorkBased Tours            "+testPattern.workBasedTours);
		System.out.println("Tour 1 Intermediate stops  "+testPattern.tour1IStops);
		System.out.println("Tour 2 Intermediate stops  "+testPattern.tour2IStops);
		System.out.println("Tour 3 Intermediate stops  "+testPattern.tour3IStops);
		System.out.println("Tour 4+Intermediate stops  "+testPattern.tour4PIStops);
		System.out.println("The sixth tour word is     "+testPattern.getTourString(6));
	
		System.out.println("Stops on work tours        "+testPattern.stopsOnWorkTours);
		System.out.println("Stops on school tours      "+testPattern.stopsOnSchoolTours);
		System.out.println("Stops on shop tours        "+testPattern.stopsOnShopTours);
		System.out.println("Stops on recreate tours    "+testPattern.stopsOnRecreateTours);
		System.out.println("Stops on other tours       "+testPattern.stopsOnOtherTours);
		System.out.println("WorkPSchool                "+testPattern.workPSchool);
		System.out.println("WorkPShop                  "+testPattern.workPShop);
		System.out.println("WorkPRecreate              "+testPattern.workPRecreate);
		System.out.println("WorkPOther                 "+testPattern.workPOther);
		System.out.println("SchoolPShop                "+testPattern.schoolPShop);
		System.out.println("SchoolPRecreate            "+testPattern.schoolPRecreate);
		System.out.println("SchoolPOther               "+testPattern.schoolPOther);
		System.out.println("ShopPRecreate              "+testPattern.shopPRecreate);
		System.out.println("ShopPOther                 "+testPattern.shopPOther);
		System.out.println("RecreatePOther             "+testPattern.recreatePOther);
		System.out.println("Tour1Purpose               "+testPattern.tour1Purpose);
		System.out.println("Tour2Purpose               "+testPattern.tour2Purpose);
		System.out.println("Tour3Purpose               "+testPattern.tour3Purpose);
		System.out.println("Tour4Purpose               "+testPattern.tour4Purpose);

	} 		
}
