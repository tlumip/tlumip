#import libraries
import pandas as pd, numpy as np
import sys, os, subprocess, time, shutil
from functools import reduce
from Properties import Properties
import warnings
warnings.filterwarnings('ignore')

class popsimSPG(object):

	def __init__(self,property_file):
		print("start SPG run - " + time.ctime())
		
		properties = Properties()
		properties.loadPropertyFile(property_file)

		#self.current_year = properties['t.year']
		self.spg1_directory = properties['spg1_directory']
		self.spg2_directory = properties['spg2_directory']
		self.spg1_data_directory = properties['spg1_data_directory']
		self.spg2_data_directory = properties['spg2_data_directory'] 
		self.spg1_configs_directory = properties['spg1_configs_directory']
		self.spg2_configs_directory = properties['spg2_configs_directory']
		self.spg1_output_directory = properties['spg1_output_directory']
		self.spg2_output_directory = properties['spg2_output_directory']
		self.controls_csv_spg1  = properties['controls.csv.spg1'] 
		self.controls_csv_spg2  = properties['controls.csv.spg2'] 
		self.settings_yaml_spg1 = properties['settings.yaml.spg1']
		self.settings_yaml_spg2 = properties['settings.yaml.spg2']
		self.logging_yaml_spg1  = properties['logging.yaml.spg1'] 
		self.logging_yaml_spg2  = properties['logging.yaml.spg2']
		self.popsim_py_file = properties['popsim.py.file']
		self.python_exe = properties['python.executable'].replace('/','\\')
		
		### INPUTS

		self.pums_to_split_industry = properties['pums.to.split.industry.file']
		self.JobsToWorkersFactor = properties['spg1.workers.per.job.factors']
		self.workersPerHouseholdMarginalxYEAR = properties['spg1.workers.per.household.marginals']
		self.activity_forecast = properties['ned.activity_forecast.path']
		self.population_forecast = properties['ned.population_forecast.path']
		self.spg1_synthetic_households_file = properties['spg1.synthetic.households']
		self.spg1_synthetic_persons_file = properties['spg1.synthetic.persons']
		self.spg2_synthetic_households_file = properties['spg2.synthetic.households']
		self.spg2_synthetic_persons_file = properties['spg2.synthetic.persons']
		self.spg2_synthetic_households_file2 = properties['spg2.synthetic.households.2']
		self.spg2_synthetic_persons_file2 = properties['spg2.synthetic.persons.2']
		self.spg2_current_synpop_summary_file = properties['spg2.current.synpop.summary']
		self.laborDollarProduction = properties['spg.labor.dollars.by.zone']
		self.householdsByHHCategory = properties['spg1.hhs.by.hh.category']
		self.ActivityLocations2 = properties['spg.hhs.by.category.by.zone']
		self.puma_beta_alpha_xwalk_file = properties['puma.beta.alpha.xwalk']
		self.acs_occ_file = properties['acs.sw.occupation.correspondence.edited.file.name']
		self.swim_pumas_file = properties['swim.pumas.file']
		self.puma_alpha_unclean_file = properties['puma.alpha.unclean.file']
		self.pseed_or_file = properties['pseed_or']
		self.pseed_wa_file = properties['pseed_wa']
		self.pseed_ca_file = properties['pseed_ca']
		self.hseed_or_file = properties['hseed_or']
		self.hseed_wa_file = properties['hseed_wa']
		self.hseed_ca_file = properties['hseed_ca']


		### OUTPUTS

		self.seed_households_file = properties['seed.households']
		self.seed_persons_file = properties['seed.persons']
		self.hh_dist_file = properties['hh.size.distribution']
		self.spg1_control_subseed_file = properties['spg1.control.subseed']
		self.spg1_control_region_file = properties['spg1.control.region']
		self.spg1_geo_cross_walk_file = properties['spg1.geo.cross.walk']
		self.spg2_control_alpha_file = properties['spg2.control.alpha']
		self.spg2_control_region_file = properties['spg2.control.region']
		self.spg2_geo_cross_walk_file = properties['spg2.geo.cross.walk']

	# The following functions perform the tasks listed below -
		# createDirectories - creates the PopulationSim directories
		# createXWalk - creates the puma beta alpha cross walk for the swim region. DO NOT RUN!
		# createSeed - creates the seed files for running PopulationSim. RUN ONCE ONLY!
		# spg1Controls - creates the SPG1 controls for the model year. RUN FOR ALL YEARS!
		# spg1PostProcess - post process SPG1 outputs for the model year. RUN FOR ALL YEARS!
		# spg2Controls - creates the SPG2 controls for the model year. RUN FOR ALL YEARS!
		# spg2PostProcess - post process the SPG2 outputs for the model year. RUN FOR ALL YEARS!
	
	def createDirectories(self):
		
		if not os.path.exists(self.spg1_directory):
			os.makedirs(self.spg1_directory)
		if not os.path.exists(self.spg1_data_directory):
			os.makedirs(self.spg1_data_directory)
		if not os.path.exists(self.spg1_configs_directory):
			os.makedirs(self.spg1_configs_directory)
		if not os.path.exists(self.spg1_output_directory):
			os.makedirs(self.spg1_output_directory)
		if not os.path.exists(self.spg2_directory):
			os.makedirs(self.spg2_directory)
		if not os.path.exists(self.spg2_data_directory):
			os.makedirs(self.spg2_data_directory)
		if not os.path.exists(self.spg2_configs_directory):
			os.makedirs(self.spg2_configs_directory)
		if not os.path.exists(self.spg2_output_directory):
			os.makedirs(self.spg2_output_directory)
		shutil.copy(self.controls_csv_spg1, self.spg1_configs_directory)
		shutil.copy(self.controls_csv_spg2, self.spg2_configs_directory)
		shutil.copy(self.settings_yaml_spg1, self.spg1_configs_directory)
		shutil.copy(self.settings_yaml_spg2, self.spg2_configs_directory)
		shutil.copy(self.logging_yaml_spg1, self.spg1_configs_directory)
		shutil.copy(self.logging_yaml_spg2, self.spg2_configs_directory)
		shutil.copy(self.popsim_py_file,  self.spg1_directory)
		shutil.copy(self.popsim_py_file,  self.spg2_directory)
		
	def copySeeds(self):
		
		shutil.copy(self.seed_households_file, self.spg1_data_directory)
		shutil.copy(self.seed_persons_file, self.spg1_data_directory)
		shutil.copy(self.seed_households_file, self.spg2_data_directory)
		shutil.copy(self.seed_persons_file, self.spg2_data_directory)

		
	def createXWalk(self):
		"""
		THIS FUNCTION SHOULD NOT BE CALLED FOR RUNNING THE MODEL, IT WAS RUN ONCE AND THE CROSS-WALK HAS BEEN STORED IN THE INPUTS/PARAMETERS DIRECTORY.
		
		this portion of the script cleans the puma_alpha crosswalk shapefile, it identifies the slivers and eliminates them
		resulting in exactly 41 pumas in the swim region, 31 OR, 9 WA and 1 CA pumas. There are 457 beta zones and 2918 alpha zones
		"""
		
		puma_alpha_xwalk = pd.read_excel(self.puma_alpha_unclean_file)

		counts = pd.DataFrame(puma_alpha_xwalk.NO.value_counts()).reset_index()
		counts.columns = ['AZONE', 'Counts']
		counts_mult = counts[counts['Counts'] > 1]
		counts_one = counts[counts['Counts'] == 1]
		mult_puma = pd.merge(puma_alpha_xwalk, counts_mult, left_on = ['NO'], right_on = ['AZONE'], how = 'inner')
		mult_puma = mult_puma[['NO', 'AZONE', 'BZONE', 'PUMACE10', 'SR']]
		mult_puma.set_index('NO')
		maxSR_puma = pd.DataFrame(mult_puma.groupby('NO').agg({'SR': 'min'}))
		maxSR_puma = maxSR_puma.reset_index()
		maxSR_puma = pd.merge(maxSR_puma, mult_puma, on = ['NO', 'SR'], how = 'inner')
		df1 = pd.merge(puma_alpha_xwalk, counts_one[['AZONE']], left_on = 'NO', right_on = 'AZONE', how = 'inner')
		df2 = pd.merge(puma_alpha_xwalk, maxSR_puma[['NO', 'PUMACE10', 'AZONE']], on = ['NO', 'PUMACE10'], how = 'inner')
		df2 = df2.drop_duplicates('NO')
		df  = pd.concat([df1, df2])
		puma_beta_alpha_xwalk = df[['AZONE', 'BZONE', 'PUMACE10', 'COUNTY', 'STATE']]
		swim_pumas = puma_beta_alpha_xwalk[['PUMACE10', 'STATE']].drop_duplicates('PUMACE10')

		swim_pumas.to_csv(self.swim_pumas_file, index=False)
		puma_beta_alpha_xwalk.to_csv(self.puma_beta_alpha_xwalk_file, index=False)

	def createSeed(self):

		"""input household and person data for OR, WA and CA"""
		pseed_or = pd.read_csv(self.pseed_or_file)
		pseed_wa = pd.read_csv(self.pseed_wa_file)
		pseed_ca = pd.read_csv(self.pseed_ca_file)
		hseed_or = pd.read_csv(self.hseed_or_file)
		hseed_wa = pd.read_csv(self.hseed_wa_file)
		hseed_ca = pd.read_csv(self.hseed_ca_file)
		#input file to identify pumas within the SWIM modeling region
		swim_pumas = pd.read_csv(self.swim_pumas_file)
		#input file used to assign split industry to workers in the pums seed file
		split_ind = pd.read_csv(self.pums_to_split_industry)
		#input file to map workers in the seed file to acs occupation categories
		acs_occ = pd.read_csv(self.acs_occ_file)

		#creating PERSONID
		pseed_or['PERSONID'] = pseed_or['SERIALNO']*100 + pseed_or['SPORDER']
		pseed_wa['PERSONID'] = pseed_wa['SERIALNO']*100 + pseed_wa['SPORDER']
		pseed_ca['PERSONID'] = pseed_ca['SERIALNO']*100 + pseed_ca['SPORDER']

		#merging household and person files from 3 states and filtering for the pums region
		pseed_master = pd.concat([pseed_or, pseed_ca, pseed_wa])
		hseed_master = pd.concat([hseed_or, hseed_ca, hseed_wa])
		pseed = pd.merge(pseed_master, swim_pumas, left_on = ['PUMA'], right_on = ['PUMACE10'], how = 'right')
		hseed = pd.merge(hseed_master, swim_pumas, left_on = ['PUMA'], right_on = ['PUMACE10'], how = 'right')

		#SPG1 runs for the entire region, so the seed data must have a unique ID to map to the region. We create a field SEED and set it
		#to 1 for all pumas.
		pseed['SEED'] = 1

		split_ind.columns = ['pums_industry_code', 'pums_occ_code', 'split_industry_id', 'split_industry', 'proportion']
		seed_split = pd.merge(pseed, split_ind, left_on = ['INDP', 'OCCP'], right_on = ['pums_industry_code', 'pums_occ_code'], how = 'left')

		pseed['DRAW'] = np.random.uniform(0,1, size=len(pseed))
		seed_split = pd.merge(seed_split, pseed[['PERSONID', 'DRAW']], on = 'PERSONID', how = 'left')
		seed_split['cumprop'] = seed_split.groupby(['PERSONID'])['proportion'].apply(lambda x: x.cumsum())
		seed_split['prev_cumprop'] = seed_split.groupby(['PERSONID'])['cumprop'].apply(lambda x: x.shift(1))
		seed_split.prev_cumprop.fillna(0, inplace=True)
		seed_split['select'] = np.where((seed_split['DRAW'] < seed_split['cumprop']) & (seed_split['DRAW'] > seed_split['prev_cumprop']), 1, 0)
		seed_split['split_industry_id'].fillna(999, inplace=True)
		seed_split['select2'] = np.where(seed_split['split_industry_id'] == 999, 1, 0)
		seed_split['select'] = seed_split['select'] + seed_split['select2']

		assigned_ind_id = seed_split[seed_split['select'] == 1]
		assigned_ind_id.fillna(99999, inplace=True)
		assigned_ind_id = pd.merge(assigned_ind_id, acs_occ, left_on = ['OCCP'], right_on = ['occupation'], how = 'left')
		assigned_ind_id['occupationLabel'] = np.where(assigned_ind_id['ESR'].isin([3,6,99999]), 'No_Occupation', assigned_ind_id['occupationLabel'])

		hseed = hseed[hseed['NP'] > 0]
		hseed = hseed[hseed['TYPE'] == 1]
		hseed['SEED'] = 1
		hseed['NP_RECODE'] = np.where((hseed['NP'] >= 4), (np.round((hseed.loc[hseed['NP'] >= 4].NP.mean()),2)), hseed['NP'])
		hh_pp_seed = pd.merge(hseed, pseed, on = ['SERIALNO'], how = 'left')
		hh_pp_seed['NWESR'] = np.where(hh_pp_seed['ESR'].isin([1,2,4,5]), 1, 0)
		workers_seed = pd.DataFrame(hh_pp_seed.groupby('SERIALNO')['NWESR'].sum()).reset_index()
		hseed = pd.merge(hseed, workers_seed, on = 'SERIALNO', how = 'inner')
		hseed['hh_id'] = hseed.index + 1

		hseed_dist = pd.DataFrame(np.round((hseed.groupby('NP_RECODE')['WGTP'].sum()),0))
		hseed_dist.columns = ['HH']
		hseed_dist.reset_index(inplace=True)
		hseed_dist['HH_PERCENT'] = np.round(((hseed_dist['HH']*100)/(hseed_dist['HH'].sum())),1)

		adjfac_map = {1061971:1.007549 * 1.05401460, 1045195:1.008425 * 1.03646282, 1035988:1.001264 * 1.03468042, 
					 1029257:1.007588 * 1.02150538, 1011189:1.011189 * 1.00000000}
		hseed['ADJFAC'] = hseed['ADJINC'].map(adjfac_map)
		hseed['HHINC2017'] = np.round((hseed['HINCP']*hseed['ADJFAC']),0)
		hseed['HHINC2009'] = np.round((hseed['HHINC2017']*0.8695),0)

		hseed['hhsize_cat'] = np.where(hseed['NP'] <= 2, '1to2', '3plus')

		hseed['hhinc_cat'] = '999'
		hseed['hhinc_cat'] = np.where(hseed['HHINC2009'] < 8000, '0to8k', hseed['hhinc_cat'])
		hseed['hhinc_cat'] = np.where((hseed['HHINC2009'] >= 8000) & (hseed['HHINC2009'] < 15000) & (hseed['hhinc_cat'] == '999'), '8to15k', hseed['hhinc_cat'])
		hseed['hhinc_cat'] = np.where((hseed['HHINC2009'] >= 15000) & (hseed['HHINC2009'] < 23000) & (hseed['hhinc_cat'] == '999'), '15to23k', hseed['hhinc_cat'])
		hseed['hhinc_cat'] = np.where((hseed['HHINC2009'] >= 23000) & (hseed['HHINC2009'] < 32000) & (hseed['hhinc_cat'] == '999'), '23to32k', hseed['hhinc_cat'])
		hseed['hhinc_cat'] = np.where((hseed['HHINC2009'] >= 32000) & (hseed['HHINC2009'] < 46000) & (hseed['hhinc_cat'] == '999'), '32to46k', hseed['hhinc_cat'])
		hseed['hhinc_cat'] = np.where((hseed['HHINC2009'] >= 46000) & (hseed['HHINC2009'] < 61000) & (hseed['hhinc_cat'] == '999'), '46to61k', hseed['hhinc_cat'])
		hseed['hhinc_cat'] = np.where((hseed['HHINC2009'] >= 61000) & (hseed['HHINC2009'] < 76000) & (hseed['hhinc_cat'] == '999'), '61to76k', hseed['hhinc_cat'])
		hseed['hhinc_cat'] = np.where((hseed['HHINC2009'] >= 76000) & (hseed['HHINC2009'] < 106000) & (hseed['hhinc_cat'] == '999'), '76to106k', hseed['hhinc_cat'])
		hseed['hhinc_cat'] = np.where((hseed['HHINC2009'] >= 106000) & (hseed['hhinc_cat'] == '999'), '106kUp', hseed['hhinc_cat'])

		hseed['Category'] = 'HH' + hseed['hhinc_cat'] + hseed['hhsize_cat']
		
		seed_persons = pd.merge(assigned_ind_id, hseed[['SERIALNO', 'hh_id']], on = 'SERIALNO', how = 'left')

		#outputs
		hseed_dist.to_csv(self.hh_dist_file, index=False)
		seed_persons.to_csv(self.seed_persons_file, index=False)
		hseed.to_csv(self.seed_households_file, index=False)

	def workersPerHouseholdMarginal(self):

		"""
		DO NOT CALL THIS FUNCTION DURING MODEL RUN, THIS FUNCTION USES MANY INPUT FILES AND WRITES ONE STATIC OUTPUT FILE. THIS FUNCTION WAS RUN ONCE AND THE OUTPUT FILE STORED IN THE INPUT/PARAMETERS DIRECTORY.
		KEEPING ALL THESE UNNECESSARY INPUT FILES IN THE MODEL DIRECTORY WILL CONSUME MORE SPACE.
		"""
		
		yr_list = [2010,2011,2012,2013,2014,2015,2016,2017]
		wrk_yr = []

		for year in yr_list:
			if year != 2017:
			
				hh_ca = pd.read_csv('./ACS Data/'+str(year)+'/csv_hca/ss'+str(year-2000)+'hca.csv')
				hh_or = pd.read_csv('./ACS Data/'+str(year)+'/csv_hor/ss'+str(year-2000)+'hor.csv')
				hh_wa = pd.read_csv('./ACS Data/'+str(year)+'/csv_hwa/ss'+str(year-2000)+'hwa.csv')

				pp_ca = pd.read_csv('./ACS Data/'+str(year)+'/csv_pca/ss'+str(year-2000)+'pca.csv')
				pp_or = pd.read_csv('./ACS Data/'+str(year)+'/csv_por/ss'+str(year-2000)+'por.csv')
				pp_wa = pd.read_csv('./ACS Data/'+str(year)+'/csv_pwa/ss'+str(year-2000)+'pwa.csv')
			
			else:
				hh_ca = pd.read_csv('./ACS Data/'+str(year)+'/csv_hca/psam_h06.csv')
				hh_or = pd.read_csv('./ACS Data/'+str(year)+'/csv_hor/psam_h41.csv')
				hh_wa = pd.read_csv('./ACS Data/'+str(year)+'/csv_hwa/psam_h53.csv')

				pp_ca = pd.read_csv('./ACS Data/'+str(year)+'/csv_pca/psam_p06.csv')
				pp_or = pd.read_csv('./ACS Data/'+str(year)+'/csv_por/psam_p41.csv')
				pp_wa = pd.read_csv('./ACS Data/'+str(year)+'/csv_pwa/psam_p53.csv')
			
			hh = pd.concat([hh_ca, hh_or, hh_wa])
			pp = pd.concat([pp_ca, pp_or, pp_wa])
			
			hh_swim = pd.merge(hh, swim_pumas, left_on = ['PUMA'], right_on = ['PUMACE10'], how = 'right')
			pp_swim = pd.merge(pp, swim_pumas, left_on = ['PUMA'], right_on = ['PUMACE10'], how = 'right')
			
			hh_pp = pd.merge(hh_swim, pp_swim, on = ['SERIALNO'], how = 'left')
			
			hh_pp['WORKER'] = np.where(hh_pp['ESR'].isin([1,2,4,5]), 1, 0)
			
			workers = pd.DataFrame(hh_pp.groupby('SERIALNO')['WORKER', 'WGTP'].sum()).reset_index()
			wrkr_dst = pd.DataFrame(workers.groupby('WORKER')['WGTP'].sum()).reset_index()
			wrkr_dst.columns = ['Workers', str(year)+'households']
			
			wrk_yr.append(wrkr_dst)

		master_wrk = reduce(lambda x, y: pd.merge(x, y, on = 'Workers', how = 'outer'), wrk_yr).fillna(0)
		master_wrk['Workers'] = np.where(master_wrk['Workers'] >= 5, np.round((workers.loc[workers['WORKER'] >= 5].WORKER.mean()),1), master_wrk['Workers'])
		master_wrk = master_wrk.groupby('Workers')['2000households', '2011households', '2012households',
			   '2013households', '2014households', '2015households', '2016households',
			   '2017households'].sum()
		master_wrk = master_wrk.reset_index()

		master_wrk['wrkr_2017'] = master_wrk['Workers']*master_wrk['2017households']
		avg_wrkr17 = master_wrk['wrkr_2017'].sum()/master_wrk['2017households'].sum()

		master_wrk.to_csv(self.workersPerHouseholdMarginalxYEAR, index=False)

	def spg1Controls(self):

		jobs_to_wrkr = pd.read_csv(self.JobsToWorkersFactor)
		wrkrs_per_hh = pd.read_csv(self.workersPerHouseholdMarginalxYEAR)
		hseed_dist = pd.read_csv(self.hh_dist_file)
		ned_emp = pd.read_csv(self.activity_forecast)
		ned_pop = pd.read_csv(self.population_forecast)
		hseed = pd.read_csv(self.seed_households_file)
		
		hseed['count'] = 1
		inc_cat = pd.DataFrame(hseed.groupby('hhinc_cat')['count'].sum())
		inc_cat.reset_index(inplace=True)

		emp_to_wrkr = pd.merge(ned_emp, jobs_to_wrkr, left_on = ['activity'], right_on = ['SPG Sector'], how = 'inner')
		emp_to_wrkr['Workers'] = np.round((emp_to_wrkr['employment']*emp_to_wrkr['2010WorkersPerJobFactor']),0)

		#average wrkr is calcualted using the workersPerHouseholdMarginalxYEAR file
		avg_wrkr = (wrkrs_per_hh['Workers']*wrkrs_per_hh['2000households']).sum()/wrkrs_per_hh['2000households'].sum()
		#average hhsize is calculated from hhsize distribution created using PUMS data
		avg_hhsize = ((hseed_dist['NP_RECODE']*hseed_dist['HH']).sum())/(hseed_dist['HH'].sum())

		#total households is calculated by dividing total workers by average workers per household
		total_hh = np.round((emp_to_wrkr.Workers.sum()/avg_wrkr),0)
		#total population is calculated by multiplying total households by the average household size
		total_pp = np.round((total_hh*avg_hhsize),0)
		total_wk = np.round(emp_to_wrkr['Workers'].sum(), 0)
		total_emp = np.round(emp_to_wrkr['employment'].sum(), 0)

		#scaling households, population and employment using totals calculated above
		wrkrs_per_hh['2000households_scaled'] = np.round((((wrkrs_per_hh['2000households'])/(wrkrs_per_hh['2000households'].sum()))*total_hh),0)
		hseed_dist['HH_scaled'] = np.round((((hseed_dist['HH'])/(hseed_dist['HH'].sum()))*total_hh),0)
		ned_pop['population_scaled'] = np.round((((ned_pop['population'])/(ned_pop['population'].sum()))*total_pp),0)
		emp_to_wrkr['workers_scaled'] = np.round((((emp_to_wrkr['Workers'])/(emp_to_wrkr['Workers'].sum()))*total_wk),0)

		control_columns = ['SUBSEED', 'HHS', 'PERS', 'HH_SIZE1', 'HH_SIZE2', 'HH_SIZE3', 'HH_SIZE4M', 'HH_WRKR0', 'HH_WRKR1', 'HH_WRKR2', 'HH_WRKR3', 'HH_WRKR4', 'HH_WRKR5M']
		control_columns.extend(ned_emp['activity'])
		age_vars = ['P_AGE0', 'P_AGE5', 'P_AGE10', 'P_AGE15', 'P_AGE20', 'P_AGE25', 'P_AGE30', 'P_AGE35', 'P_AGE40', 'P_AGE45',
					'P_AGE50', 'P_AGE55', 'P_AGE60', 'P_AGE65', 'P_AGE70', 'P_AGE75', 'P_AGE80', 'P_AGE85']
		control_columns.extend(age_vars)

		control_totals = [1, total_hh, total_pp]
		control_totals.extend(hseed_dist['HH_scaled'])
		control_totals.extend(wrkrs_per_hh['2000households_scaled'])
		control_totals.extend(emp_to_wrkr['Workers'])
		control_totals.extend(ned_pop['population_scaled'])

		spg1_control_subseed = pd.DataFrame(control_totals).T
		spg1_control_subseed.columns = control_columns

		#After scaling, there are small rounding differences which is assigned to the maximum category. the following for loops do
		#the rounding for hhsize, hhwrkr, and pAge
		spg1_control_subseed['siz_diff'] = (spg1_control_subseed['HHS']) - (spg1_control_subseed['HH_SIZE1'] + spg1_control_subseed['HH_SIZE2'] + spg1_control_subseed['HH_SIZE3'] + spg1_control_subseed['HH_SIZE4M'])
		spg1_control_subseed['siz_max'] = spg1_control_subseed[['HH_SIZE1', 'HH_SIZE2', 'HH_SIZE3', 'HH_SIZE4M']].max(axis=1)

		for index, row in spg1_control_subseed.iterrows():
			siz1 = row['HH_SIZE1']
			siz2 = row['HH_SIZE2']
			siz3 = row['HH_SIZE3']
			siz4 = row['HH_SIZE4M']
			sizd = row['siz_diff']
			sizm = row['siz_max']
			
			hhs = row['HHS']
			
			if (siz1) == (sizm):
				spg1_control_subseed.set_value(index, 'HH_SIZE1', siz1 + sizd)
				sizd = sizd - sizd
				sizm = sizm + sizd
			elif (siz2) == (sizm):
				spg1_control_subseed.set_value(index, 'HH_SIZE2', siz2 + sizd)
				sizd = sizd - sizd
				sizm = sizm + sizd
			elif (siz3) == (sizm):
				spg1_control_subseed.set_value(index, 'HH_SIZE3', siz3 + sizd)
				sizd = sizd - sizd
				sizm = sizm + sizd
			elif (siz4) == (sizm):
				spg1_control_subseed.set_value(index, 'HH_SIZE4M', siz4 + sizd)
				sizd = sizd - sizd
				sizm = sizm + sizd
				
		spg1_control_subseed['WRKR_diff'] = (spg1_control_subseed['HHS']) - (spg1_control_subseed['HH_WRKR0'] + spg1_control_subseed['HH_WRKR1'] + spg1_control_subseed['HH_WRKR2'] + spg1_control_subseed['HH_WRKR3'] + spg1_control_subseed['HH_WRKR4'] + spg1_control_subseed['HH_WRKR5M'])
		spg1_control_subseed['WRKR_max'] = spg1_control_subseed[['HH_WRKR0', 'HH_WRKR1', 'HH_WRKR2', 'HH_WRKR3', 'HH_WRKR4', 'HH_WRKR5M']].max(axis=1)

		for index, row in spg1_control_subseed.iterrows():
			WRKR0 = row['HH_WRKR0']
			WRKR1 = row['HH_WRKR1']
			WRKR2 = row['HH_WRKR2']
			WRKR3 = row['HH_WRKR3']
			WRKR4 = row['HH_WRKR4']
			WRKR5 = row['HH_WRKR5M']
			WRKRd = row['WRKR_diff']
			WRKRm = row['WRKR_max']
			
			#hhs = row['HHS']
			
			if (WRKR0) == (WRKRm):
				spg1_control_subseed.set_value(index, 'HH_WRKR0', WRKR0 + WRKRd)
				WRKRd = WRKRd - WRKRd
				WRKRm = WRKRm + WRKRd
			elif (WRKR1) == (WRKRm):
				spg1_control_subseed.set_value(index, 'HH_WRKR1', WRKR1 + WRKRd)
				WRKRd = WRKRd - WRKRd
				WRKRm = WRKRm + WRKRd
			elif (WRKR2) == (WRKRm):
				spg1_control_subseed.set_value(index, 'HH_WRKR2', WRKR2 + WRKRd)
				WRKRd = WRKRd - WRKRd
				WRKRm = WRKRm + WRKRd
			elif (WRKR3) == (WRKRm):
				spg1_control_subseed.set_value(index, 'HH_WRKR3', WRKR3 + WRKRd)
				WRKRd = WRKRd - WRKRd
				WRKRm = WRKRm + WRKRd
			elif (WRKR4) == (WRKRm):
				spg1_control_subseed.set_value(index, 'HH_WRKR4', WRKR4 + WRKRd)
				WRKRd = WRKRd - WRKRd
				WRKRm = WRKRm + WRKRd
			elif (WRKR5) == (WRKRm):
				spg1_control_subseed.set_value(index, 'HH_WRKR5M', WRKR5 + WRKRd)
				WRKRd = WRKRd - WRKRd
				WRKRm = WRKRm + WRKRd
				
		spg1_control_subseed['AGE_diff'] = (spg1_control_subseed['PERS']) - (spg1_control_subseed['P_AGE0'] + spg1_control_subseed['P_AGE5'] + spg1_control_subseed['P_AGE10'] + 
														 spg1_control_subseed['P_AGE15'] + spg1_control_subseed['P_AGE20'] + spg1_control_subseed['P_AGE25'] + 
														 spg1_control_subseed['P_AGE30'] + spg1_control_subseed['P_AGE35'] + spg1_control_subseed['P_AGE40'] + 
														 spg1_control_subseed['P_AGE45'] + spg1_control_subseed['P_AGE50'] + spg1_control_subseed['P_AGE55'] + 
														 spg1_control_subseed['P_AGE60'] + spg1_control_subseed['P_AGE65'] + spg1_control_subseed['P_AGE70'] + 
														 spg1_control_subseed['P_AGE75'] + spg1_control_subseed['P_AGE80'] + spg1_control_subseed['P_AGE85'])
		spg1_control_subseed['AGE_max'] = spg1_control_subseed[['P_AGE0', 'P_AGE5', 'P_AGE10', 'P_AGE15', 'P_AGE20', 'P_AGE25', 'P_AGE30', 'P_AGE35', 'P_AGE40', 'P_AGE45',
					'P_AGE50', 'P_AGE55', 'P_AGE60', 'P_AGE65', 'P_AGE70', 'P_AGE75', 'P_AGE80', 'P_AGE85']].max(axis=1)

		for index, row in spg1_control_subseed.iterrows():
			AGE0  = row['P_AGE0']
			AGE5  = row['P_AGE5']
			AGE10 = row['P_AGE10']
			AGE15 = row['P_AGE15']
			AGE20 = row['P_AGE20']
			AGE25 = row['P_AGE25']
			AGE30 = row['P_AGE30']
			AGE35 = row['P_AGE35']
			AGE40 = row['P_AGE40']
			AGE45 = row['P_AGE45']
			AGE50 = row['P_AGE50']
			AGE55 = row['P_AGE55']
			AGE60 = row['P_AGE60']
			AGE65 = row['P_AGE65']
			AGE70 = row['P_AGE70']
			AGE75 = row['P_AGE75']
			AGE80 = row['P_AGE80']
			AGE85 = row['P_AGE85']
			AGEd = row['AGE_diff']
			AGEm = row['AGE_max']
			
			pers = row['PERS']
			
			if (AGE0) == (AGEm):
				spg1_control_subseed.set_value(index, 'P_AGE0', AGE0 + AGEd)
				AGEd = AGEd - AGEd
				AGEm = AGEm + AGEd
			elif (AGE5) == (AGEm):
				spg1_control_subseed.set_value(index, 'P_AGE5', AGE5 + AGEd)
				AGEd = AGEd - AGEd
				AGEm = AGEm + AGEd
			elif (AGE10) == (AGEm):
				spg1_control_subseed.set_value(index, 'P_AGE10', AGE10 + AGEd)
				AGEd = AGEd - AGEd
				AGEm = AGEm + AGEd
			elif (AGE15) == (AGEm):
				spg1_control_subseed.set_value(index, 'P_AGE15', AGE15 + AGEd)
				AGEd = AGEd - AGEd
				AGEm = AGEm + AGEd
			elif (AGE20) == (AGEm):
				spg1_control_subseed.set_value(index, 'P_AGE20', AGE20 + AGEd)
				AGEd = AGEd - AGEd
				AGEm = AGEm + AGEd
			elif (AGE25) == (AGEm):
				spg1_control_subseed.set_value(index, 'P_AGE25', AGE25 + AGEd)
				AGEd = AGEd - AGEd
				AGEm = AGEm + AGEd
			elif (AGE30) == (AGEm):
				spg1_control_subseed.set_value(index, 'P_AGE30', AGE30 + AGEd)
				AGEd = AGEd - AGEd
				AGEm = AGEm + AGEd
			elif (AGE35) == (AGEm):
				spg1_control_subseed.set_value(index, 'P_AGE35', AGE35 + AGEd)
				AGEd = AGEd - AGEd
				AGEm = AGEm + AGEd
			elif (AGE40) == (AGEm):
				spg1_control_subseed.set_value(index, 'P_AGE40', AGE40 + AGEd)
				AGEd = AGEd - AGEd
				AGEm = AGEm + AGEd
			elif (AGE45) == (AGEm):
				spg1_control_subseed.set_value(index, 'P_AGE45', AGE45 + AGEd)
				AGEd = AGEd - AGEd
				AGEm = AGEm + AGEd
			elif (AGE50) == (AGEm):
				spg1_control_subseed.set_value(index, 'P_AGE50', AGE50 + AGEd)
				AGEd = AGEd - AGEd
				AGEm = AGEm + AGEd
			elif (AGE55) == (AGEm):
				spg1_control_subseed.set_value(index, 'P_AGE55', AGE55 + AGEd)
				AGEd = AGEd - AGEd
				AGEm = AGEm + AGEd
			elif (AGE60) == (AGEm):
				spg1_control_subseed.set_value(index, 'P_AGE60', AGE60 + AGEd)
				AGEd = AGEd - AGEd
				AGEm = AGEm + AGEd
			elif (AGE65) == (AGEm):
				spg1_control_subseed.set_value(index, 'P_AGE65', AGE65 + AGEd)
				AGEd = AGEd - AGEd
				AGEm = AGEm + AGEd
			elif (AGE70) == (AGEm):
				spg1_control_subseed.set_value(index, 'P_AGE70', AGE70 + AGEd)
				AGEd = AGEd - AGEd
				AGEm = AGEm + AGEd
			elif (AGE75) == (AGEm):
				spg1_control_subseed.set_value(index, 'P_AGE75', AGE75 + AGEd)
				AGEd = AGEd - AGEd
				AGEm = AGEm + AGEd
			elif (AGE80) == (AGEm):
				spg1_control_subseed.set_value(index, 'P_AGE70', AGE70 + AGEd)
				AGEd = AGEd - AGEd
				AGEm = AGEm + AGEd
			elif (AGE85) == (AGEm):
				spg1_control_subseed.set_value(index, 'P_AGE85', AGE85 + AGEd)
				AGEd = AGEd - AGEd
				AGEm = AGEm + AGEd

		spg1_control_subseed = spg1_control_subseed[control_columns]
		spg1_control_region = pd.DataFrame({'REGION': [1], 'POPULATION': [total_pp]})
		spg1_geo_cross_walk = pd.DataFrame({'SUBSEED': [1], 'SEED': [1], 'REGION': [1]})

		spg1_control_subseed = spg1_control_subseed.astype(int)
		spg1_control_region = spg1_control_region.astype(int)

		#outputs: subseed control, regional control and geo cross walk
		spg1_control_subseed.to_csv(self.spg1_control_subseed_file, index=False)
		spg1_control_region.to_csv(self.spg1_control_region_file, index=False)
		spg1_geo_cross_walk.to_csv(self.spg1_geo_cross_walk_file, index=False)
		
	def run_spg1(self):
		cmd = self.python_exe + " " + self.popsim_py_file + " --config " + self.spg1_configs_directory + " --output " + self.spg1_output_directory + " --data " + self.spg1_data_directory
		subprocess.call(os.path.join(self.spg1_directory, cmd))
		
	def spg1PostProcess(self):
		spg1_hh = pd.read_csv(self.spg1_synthetic_households_file)

		spg1_hh['Category'] = 'HH' + spg1_hh['hhinc_cat'] + spg1_hh['hhsize_cat']
		spg1_hh['COUNT'] = 1
		hh_by_hhcategory = pd.DataFrame(spg1_hh.groupby('Category')['COUNT'].sum())
		hh_by_hhcategory = hh_by_hhcategory.reset_index()
		hh_by_hhcategory.columns = ['hhCategory', 'spg1Households']

		hh_by_hhcategory.to_csv(self.householdsByHHCategory, index=False)

	def spg2Controls(self):

		"""input files: SPG1 outputs, AA outputs, puma-alpha crosswalk and spg1 control"""
		spg1_hh = pd.read_csv(self.spg1_synthetic_households_file)
		spg1_pp = pd.read_csv(self.spg1_synthetic_persons_file)
		labor_dollar_prod = pd.read_csv(self.laborDollarProduction)
		hh_by_hhcategory = pd.read_csv(self.householdsByHHCategory)
		act_loc2 = pd.read_csv(self.ActivityLocations2)
		puma_beta_alpha_xwalk = pd.read_csv(self.puma_beta_alpha_xwalk_file)
		spg1_control_region = pd.read_csv(self.spg1_control_subseed_file)

		puma_beta_alpha_xwalk['REGION'] = 1
		puma_beta_alpha_xwalk['SEED'] = 1
		spg2_geo_cross_walk = puma_beta_alpha_xwalk[['AZONE', 'PUMACE10', 'REGION']]
		spg2_geo_cross_walk.columns = ['AZONE', 'PUMA', 'REGION']

		#count workers per household category and occupation from the synthetic persons file, divide total labor dollars per
		#hh category and occupation by the workers calculated above, this gives dollars per worker by hh category and occupation
		#across the region. Dividing alpha zone labor dollars by dollars per worker, number of workers in alpha zone is obtained.
		spg1_hh['Category'] = 'HH' + spg1_hh['hhinc_cat'] + spg1_hh['hhsize_cat']
		spg1_pp = pd.merge(spg1_pp, spg1_hh[['household_id', 'Category']], on = 'household_id', how = 'left')
		spg1_pp['Worker'] = np.where(spg1_pp['ESR'].isin([1,2,4,5]), 1, 0)
		spg1_pp['occupationLabel'] = np.where(spg1_pp['ESR'].isin([3,6,99999]), 'No_Occupation', spg1_pp['occupationLabel'])
		workers_occ = spg1_pp.pivot_table(index=['occupationLabel'], columns = ['Category'], values = 'Worker', aggfunc = 'sum')
		workers_occ.drop(labels=['No_Occupation'], axis=0, inplace=True)
		labor_dollar = labor_dollar_prod.groupby('occupation')[hh_by_hhcategory['hhCategory'].tolist()].sum()
		percap_dollar = labor_dollar/workers_occ
		percap_dollar = percap_dollar.stack().reset_index()
		percap_dollar.columns = ['occupation', 'hhCategory', 'percap_dollar']
		total_dollars = labor_dollar_prod.set_index(['zoneNumber', 'occupation']).stack().reset_index()
		total_dollars.columns = ['zoneNumber', 'occupation', 'hhCategory', 'total_dollars']
		total_dollars = pd.merge(total_dollars, percap_dollar, on = ['occupation', 'hhCategory'], how = 'left')
		total_dollars['Workers'] = total_dollars['total_dollars']/total_dollars['percap_dollar']
		total_dollars.fillna(0, inplace=True)
		wk_list = total_dollars['Workers'].tolist()

		#Bucket rounding workers count to match total workers

		rv = [np.round((total_dollars.at[0, 'Workers']),0)]
		res = total_dollars.at[0, 'Workers'] - np.round((total_dollars.at[0, 'Workers']),0)

		for i in wk_list[1:]:
			i = i + res
			ri = np.round(i, 0)
			res = ri - i
			rv.append(ri)

		total_dollars['Workers'] = rv

		difference = (total_dollars.Workers.sum() - workers_occ.sum().sum()).astype(int)
		if (difference > 0):
			total_dollars.sort_values('Workers', ascending=False, inplace=True)
			total_dollars[:difference]['Workers'] = total_dollars[:difference]['Workers'] - 1
		elif (difference < 0):
			total_dollars.sort_values('Workers', ascending=False, inplace=True)
			total_dollars[:abs(difference)]['Workers'] = total_dollars[:abs(difference)]['Workers'] + 1
		else:
			pass

		workers_by_alpha = total_dollars.pivot_table(index=['zoneNumber'], columns = ['occupation'], values = 'Workers', aggfunc = 'sum')
		workers_by_alpha.reset_index(inplace=True)
		workers_by_alpha.rename(columns={workers_by_alpha.columns[0]: "AZONE"}, inplace=True)

		act_loc2.columns = ['hhCategory', 'zoneNumber', 'Households']
		act_loc2 = act_loc2.loc[act_loc2['hhCategory'].isin(hh_by_hhcategory['hhCategory'].tolist())]
		act_loc2.reset_index(drop=True, inplace=True)
		act_loc2.index
		
		#Rouding float number of households from AA to create controls
		#act_loc2['Households'] = act_loc2['Households'].astype(int)
		hh_list = act_loc2['Households'].tolist()
		av = [np.round((act_loc2.at[0, 'Households']),0)]
		aes = act_loc2.at[0, 'Households'] - np.round((act_loc2.at[0, 'Households']),0)

		for i in hh_list[1:]:
			i = i + aes
			ai = np.round(i, 0)
			aes = ai - i
			av.append(ai)

		act_loc2['Households'] = av

		total_alpha_hh = np.round((act_loc2['Households'].sum()),0)
		alpha_hhcat = act_loc2.pivot_table(index = ['zoneNumber'], columns = ['hhCategory'])
		alpha_hhcat.reset_index(inplace=True)
		alpha_hhcat.columns = alpha_hhcat.columns.droplevel()
		alpha_hhcat.rename(columns={alpha_hhcat.columns[0]: "AZONE"}, inplace=True)
		spg2_control_alpha = pd.merge(alpha_hhcat, workers_by_alpha, on = ['AZONE'], how = 'inner')
		spg2_control_alpha['HHS'] = spg2_control_alpha[hh_by_hhcategory['hhCategory']].sum(axis=1)
		
		#Renaming the first column of SPG1 regional control file and using it as the SPG2 regional control. Regional controls are the same for both SPG1 and SPG2.
		spg1_control_region.rename(columns={spg1_control_region.columns[0]: "REGION"}, inplace=True)

		#outputs: alpha zone control, regional control and geo cross walk
		spg2_control_alpha.to_csv(self.spg2_control_alpha_file, index=False)
		spg1_control_region.to_csv(self.spg2_control_region_file, index=False)
		spg2_geo_cross_walk.to_csv(self.spg2_geo_cross_walk_file, index=False)
	
	def run_spg2(self):
		cmd = self.python_exe + " " + self.popsim_py_file + " --config " + self.spg2_configs_directory + " --output " + self.spg2_output_directory + " --data " + self.spg2_data_directory
		subprocess.call(os.path.join(self.spg2_directory, cmd))

	def spg2PostProcess(self):
		spg2_synthetic_households = pd.read_csv(self.spg2_synthetic_households_file)
		spg2_synthetic_persons = pd.read_csv(self.spg2_synthetic_persons_file)
		
		hh = spg2_synthetic_households
		pp = spg2_synthetic_persons
		azones = pd.read_csv(self.puma_beta_alpha_xwalk_file)
		
		hh['hhsize_cat'] = np.where(hh['NP'] <= 2, '1to2', '3plus')

		hh['hhinc_cat'] = '999'
		hh['hhinc_cat'] = np.where(hh['HHINC2009'] < 8000, '0to8k', hh['hhinc_cat'])
		hh['hhinc_cat'] = np.where((hh['HHINC2009'] >= 8000) & (hh['HHINC2009'] < 15000) & (hh['hhinc_cat'] == '999'), '8to15k', hh['hhinc_cat'])
		hh['hhinc_cat'] = np.where((hh['HHINC2009'] >= 15000) & (hh['HHINC2009'] < 23000) & (hh['hhinc_cat'] == '999'), '15to23k', hh['hhinc_cat'])
		hh['hhinc_cat'] = np.where((hh['HHINC2009'] >= 23000) & (hh['HHINC2009'] < 32000) & (hh['hhinc_cat'] == '999'), '23to32k', hh['hhinc_cat'])
		hh['hhinc_cat'] = np.where((hh['HHINC2009'] >= 32000) & (hh['HHINC2009'] < 46000) & (hh['hhinc_cat'] == '999'), '32to46k', hh['hhinc_cat'])
		hh['hhinc_cat'] = np.where((hh['HHINC2009'] >= 46000) & (hh['HHINC2009'] < 61000) & (hh['hhinc_cat'] == '999'), '46to61k', hh['hhinc_cat'])
		hh['hhinc_cat'] = np.where((hh['HHINC2009'] >= 61000) & (hh['HHINC2009'] < 76000) & (hh['hhinc_cat'] == '999'), '61to76k', hh['hhinc_cat'])
		hh['hhinc_cat'] = np.where((hh['HHINC2009'] >= 76000) & (hh['HHINC2009'] < 106000) & (hh['hhinc_cat'] == '999'), '76to106k', hh['hhinc_cat'])
		hh['hhinc_cat'] = np.where((hh['HHINC2009'] >= 106000) & (hh['hhinc_cat'] == '999'), '106kUp', hh['hhinc_cat'])

		hh['Category'] = 'HH' + hh['hhinc_cat'] + hh['hhsize_cat']

		hh['count'] = 1
		pp['count'] = 1

		taz_summary = hh.groupby('AZONE').agg({'HHINC2009':'mean', 
								 'count':'sum', 
								 'NP':'sum',
								 'NWESR':'sum',
								 })

		taz_summary.reset_index(inplace=True)
		taz_summary.rename(columns={'AZONE':'TAZ', 'count':'TotalHHs', 'NP':'TotalPersons', 'HHINC2009':'AvgHHInc', 'NWESR':'TotalWorkers'}, inplace=True)
		taz_summary = taz_summary[['TAZ', 'AvgHHInc', 'TotalHHs', 'TotalPersons', 'TotalWorkers']]

		taz_cat = pd.DataFrame(hh.groupby('AZONE')['Category'].value_counts())
		taz_cat.columns = ['Households']

		taz_cat.reset_index(inplace=True)
		taz_cat.columns = ['TAZ', 'Category', 'Households']
		taz_cat = taz_cat.pivot(index='TAZ', columns='Category', values='Households')
		taz_cat = pd.DataFrame(taz_cat.to_records())
		taz_summary = pd.merge(taz_summary, taz_cat, on = 'TAZ')

		pp['age_cat'] = '999'
		pp['age_cat'] = np.where(pp['AGEP'] < 5, 'Person0to5', pp['age_cat'])
		pp['age_cat'] = np.where((pp['AGEP'] >= 5) & (pp['AGEP'] < 10) & (pp['age_cat'] == '999'), 'Person5to10', pp['age_cat'])
		pp['age_cat'] = np.where((pp['AGEP'] >= 10) & (pp['AGEP'] < 15) & (pp['age_cat'] == '999'), 'Person10to15', pp['age_cat'])
		pp['age_cat'] = np.where((pp['AGEP'] >= 15) & (pp['AGEP'] < 21) & (pp['age_cat'] == '999'), 'Person15to21', pp['age_cat'])
		pp['age_cat'] = np.where((pp['AGEP'] >= 21) & (pp['AGEP'] < 40) & (pp['age_cat'] == '999'), 'Person21to40', pp['age_cat'])
		pp['age_cat'] = np.where((pp['AGEP'] >= 40) & (pp['AGEP'] < 60) & (pp['age_cat'] == '999'), 'Person40to60', pp['age_cat'])
		pp['age_cat'] = np.where((pp['AGEP'] >= 60), 'Person60plus', pp['age_cat'])

		taz_age = pd.DataFrame(pp.groupby('AZONE')['age_cat'].value_counts())
		taz_age.columns = ['Persons']
		taz_age.reset_index(inplace=True)
		taz_age.columns = ['TAZ', 'age_cat', 'Persons']
		taz_age = taz_age.pivot(index='TAZ', columns='age_cat', values='Persons')
		taz_age = pd.DataFrame(taz_age.to_records())
		taz_summary = pd.merge(taz_summary, taz_age, on = 'TAZ')
		
		azones.rename(columns={'AZONE':'TAZ'}, inplace=True)
		taz_summary = pd.merge(azones[['TAZ']], taz_summary, on = 'TAZ', how = 'left')
		taz_summary = taz_summary.fillna(0)
		
		spg2_synthetic_households = spg2_synthetic_households[['household_id', 'NP', 'BLD', 'VEH', 'HHINC2009', 'AZONE']]
		spg2_synthetic_households.columns = ['HH_ID', 'PERSONS', 'UNITS', 'AUTOS', 'RHHINC', 'Azone']
		
		spg2_synthetic_persons = spg2_synthetic_persons[['household_id', 'per_num', 'SEX', 'AGEP', 'INDP', 'OCCP']]
		spg2_synthetic_persons.columns = ['HH_ID', 'PERS_ID', 'SEX', 'AGE', 'INDUSTRY', 'OCCUP']
		
		spg2_synthetic_households.to_csv(self.spg2_synthetic_households_file2, index=False)
		spg2_synthetic_persons.to_csv(self.spg2_synthetic_persons_file2, index=False)
		taz_summary.to_csv(self.spg2_current_synpop_summary_file, index=False)
	
	
####################################################################################################################
# Entry Point

if __name__ == "__main__":

	property_file = sys.argv[1]
	mode = sys.argv[2]
	p = popsimSPG(property_file)
	
	if mode == 'runSPG1':
		p.createDirectories()
		p.createSeed() # need to call this only once and reuse for all years
		p.copySeeds()
		p.spg1Controls()
		p.run_spg1()
		p.spg1PostProcess()
	
	### AA needs to run in between these two
	
	if mode == 'runSPG2':
		#p.createDirectories()
		#p.copySeeds()
		#p.spg2Controls()
		#p.run_spg2()
		p.spg2PostProcess()
	
	print("end PopulationSim SPG run - " + mode + " - " + time.ctime())
