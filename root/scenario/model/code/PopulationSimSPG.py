#import libraries
import pandas as pd, numpy as np
import multiprocessing as mp
import sys, os, subprocess, time, shutil
from functools import reduce
from Properties import Properties
from collections import Mapping
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
		self.python_exe = properties['python.popsim.executable'].replace('/','\\')
		self.spg2_num_processes = min(int(properties['spg2.num.processors']), 
									  max(mp.cpu_count()-1, 1))
		self.spg2_max_iterations = max(int(properties['spg2.max.iterations']), 
									  100)
		
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
		self.alpha2beta_file = properties['spg1.alpha2beta']
		self.acs_occ_file = properties['acs.sw.occupation.correspondence.edited.file.name']
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
		self.spg2_synpopH_file = properties['spg2.synpopH']
		self.spg2_synpopP_file = properties['spg2.synpopP']

	# The following functions perform the tasks listed below -
		# createDirectories - creates the PopulationSim directories
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

	def createSeed(self):
		
		try:
			f = open(self.hh_dist_file)
			f.close()
			f = open(self.seed_persons_file)
			f.close()
			f = open(self.seed_households_file)
			f.close()
			
		except IOError:

			"""input household and person data for OR, WA and CA"""
			pseed_or = pd.read_csv(self.pseed_or_file)
			pseed_wa = pd.read_csv(self.pseed_wa_file)
			pseed_ca = pd.read_csv(self.pseed_ca_file)
			hseed_or = pd.read_csv(self.hseed_or_file)
			hseed_wa = pd.read_csv(self.hseed_wa_file)
			hseed_ca = pd.read_csv(self.hseed_ca_file)
			# input file to identify pumas within the SWIM modeling region
			swim_pumas = pd.read_csv(self.puma_beta_alpha_xwalk_file)
			swim_pumas = swim_pumas[['PUMACE10', 'STATE']].groupby(["PUMACE10", "STATE"]).count().reset_index()
			alpha2beta = pd.read_csv(self.alpha2beta_file)
			alpha2beta = alpha2beta[['State', 'STATEFIPS']].drop_duplicates().rename(columns={'State':'STATE','STATEFIPS':'ST'})
			swim_pumas = pd.merge(swim_pumas, alpha2beta, left_on=['STATE'], right_on=['STATE'], how='left')
			# input file used to assign split industry to workers in the pums seed file
			split_ind = pd.read_csv(self.pums_to_split_industry)
			# input file to map workers in the seed file to acs occupation categories
			acs_occ = pd.read_csv(self.acs_occ_file)

			# creating PERSONID
			if pd.api.types.is_object_dtype(pseed_or['SERIALNO']):
				pseed_or['SERIALNO'] = pseed_or['SERIALNO'].astype(str).str.replace('\D\D','00').astype(np.int64)
				pseed_wa['SERIALNO'] = pseed_wa['SERIALNO'].astype(str).str.replace('\D\D','00').astype(np.int64)
				pseed_ca['SERIALNO'] = pseed_ca['SERIALNO'].astype(str).str.replace('\D\D','00').astype(np.int64)
				hseed_or['SERIALNO'] = hseed_or['SERIALNO'].astype(str).str.replace('\D\D','00').astype(np.int64)
				hseed_wa['SERIALNO'] = hseed_wa['SERIALNO'].astype(str).str.replace('\D\D','00').astype(np.int64)
				hseed_ca['SERIALNO'] = hseed_ca['SERIALNO'].astype(str).str.replace('\D\D','00').astype(np.int64)
			
			pseed_or['PERSONID'] = pseed_or['SERIALNO'] * 100 + pseed_or['SPORDER']
			pseed_wa['PERSONID'] = pseed_wa['SERIALNO'] * 100 + pseed_wa['SPORDER']
			pseed_ca['PERSONID'] = pseed_ca['SERIALNO'] * 100 + pseed_ca['SPORDER']

			# merging household and person files from 3 states and filtering for the pums region
			pseed_master = pd.concat([pseed_or, pseed_ca, pseed_wa])
			hseed_master = pd.concat([hseed_or, hseed_ca, hseed_wa])
			pseed = pd.merge(pseed_master, swim_pumas, left_on=['PUMA', 'ST'], right_on=['PUMACE10', 'ST'], how='right')
			hseed = pd.merge(hseed_master, swim_pumas, left_on=['PUMA', 'ST'], right_on=['PUMACE10', 'ST'], how='right')

			# SPG1 runs for the entire region, so the seed data must have a unique ID to map to the region. We create a field SEED and set it
			# to 1 for all pumas.
			pseed['SEED'] = 1
			# pseed['INDP'] = pseed['INDP'].fillna(0)
			# pseed['OCCP'] = pseed['OCCP'].fillna(0)

			split_ind.columns = ['pums_industry_code', 'pums_occ_code', 'split_industry_id', 'split_industry',
								 'proportion']
			seed_split = pd.merge(pseed, split_ind, left_on=['INDP', 'OCCP'],
								  right_on=['pums_industry_code', 'pums_occ_code'],
								  how='left')

			np.random.seed(2020)
			pseed['DRAW'] = np.random.uniform(0, 1, size=len(pseed))
			seed_split = pd.merge(seed_split, pseed[['PERSONID', 'DRAW']], on='PERSONID', how='left')
			seed_split['cumprop'] = seed_split.groupby(['PERSONID'])['proportion'].apply(lambda x: x.cumsum())
			seed_split['prev_cumprop'] = seed_split.groupby(['PERSONID'])['cumprop'].apply(lambda x: x.shift(1))
			seed_split.prev_cumprop.fillna(0, inplace=True)
			seed_split['select'] = np.where(
				(seed_split['DRAW'] < seed_split['cumprop']) & (seed_split['DRAW'] > seed_split['prev_cumprop']), 1, 0)
			seed_split['split_industry_id'].fillna(999, inplace=True)
			seed_split['select2'] = np.where(seed_split['split_industry_id'] == 999, 1, 0)
			seed_split['select'] = seed_split['select'] + seed_split['select2']

			assigned_ind_id = seed_split[seed_split['select'] == 1]
			assigned_ind_id.fillna(99999, inplace=True)
			assigned_ind_id = pd.merge(assigned_ind_id, acs_occ, left_on=['OCCP'], right_on=['occupation'], how='left')
			assigned_ind_id['occupationLabel'] = np.where(assigned_ind_id['ESR'].isin([3, 6, 99999]), 'No_Occupation',
														  assigned_ind_id['occupationLabel'])
			assigned_ind_id['split_industry_id'] = np.where(assigned_ind_id['ESR'].isin([3, 6, 99999]), 999,
                                                           assigned_ind_id['split_industry_id'])
			assigned_ind_id['split_industry'] = np.where(assigned_ind_id['ESR'].isin([3, 6, 99999]), '99999',
                                                           assigned_ind_id['split_industry'])

			hseed = hseed[hseed['NP'] > 0]
			hseed = hseed[hseed['TYPE'] == 1]
			hseed['SEED'] = 1
			hseed['NP_RECODE'] = np.where((hseed['NP'] >= 4), (np.round((hseed.loc[hseed['NP'] >= 4].NP.mean()), 2)),
										  hseed['NP'])
			hh_pp_seed = pd.merge(hseed, pseed, on=['SERIALNO'], how='left')
			hh_pp_seed['NWESR'] = np.where(hh_pp_seed['ESR'].isin([1, 2, 4, 5]), 1, 0)
			workers_seed = pd.DataFrame(hh_pp_seed.groupby('SERIALNO')['NWESR'].sum()).reset_index()
			hseed = pd.merge(hseed, workers_seed, on='SERIALNO', how='inner')
			hseed['hh_id'] = hseed.index + 1

			hseed_dist = pd.DataFrame(np.round((hseed.groupby('NP_RECODE')['WGTP'].sum()), 0))
			hseed_dist.columns = ['HH']
			hseed_dist.reset_index(inplace=True)
			hseed_dist['HH_PERCENT'] = np.round(((hseed_dist['HH'] * 100) / (hseed_dist['HH'].sum())), 1)

			adjfac_map = {1061971: 1.007549 * 1.05401460, 1045195: 1.008425 * 1.03646282,
						  1035988: 1.001264 * 1.03468042,
						  1029257: 1.007588 * 1.02150538, 1011189: 1.011189 * 1.00000000}
			hseed['ADJFAC'] = hseed['ADJINC'].map(adjfac_map)
			hseed['HHINC2017'] = np.round((hseed['HINCP']*hseed['ADJFAC']),0)
			hseed['HHINC2009'] = np.round((hseed['HHINC2017']*0.8695),0)

			hseed['hhsize_cat'] = np.where(hseed['NP'] <= 2, '1to2', '3plus')

			hseed['hhinc_cat'] = '999'
			hseed['hhinc_cat'] = np.where(hseed['HHINC2009'] < 8000, '0to8k', hseed['hhinc_cat'])
			hseed['hhinc_cat'] = np.where(
				(hseed['HHINC2009'] >= 8000) & (hseed['HHINC2009'] < 15000) & (hseed['hhinc_cat'] == '999'), '8to15k',
				hseed['hhinc_cat'])
			hseed['hhinc_cat'] = np.where(
				(hseed['HHINC2009'] >= 15000) & (hseed['HHINC2009'] < 23000) & (hseed['hhinc_cat'] == '999'), '15to23k',
				hseed['hhinc_cat'])
			hseed['hhinc_cat'] = np.where(
				(hseed['HHINC2009'] >= 23000) & (hseed['HHINC2009'] < 32000) & (hseed['hhinc_cat'] == '999'), '23to32k',
				hseed['hhinc_cat'])
			hseed['hhinc_cat'] = np.where(
				(hseed['HHINC2009'] >= 32000) & (hseed['HHINC2009'] < 46000) & (hseed['hhinc_cat'] == '999'), '32to46k',
				hseed['hhinc_cat'])
			hseed['hhinc_cat'] = np.where(
				(hseed['HHINC2009'] >= 46000) & (hseed['HHINC2009'] < 61000) & (hseed['hhinc_cat'] == '999'), '46to61k',
				hseed['hhinc_cat'])
			hseed['hhinc_cat'] = np.where(
				(hseed['HHINC2009'] >= 61000) & (hseed['HHINC2009'] < 76000) & (hseed['hhinc_cat'] == '999'), '61to76k',
				hseed['hhinc_cat'])
			hseed['hhinc_cat'] = np.where(
				(hseed['HHINC2009'] >= 76000) & (hseed['HHINC2009'] < 106000) & (hseed['hhinc_cat'] == '999'),
				'76to106k',
				hseed['hhinc_cat'])
			hseed['hhinc_cat'] = np.where((hseed['HHINC2009'] >= 106000) & (hseed['hhinc_cat'] == '999'), '106kUp',
										  hseed['hhinc_cat'])

			hseed['Category'] = 'HH' + hseed['hhinc_cat'] + hseed['hhsize_cat']

			seed_persons = pd.merge(assigned_ind_id, hseed[['SERIALNO', 'hh_id']], on='SERIALNO', how='inner')

			# outputs
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
			hh = hh[hh['NP'] > 0]
			pp = pd.concat([pp_ca, pp_or, pp_wa])
			
			hh_swim = pd.merge(hh, swim_pumas, left_on = ['PUMA'], right_on = ['PUMACE10'], how = 'right')
			pp_swim = pd.merge(pp, swim_pumas, left_on = ['PUMA'], right_on = ['PUMACE10'], how = 'right')
			
			hh_pp = pd.merge(hh_swim, pp_swim, on = ['SERIALNO'], how = 'left')
			
			hh_pp['WORKER'] = np.where(hh_pp['ESR'].isin([1,2,4,5]), 1, 0)
			
			workers = pd.DataFrame(hh_pp.groupby('SERIALNO').aggregate({'WORKER': 'sum', 'WGTP': 'mean'})).reset_index()
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
		hseed_dist = pd.read_csv(self.hh_dist_file)
		ned_emp = pd.read_csv(self.activity_forecast)
		ned_pop = pd.read_csv(self.population_forecast)
		hseed = pd.read_csv(self.seed_households_file)

		# Sort ned population file by age group
		ned_pop = ned_pop.sort_values(by=['age_category'])

		hseed['count'] = 1

		emp_to_wrkr = pd.merge(ned_emp, jobs_to_wrkr, left_on=['activity'], right_on=['SPG Sector'], how='inner')
		emp_to_wrkr['Workers'] = np.round((emp_to_wrkr['employment'] * emp_to_wrkr['2010WorkersPerJobFactor']), 0)

		# average hhsize is calculated from hhsize distribution created using PUMS data
		avg_hhsize = ((hseed_dist['NP_RECODE'] * hseed_dist['HH']).sum()) / (hseed_dist['HH'].sum())

		# total households is calculated by dividing total workers by average workers per household
		total_hh = np.round((ned_pop.population.sum() / avg_hhsize), 0)
		total_pp = np.round(ned_pop.population.sum(), 0)
		total_wk = np.round(emp_to_wrkr['Workers'].sum(), 0)
		total_emp = np.round(emp_to_wrkr['employment'].sum(), 0)

		control_columns = ['SUBSEED', 'HHS', 'PERS']
		control_columns.extend(ned_emp['activity'])
		age_vars = ['P_AGE0', 'P_AGE5', 'P_AGE10', 'P_AGE15', 'P_AGE20', 'P_AGE25', 'P_AGE30', 'P_AGE35', 'P_AGE40',
					'P_AGE45',
					'P_AGE50', 'P_AGE55', 'P_AGE60', 'P_AGE65', 'P_AGE70', 'P_AGE75', 'P_AGE80', 'P_AGE85']
		control_columns.extend(age_vars)

		control_totals = [1, total_hh, total_pp]
		#		control_totals.extend(hseed_dist['HH_scaled'])
		#		control_totals.extend(wrkrs_per_hh['2000households_scaled'])
		control_totals.extend(emp_to_wrkr['Workers'])
		control_totals.extend(ned_pop['population'])

		spg1_control_subseed = pd.DataFrame(control_totals).T
		spg1_control_subseed.columns = control_columns

		spg1_control_subseed = spg1_control_subseed[control_columns]
		spg1_control_region = pd.DataFrame({'REGION': [1], 'POPULATION': [total_pp]})
		spg1_geo_cross_walk = pd.DataFrame({'SUBSEED': [1], 'SEED': [1], 'REGION': [1]})

		spg1_control_subseed = spg1_control_subseed.astype(int)
		spg1_control_region = spg1_control_region.astype(int)

		# outputs: subseed control, regional control and geo cross walk
		spg1_control_subseed.to_csv(self.spg1_control_subseed_file, index=False)
		spg1_control_region.to_csv(self.spg1_control_region_file, index=False)
		spg1_geo_cross_walk.to_csv(self.spg1_geo_cross_walk_file, index=False)

	def run_spg1(self):
		cmd = "python " + self.popsim_py_file + " --config " + self.spg1_configs_directory + " --output " + self.spg1_output_directory + " --data " + self.spg1_data_directory
		subprocess.call(cmd)
		
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
		spg2_geo_cross_walk = puma_beta_alpha_xwalk[['Azone', 'PUMACE10', 'REGION']]
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
		spg2_control_alpha = spg2_control_alpha.astype(int)
		spg1_control_region = spg1_control_region.astype(int)
		spg2_geo_cross_walk = spg2_geo_cross_walk.astype(int)
		spg2_control_alpha.to_csv(self.spg2_control_alpha_file, index=False)
		spg1_control_region.to_csv(self.spg2_control_region_file, index=False)
		spg2_geo_cross_walk.to_csv(self.spg2_geo_cross_walk_file, index=False)
	
	def spg2Settings(self):
		settings_yaml_spg2_output = os.path.join(self.spg2_configs_directory,os.path.basename(self.settings_yaml_spg2))
		update_num_processes = {'@POPSIMSPG2.NUM.PROCESSES@': self.spg2_num_processes}
		update_max_iterations = {'@POPSIMSPG2.MAX.ITERATIONS@': self.spg2_max_iterations}
		f = open(settings_yaml_spg2_output,'w')
		for line in open(self.settings_yaml_spg2):
			# line = line.strip()
			for key in update_num_processes:
				line = line.replace(key,str(update_num_processes[key]))
			for key in update_max_iterations:
				line = line.replace(key,str(update_max_iterations[key]))
			f.write(line)
		f.close()
	
	def run_spg2(self):
		cmd = "python " + self.popsim_py_file + " --config " + self.spg2_configs_directory + " --output " + self.spg2_output_directory + " --data " + self.spg2_data_directory
		subprocess.call(cmd)

	def spg2PostProcess(self):
		spg2_synthetic_households = pd.read_csv(self.spg2_synthetic_households_file)
		spg2_synthetic_persons = pd.read_csv(self.spg2_synthetic_persons_file)
		acs_occ = pd.read_csv(self.acs_occ_file)
		
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
		
		azones.rename(columns={'Azone':'TAZ'}, inplace=True)
		taz_summary = pd.merge(azones[['TAZ']], taz_summary, on = 'TAZ', how = 'left')
		taz_summary = taz_summary.fillna(0)
		
		spg2_synthetic_households = spg2_synthetic_households[['household_id', 'NP', 'BLD', 'VEH', 'HHINC2009', 'AZONE']]
		spg2_synthetic_households.columns = ['HH_ID', 'PERSONS', 'UNITS1', 'AUTOS', 'RHHINC', 'Azone']
		
		acs_lookup = acs_occ.groupby('occupationLabel').min().reset_index()[['occupationLabel', 'occupationIndex']]
		spg2_synthetic_persons = pd.merge(spg2_synthetic_persons, acs_lookup, on = 'occupationLabel', how = 'left')
		spg2_synthetic_persons = spg2_synthetic_persons[['household_id', 'per_num', 'SEX', 'AGEP', 'SCH', 'ESR', 'INDP', 'OCCP', 'split_industry_id', 'occupationIndex']]
		spg2_synthetic_persons.columns = ['HH_ID', 'PERS_ID', 'SEX', 'AGE', 'SCHOOL', 'RLABOR', 'INDUSTRY', 'OCCUP', 'SW_UNSPLIT_IND', 'SW_OCCUP']
		spg2_synthetic_persons['SW_SPLIT_IND'] = spg2_synthetic_persons['SW_UNSPLIT_IND']
		
		spg2_synthetic_persons['SCHOOL'].replace(99999, 0, inplace=True)
		spg2_synthetic_persons['RLABOR'].replace(99999, 0, inplace=True)
		spg2_synthetic_persons['INDUSTRY'].replace(99999, 0, inplace=True)
		spg2_synthetic_persons['OCCUP'].replace(99999, 0, inplace=True)
		spg2_synthetic_persons['SW_UNSPLIT_IND'].replace(999, np.nan, inplace=True)
		spg2_synthetic_persons['SW_SPLIT_IND'].replace(999, np.nan, inplace=True)
		mask = spg2_synthetic_persons['RLABOR'].isin([1,2,4,5])
		synpop_worker = spg2_synthetic_persons[mask]
		synpop_nonwrk = spg2_synthetic_persons[~mask]
		synpop_worker = synpop_worker.sort_values(['INDUSTRY', 'OCCUP'], ascending = [True, True])
		synpop_worker[['SW_UNSPLIT_IND', 'SW_SPLIT_IND']] = synpop_worker[['SW_UNSPLIT_IND', 'SW_SPLIT_IND']].interpolate()
		spg2_synthetic_persons = pd.concat([synpop_nonwrk, synpop_worker])
		spg2_synthetic_persons = spg2_synthetic_persons.sort_values(["HH_ID","PERS_ID"])
		spg2_synthetic_persons['SW_UNSPLIT_IND'].replace(np.nan, 0, inplace=True)
		spg2_synthetic_persons['SW_SPLIT_IND'].replace(np.nan, 0, inplace=True)
		
		spg2_synthetic_households = spg2_synthetic_households.astype(int)
		spg2_synthetic_persons = spg2_synthetic_persons.astype(int)
		taz_summary = taz_summary.astype(int)
		
		spg2_synthetic_households.to_csv(self.spg2_synthetic_households_file2, index=False)
		spg2_synthetic_persons.to_csv(self.spg2_synthetic_persons_file2, index=False)
		spg2_synthetic_households.to_csv(self.spg2_synpopH_file, index=False)
		spg2_synthetic_persons.to_csv(self.spg2_synpopP_file, index=False)
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
		p.createDirectories()
		p.copySeeds()
		p.spg2Controls()
		p.spg2Settings()
		p.run_spg2()
		p.spg2PostProcess()
	
	print("end PopulationSim SPG run - " + mode + " - " + time.ctime())
