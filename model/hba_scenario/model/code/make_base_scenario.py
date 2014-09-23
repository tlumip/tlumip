"""Produces a complete set of files describing the baseline economic scenario for SWIM2"""
import csv # reads and writes csv fiels
from xlrd import open_workbook #reads excel workbooks
from bisect import bisect_left #finds position in sorted array

# Set constants
BASE_YEAR = 2009
END_YEAR = 2041
MAX_PRODUCTIVITY_RATIO = 1.05
GI_FILENAME = 'Baseline LR Growth Scenario 2011_05.xls'
GI_OUTPUT_BASE_YEAR_COLUMN = 11
GI_EMPLOYMENT_BASE_YEAR_COLUMN = 9
# FGOV_acct_gov	 93817953918
# SLGOV_acct_gov 71879103322
# CAP_acct_gov 69549704567
BASE_FED_TAX = 93817953918.0
BASE_SL_TAX = 71879103322.0
BASE_CORP_TAX = 69549704567.0
FED_TAX_ELASTICITY = 1.214
SL_TAX_ELASTICITY = 1.234
CORP_TAX_ELASTICITY = 1.054

# Define functions to get external data from files, which are assumed to reside in current working directory.
def get_IMPLAN_industry_detail():
	"""
	Returns a dictionary containing IMPLAN industry detail, with the base year's data filled in.
		Outer key is year. 
		Next is region (the portion of each state in the model region and the entire model region)
		Next is IMPLAN industry identifier (1 to 440)
		Inner keys identify data elements (employment, output, compensation, or value_added).
	"""
	implan_activity = {}
	for year in range(BASE_YEAR, END_YEAR + 1):
		implan_activity[year] = {}
		for region in ('OR', 'WA', 'ID', 'NV', 'CA', 'model_region'):
			implan_activity[year][region] = {}
			for sector in range(1, 441):
				implan_activity[year][region][sector] = {'employment' : 0, 'output' : 0, 'compensation' : 0, 'value_added' : 0}
	FILENAMES = {'OR' : 'Oregon Industry Detail.xls', 
		'WA' : 'Washington Industry Detail.xls',
		'ID' : 'Idaho Industry Detail.xls',
		'NV' : 'Nevada Industry Detail.xls',
		'CA' : 'California Industry Detail.xls',
		'model_region' : 'OregonAndHalo1 Industry Detail.xls'}
	for region in FILENAMES.keys():
		wb = open_workbook(FILENAMES[region])
		sheet = wb.sheet_by_index(0) # use the first (and only) worksheet in the workbook
		for i in range(3, 443): # skip heading rows and totals row
			row = sheet.row_values(i, 0)
			implan_activity[BASE_YEAR][region][int(row[0])]['employment'] = row[2]
			implan_activity[BASE_YEAR][region][int(row[0])]['output'] = row[3]
			implan_activity[BASE_YEAR][region][int(row[0])]['compensation'] = row[4]
			implan_activity[BASE_YEAR][region][int(row[0])]['other_value_added'] = row[5] + row[6] + row[7]
	return implan_activity

def get_IMPLAN_CGE():
	"""
	Returns two dictionaries containing data in IMPLAN's industry-by-industry CGE table.
	
	The first, implan_commodity, contains imports and exports by commodity.
		Outer keys are years; inner keys are IMPALN commodity identifiers (3001 to 3440).
		Only values for the base year are filled in; the rest are zeros for now.
	The second, implan_structure, contains imports and exports by each industry by commodity.
		Outer kyes are IMPLAN industry identifiers (1 to 440).
		Inner keys are IMPALN commodity identifiers (3001 to 3440).
		"""
	implan_commodity = {}
	for year in range(BASE_YEAR, END_YEAR + 1):
		implan_commodity[year] = {}
		for commodity in range(3001, 3441):
			implan_commodity[year][commodity] = {'industry_imports' : 0, 'industry_exports' : 0,
				'institutional_imports' : 0, 'institutional_exports' : 0}
	implan_structure = {}
	for sector in range(1, 441):
		implan_structure[sector] = {}
		for commodity in range(3001, 3441):
			implan_structure[sector][commodity] = {'industry_imports' : 0, 'industry_exports' : 0}
	cge = csv.reader(open('cge_IxI.csv', 'rb'))
	header = True
	for row in cge:
		if header:
			header = False #skip column headings
		elif row[2] in ('1x7', '1x8'):
			implan_structure[int(row[0])][int(row[1])]['industry_exports'] += 1000000 * float(row[3])
			implan_commodity[BASE_YEAR][int(row[1])]['industry_exports'] += 1000000 * float(row[3])
		elif row[2] in ('7x1', '8x1'):
			implan_structure[int(row[1])][int(row[0])]['industry_imports'] += 1000000 * float(row[3])
			implan_commodity[BASE_YEAR][int(row[0])]['industry_imports'] += 1000000 * float(row[3])
		elif row[2] in ('7x4', '8x4'):
			implan_commodity[BASE_YEAR][int(row[0])]['institutional_imports'] += 1000000 * float(row[3])
		elif row[2] in ('4x7', '4x8'):
			implan_commodity[BASE_YEAR][int(row[1])]['institutional_exports'] += 1000000 * float(row[3])
	cge = None
	return implan_commodity, implan_structure

def get_state_forecasts():
	"""
	Returns a dictionary containing state forecasts of employment.
	
	Only Oregon provides a suitable forecast now; others may be added if they become available.
	Outer keys are state identifiers.
	Second-level keys are years.
	Inner keys are industry sectors, which are aggregated to a level that can match aggregations of IMPLAN industry identifiers.
	"""
	state_forecasts = {}
	for state in ('OR', 'WA', 'ID', 'NV', 'CA'):
		state_forecasts[state] = {}
		for year in range(BASE_YEAR, END_YEAR + 1):
			state_forecasts[state][year] = {}
	#
	wb = open_workbook('employment-annual.xls')
	BASE_YEAR_COLUMN = 21
	sheet = wb.sheet_by_index(0)
	for i in range(4, 64):
		row = sheet.row_values(i, 0)
		if row[1] == 'Oregon':
			for yr in range(BASE_YEAR, BASE_YEAR + len(row) - BASE_YEAR_COLUMN):
				state_forecasts['OR'][yr][row[0].strip()] = row[yr - BASE_YEAR + BASE_YEAR_COLUMN]
	# make some aggregations to match aggregations of NED sectors
	aggregate_oregon = {'Other Manufacturing' : ('Wood Products', 'Metals and Machinery', 'Transportation Equipment',
		'Other Durables', 'Other Nondurables'), 'Education' : ('Educational Services', 'Education, State Government',
		'Education, Local Government'), 'Government Administration' : ('Government', '-Education, Local Government',
		'-Education, State Government')}
	for yr in range(BASE_YEAR, BASE_YEAR + len(row) - BASE_YEAR_COLUMN):
		for agg in aggregate_oregon.keys():
			state_forecasts['OR'][yr][agg] = 0
			for sector in aggregate_oregon[agg]:
				if sector[0] == '-':
					state_forecasts['OR'][yr][agg] -= state_forecasts['OR'][yr][sector[1:]]
				else:
					state_forecasts['OR'][yr][agg] += state_forecasts['OR'][yr][sector]
	
	return state_forecasts

def get_population():
	"""
	Returns a dictionary containing Oregon populaiton forecsast by 5-year age groups.
	
	Outer keys are years.
	Inner keys are age groups identified by the bottom of the range (e.g. 25- to 29-year-olds are identified by 25).
	"""
	population = {}
	wb = open_workbook('pop_forecast.xls')
	sheet = wb.sheet_by_index(0)
	for i in range(1, 43):
		row = sheet.row_values(i, 0)
		year = row[0]
		population[year] = {}
		for age in range(0, 86, 5):
			population[year][age] = row[1 + age / 5]
	return population

def get_national_forecast():
	"""
	Returns a dictionary containing the Global Insight long-range forecast of US employment and output.
	
	Outer keys are years.
	Inner keys identify data elements.
	When a new forecast becomes available:
		Alter the GI_FILENAME, GI_OUTPUT_BASE_YEAR_COLUMN, and GI_EMPLOYMENT_BASE_YEAR_COLUMN global constants as necessary.
		Base year columns are counted from the left starting with 1 (e.g., A=1, B=2, etc.)
	"""
	national_forecast = {}
	for year in range(BASE_YEAR, END_YEAR + 1):
		national_forecast[year] = {'output' : {}, 'employment' : {}}
	wb = open_workbook(GI_FILENAME)
	sheet = wb.sheet_by_index(7)
	for i in range(1, 77):
		row = sheet.row_values(i, 0)
		for yr in range(BASE_YEAR, BASE_YEAR + len(row) - GI_OUTPUT_BASE_YEAR_COLUMN):
			national_forecast[yr]['output'][row[2].strip()] = row[yr - BASE_YEAR + GI_OUTPUT_BASE_YEAR_COLUMN]
	sheet = wb.sheet_by_index(5)
	for i in range(5, 66):
		row = sheet.row_values(i, 0)
		if row[1] > ' ':
			for yr in range(BASE_YEAR, BASE_YEAR + len(row) - GI_EMPLOYMENT_BASE_YEAR_COLUMN):
				national_forecast[yr]['employment'][row[1].strip()] = row[yr - BASE_YEAR + GI_EMPLOYMENT_BASE_YEAR_COLUMN]
	return national_forecast

def get_state_crosswalks():
	"""
	Returns a dictionary matching IMPLAN industry identifiers to the sectors used in state and national forecasts.
	
	Only Oregon provides a suitable forecast now; others may be added if they become available.
	Outer keys identify the state or 'US' for national.
	For states, inner keys are IMPLAN industry identifiers (1 to 440).
	For national, second-level keys are 'output' and 'employment' and inner keys are IMPLAN industry identifiers (1 to 440).
	Values are the sector identifierss used in the individual forecasts.
	"""
	state_crosswalks = {'OR' : {}, 'WA' : {}, 'ID' : {}, 'NV' : {}, 'CA' : {}, 'US' : {'output' : {}, 'employment' : {}}}
	wb = open_workbook('IMPLAN_OEA.xls')
	sheet = wb.sheet_by_index(0)
	for i in range(1, 436):
		row = sheet.row_values(i, 0)
		state_crosswalks['OR'][row[0]] = row[2]
	wb = open_workbook('implan_gi_sectors.xls')
	sheet = wb.sheet_by_index(0)
	for i in range(1, 437):
		row = sheet.row_values(i, 0)
		state_crosswalks['US']['output'][row[0]] = row[1]
	sheet = wb.sheet_by_index(1)
	for i in range(1, 437):
		row = sheet.row_values(i, 0)
		state_crosswalks['US']['employment'][row[0]] = row[1]
	return state_crosswalks

def get_IMPLAN_crosswalks():
	"""
	Returns two dictionaries matching IMPLAN industries and commodities to AA activities and AA commodities.
	
	The first, implan_ned_activity, matches IMPLAN industry identifiers to AA activity identifiers.
		Outer keys are IMPLAN industry identifiers.
		Second-level keys are AA activity identifiers.
		Inner keys are 'employment_proportion' and 'output_proportion'.
		Values range from zero to one and represent the proportion of activity in the IMPLAN sector 
			that is assigned to that AA activity.
	The second, implan_aa_commodity, matches IMPLAN commodity identifiers to AA commodity identifiers.
		Outer keys are IMPLAN commodity identifiers.
		Inner keys are AA commodity identifiers.
		Values range from zero to one and represent the proportion of the IMPLAN commodity 
			that is assigned to that AA commodity.
	"""
	implan_ned_activity = {}
	wb = open_workbook('implan_ned_activity.xls')
	sheet = wb.sheet_by_index(0)
	for i in range(1, 1014):
		row = sheet.row_values(i, 0)
		implan_ned_activity.setdefault(row[0], {})
		implan_ned_activity[row[0]][row[1]] = {'employment_proportion' : row[3], 'output_proportion' : row[2]}
	implan_aa_commodity = {}
	wb = open_workbook('implan_aa_commodity.xls')
	sheet = wb.sheet_by_index(0)
	for i in range(1, 443):
		row = sheet.row_values(i, 0)
		implan_aa_commodity.setdefault(row[0], {})
		implan_aa_commodity[row[0]][row[1]] = row[2]
	return implan_ned_activity, implan_aa_commodity

def do_base_year(implan_ned_activity, implan_activity, implan_aa_commodity, implan_commodity):
	"""
	Returns five dictionaries containing base-year data from input files.  These dictionaries get added to when
	subsequent years are run.
	
	The first, ned_activity, contains employment, output, compensation, and other value added for each activity.
		Outer key is the year, next is the activity identifier, next is one of employment, output, compensation, or other_value_added.
		Values are jobs for employment and dollars for everything else.
	
	The second, ned_trade, contains imports and exports of each commodity. 
		Outer key is the year, next is the commodity identifier, and next is either imports or exports.
		Values are dollars.
	
	The third, ned construction, contains residential and non-residential construction activity.
		Outer key is year, and next is either residential or non-residential.
		Values are dollars.
	
	The fourth, ned_gov, contains government and investment activity.
		Outer key is year, and next is either fed, sl, or corp.
		Values are dollars.
	
	The fifth, tot_emp, contains total employment.
		Key is year and value jobs.
	"""
	ned_activity = {}
	for year in range(BASE_YEAR, END_YEAR + 1):
		ned_activity[year] = {}

	ned_trade = {}
	for year in range(BASE_YEAR, END_YEAR + 1):
		ned_trade[year] = {}

	ned_construction = {}
	for year in range(BASE_YEAR, END_YEAR + 1):
		ned_construction[year] = {}

	ned_gov = {}
	for year in range(BASE_YEAR, END_YEAR + 1):
		ned_gov[year] = {}

	# build base-year NED tables by aggregating IMPLAN data
	# ned_activity
	for implan_sector in implan_ned_activity.keys():
		for ned_sector in implan_ned_activity[implan_sector].keys():
			ned_activity[BASE_YEAR].setdefault(ned_sector, {'employment' : 0, 'output' : 0, 'compensation' : 0, 'other_value_added' : 0})
			ned_activity[BASE_YEAR][ned_sector]['employment'] += (implan_activity[BASE_YEAR]['model_region'][implan_sector]['employment']
				* implan_ned_activity[implan_sector][ned_sector]['employment_proportion']) 
			ned_activity[BASE_YEAR][ned_sector]['output'] += (implan_activity[BASE_YEAR]['model_region'][implan_sector]['output']
				* implan_ned_activity[implan_sector][ned_sector]['output_proportion']) 
			ned_activity[BASE_YEAR][ned_sector]['compensation'] += (implan_activity[BASE_YEAR]['model_region'][implan_sector]['compensation']
				* implan_ned_activity[implan_sector][ned_sector]['employment_proportion']) 
			ned_activity[BASE_YEAR][ned_sector]['other_value_added'] += (implan_activity[BASE_YEAR]['model_region'][implan_sector]['other_value_added']
				* implan_ned_activity[implan_sector][ned_sector]['output_proportion']) 

	# ned_trade
	for commodity in implan_aa_commodity.keys():
		for aa_commodity in implan_aa_commodity[commodity].keys():
			ned_trade[BASE_YEAR].setdefault(aa_commodity, {'imports' : 0, 'exports' : 0})
			ned_trade[BASE_YEAR][aa_commodity]['imports'] += ((implan_commodity[BASE_YEAR][commodity]['industry_imports']
				+ implan_commodity[BASE_YEAR][commodity]['institutional_imports']) * implan_aa_commodity[commodity][aa_commodity])
			ned_trade[BASE_YEAR][aa_commodity]['exports'] += ((implan_commodity[BASE_YEAR][commodity]['industry_exports']
				+ implan_commodity[BASE_YEAR][commodity]['institutional_exports']) * implan_aa_commodity[commodity][aa_commodity])

	# ned_construction
	ned_construction[BASE_YEAR]['residential'] = (implan_activity[BASE_YEAR]['model_region'][37]['output']
		+ implan_activity[BASE_YEAR]['model_region'][38]['output'] + implan_activity[BASE_YEAR]['model_region'][40]['output'])
	ned_construction[BASE_YEAR]['non_residential'] = (implan_activity[BASE_YEAR]['model_region'][34]['output']
		+ implan_activity[BASE_YEAR]['model_region'][35]['output'] + implan_activity[BASE_YEAR]['model_region'][39]['output']
		* (implan_activity[BASE_YEAR]['model_region'][34]['output'] + implan_activity[BASE_YEAR]['model_region'][35]['output'])
		/ (implan_activity[BASE_YEAR]['model_region'][34]['output'] + implan_activity[BASE_YEAR]['model_region'][35]['output']
		+ implan_activity[BASE_YEAR]['model_region'][36]['output']))

	# ned government revenues
	ned_gov[BASE_YEAR]['fed'] = BASE_FED_TAX
	ned_gov[BASE_YEAR]['sl'] = BASE_SL_TAX
	ned_gov[BASE_YEAR]['corp'] = BASE_CORP_TAX
	tot_emp = {BASE_YEAR : 0}
	for ned_sector in ned_activity[BASE_YEAR].keys():
		tot_emp[BASE_YEAR] += ned_activity[BASE_YEAR][ned_sector]['employment']
	
	return ned_activity, ned_trade, ned_construction, ned_gov, tot_emp

def do_forecast_year(year, state_forecasts, state_crosswalks, implan_activity, national_forecast, implan_ned_activity, ned_activity, 
	ned_trade, ned_construction, ned_gov, implan_aa_commodity, productivity_ratios, population, implan_structure, implan_commodity, tot_emp):
	"""
	Returns six dictionaries containing forecast-year data from computations.
	
	The first, ned_activity, contains employment, output, compensation, and other value added for each activity.
		Outer key is the year, next is the activity identifier, next is one of employment, output, compensation, or other_value_added.
		Values are jobs for employment and dollars for everything else.
	
	The second, implan_activity, containins the same things by IMPLAN industry sector.
			Outer key is year. 
			Next is region (the portion of each state in the model region and the entire model region)
			Next is IMPLAN industry identifier (1 to 440)
			Inner keys identify data elements (employment, output, compensation, or value_added).
	
	The third, ned_trade, contains imports and exports of each commodity. 
		Outer key is the year, next is the commodity identifier, and next is either imports or exports.
		Values are dollars.
	
	The fourth, ned construction, contains residential and non-residential construction activity.
		Outer key is year, and next is either residential or non-residential.
		Values are dollars.
	
	The fifth, ned_gov, contains government and investment activity.
		Outer key is year, and next is either fed, sl, or corp.
		Values are dollars.
	
	The sixth, tot_emp, contains total employment.
		Key is year and value jobs.
	"""
	# forecast employment
	for state in ('OR', 'WA', 'ID', 'NV', 'CA'):
		emp_growth = {}
		if len(state_forecasts[state][year]):
			for sector in state_forecasts[state][year].keys():
				if state_forecasts[state][year - 1][sector] > 0:
					emp_growth[sector] = (state_forecasts[state][year][sector] / state_forecasts[state][year - 1][sector])
				else:
					emp_growth[sector] = 1.0
			for implan_sector in state_crosswalks[state].keys():
				implan_activity[year][state][implan_sector]['employment'] = (implan_activity[year - 1][state][implan_sector]['employment']
					* emp_growth[state_crosswalks[state][implan_sector]])
		else:
			for sector in national_forecast[year]['employment'].keys():
				if national_forecast[year - 1]['employment'][sector] > 0:
					emp_growth[sector] = (national_forecast[year]['employment'][sector] 
						/ national_forecast[year - 1]['employment'][sector])
				else:
					emp_growth[sector] = 1.0
			for implan_sector in state_crosswalks['US']['employment'].keys():
				implan_activity[year][state][implan_sector]['employment'] = (implan_activity[year - 1][state][implan_sector]['employment']
					* emp_growth[state_crosswalks['US']['employment'][implan_sector]])
		# add to model region totals
		for implan_sector in implan_activity[year][state].keys():
			implan_activity[year]['model_region'][implan_sector]['employment'] += implan_activity[year][state][implan_sector]['employment']
	# aggregate to NED activities
	for implan_sector in implan_ned_activity.keys():
		for ned_sector in implan_ned_activity[implan_sector].keys():
			ned_activity[year].setdefault(ned_sector, {'employment' : 0, 'output' : 0, 'compensation' : 0, 'other_value_added' : 0})
			ned_activity[year][ned_sector]['employment'] += (implan_activity[year]['model_region'][implan_sector]['employment']
				* implan_ned_activity[implan_sector][ned_sector]['employment_proportion']) 
	
	# forecast output
	# find ratio of this year's output per employee to last year's
	productivity_ratio = {}
	for implan_sector in state_crosswalks['US']['output'].keys():
		gi_emp_sector = state_crosswalks['US']['employment'][implan_sector]
		gi_out_sector = state_crosswalks['US']['output'][implan_sector]
		productivity_ratio[implan_sector] = min(MAX_PRODUCTIVITY_RATIO, (national_forecast[year]['output'][gi_out_sector] 
			/ national_forecast[year]['employment'][gi_emp_sector])
			/ (national_forecast[year - 1]['output'][gi_out_sector] / national_forecast[year - 1]['employment'][gi_emp_sector]))
	productivity_ratios[year] = productivity_ratio
	# apply to forecasted employment
	for implan_sector in implan_activity[year]['model_region'].keys():
		if (not implan_activity[year - 1]['model_region'][implan_sector]['employment'] 
			or not implan_activity[year]['model_region'][implan_sector]['employment']):
			productivity = 1
		else:
			productivity = (implan_activity[year - 1]['model_region'][implan_sector]['output'] 
				/ implan_activity[year - 1]['model_region'][implan_sector]['employment']
				* productivity_ratio[implan_sector])
		implan_activity[year]['model_region'][implan_sector]['output'] = (implan_activity[year]['model_region'][implan_sector]['employment'] 
			* productivity)
	# aggregate to NED activities
	for implan_sector in implan_ned_activity.keys():
		for ned_sector in implan_ned_activity[implan_sector].keys():
			ned_activity[year][ned_sector]['output'] += (implan_activity[year]['model_region'][implan_sector]['output']
				* implan_ned_activity[implan_sector][ned_sector]['output_proportion']) 
	
	# forecast trade
	# calculate ratio of this year's output to base year's
	output_ratios = {}
	for implan_sector in implan_activity[year]['model_region'].keys():
		if implan_activity[BASE_YEAR]['model_region'][implan_sector]['output']:
			output_ratios[implan_sector] = implan_activity[year]['model_region'][implan_sector]['output'] / implan_activity[BASE_YEAR]['model_region'][implan_sector]['output']
		else:
			output_ratios[implan_sector] = 1.0
	
	# calculate ratio of this year's population to base year's
	base_year_pop = 0
	this_year_pop = 0
	for age_group in population[BASE_YEAR].keys():
		base_year_pop += population[BASE_YEAR][age_group]
		this_year_pop += population[year][age_group]
	pop_ratio = this_year_pop / base_year_pop
	
	# calculate industry exports by scaling each industry's make of each export commodity with output ratio
	for implan_sector in implan_structure.keys():
		for commodity in implan_structure[implan_sector].keys():
			implan_commodity[year][commodity]['industry_exports'] += (implan_structure[implan_sector][commodity]['industry_exports'] 
				* output_ratios[implan_sector])
	
	# calculate industry imports by scaling each industry's use of each import commodity with output ratio
	for implan_sector in implan_structure.keys():
		for commodity in implan_structure[implan_sector].keys():
			implan_commodity[year][commodity]['industry_imports'] += (implan_structure[implan_sector][commodity]['industry_imports'] 
				* output_ratios[implan_sector])
	
	# calculate institutional imports and exports by scaling with population ratio
	for commodity in implan_commodity[year].keys():
		implan_commodity[year][commodity]['institutional_imports'] = implan_commodity[BASE_YEAR][commodity]['institutional_imports'] * pop_ratio
		implan_commodity[year][commodity]['institutional_exports'] = implan_commodity[BASE_YEAR][commodity]['institutional_exports'] * pop_ratio
		implan_commodity[year][commodity]['imports'] = (implan_commodity[year][commodity]['industry_imports'] 
			+ implan_commodity[year][commodity]['institutional_imports'])
		implan_commodity[year][commodity]['exports'] = (implan_commodity[year][commodity]['industry_exports']
			+ implan_commodity[year][commodity]['institutional_exports'])
	
	# aggregate imports and exports to AA commodities
	for commodity in implan_aa_commodity.keys():
		for aa_commodity in implan_aa_commodity[commodity].keys():
			ned_trade[year].setdefault(aa_commodity, {'imports' : 0, 'exports' : 0})
			ned_trade[year][aa_commodity]['imports'] += (implan_commodity[year][commodity]['imports']
				* implan_aa_commodity[commodity][aa_commodity])
			ned_trade[year][aa_commodity]['exports'] += (implan_commodity[year][commodity]['exports']
				* implan_aa_commodity[commodity][aa_commodity])
	
	
	# calculate construction
	ned_construction[year]['residential'] = (implan_activity[year]['model_region'][37]['output']
		+ implan_activity[year]['model_region'][38]['output'] + implan_activity[year]['model_region'][40]['output'])
	ned_construction[year]['non_residential'] = (implan_activity[year]['model_region'][34]['output']
		+ implan_activity[year]['model_region'][35]['output'] + implan_activity[year]['model_region'][39]['output']
		* (implan_activity[year]['model_region'][34]['output'] + implan_activity[year]['model_region'][35]['output'])
		/ (implan_activity[year]['model_region'][34]['output'] + implan_activity[year]['model_region'][35]['output']
		+ implan_activity[year]['model_region'][36]['output']))
	
	# calculate gevernment revenues
	for ned_sector in ned_activity[year].keys():
		tot_emp[year] += ned_activity[year][ned_sector]['employment']
	emp_pct_chg = 1.0 * tot_emp[year] / tot_emp[year - 1] - 1.0
	ned_gov[year]['fed'] = ned_gov[year-1]['fed'] * (1.0 + emp_pct_chg * FED_TAX_ELASTICITY)
	ned_gov[year]['sl'] = ned_gov[year-1]['sl'] * (1.0 + emp_pct_chg * SL_TAX_ELASTICITY)
	ned_gov[year]['corp'] = ned_gov[year-1]['corp'] * (1.0 + emp_pct_chg * CORP_TAX_ELASTICITY)
	
	return ned_activity, implan_activity, ned_trade, ned_construction, ned_gov, tot_emp


def make_base_scenario():
	"""
	Gets input, performs calculations, writes output files, and returns nothing.
	"""
	# get input data
	implan_activity = get_IMPLAN_industry_detail()
	implan_commodity, implan_structure = get_IMPLAN_CGE()
	state_forecasts = get_state_forecasts()
	national_forecast = get_national_forecast()
	population = get_population()
	state_crosswalks = get_state_crosswalks()
	implan_ned_activity, implan_aa_commodity = get_IMPLAN_crosswalks()
	
	# do base-year calculations
	ned_activity, ned_trade, ned_construction, ned_gov, tot_emp = do_base_year(implan_ned_activity, implan_activity, 
		implan_aa_commodity, implan_commodity)
	
	# do subsequent years
	productivity_ratios = {}
	for year in range(BASE_YEAR + 1, END_YEAR + 1):
		tot_emp[year] = 0
		productivity_ratios[year] = {}
		ned_activity, implan_activity, ned_trade, ned_construction, ned_gov, tot_emp = do_forecast_year(year, state_forecasts, 
			state_crosswalks, implan_activity, national_forecast, implan_ned_activity, ned_activity, ned_trade, ned_construction, 
			ned_gov, implan_aa_commodity, productivity_ratios, population, implan_structure, implan_commodity, tot_emp)
	
	# write output files
	outfile = open('activity_forecast.csv', 'w')
	outfile.write("year,activity,employment,output\n")
	years = ned_activity.keys()
	years.sort()
	ned_sectors = ned_activity[years[0]].keys()
	ned_sectors.sort()
	for year in years:
		for sector in ned_sectors:
			outfile.write(str(year) + ',' + sector + ',' + str(int(ned_activity[year][sector]['employment'])) + ',' 
				+ str(int(ned_activity[year][sector]['output'])) + "\n")
	outfile.close()
	outfile = open('trade_forecast.csv', 'w')
	outfile.write("year,trade_activity,dollars\n")
	years = ned_trade.keys()
	years.sort()
	aa_commodities = ned_trade[years[0]].keys()
	aa_commodities.sort()
	for year in years:
		for commodity in aa_commodities:
			outfile.write(str(year) + ',' + commodity + '_expt' + ',' + str(int(max(0, ned_trade[year][commodity]['exports']))) + "\n")
			outfile.write(str(year) + ',' + commodity + '_impt' + ',' + str(int(max(0, ned_trade[year][commodity]['imports']))) + "\n")
	outfile.close()
	outfile = open('construction_forecast.csv', 'w')
	outfile.write("year,res_or_non_res,dollars\n")
	years = ned_construction.keys()
	years.sort()
	for year in years:
		outfile.write(str(year) + ',' + 'residential' + ',' + str(int(ned_construction[year]['residential'])) + "\n")
		outfile.write(str(year) + ',' + 'non_residential' + ',' + str(int(ned_construction[year]['non_residential'])) + "\n")
	outfile.close()
	outfile = open('population_forecast.csv', 'w')
	outfile.write("year,age_group,population\n")
	years = population.keys()
	years.sort()
	age_groups = population[years[0]].keys()
	age_groups.sort()
	for year in years:
		for age_group in age_groups:
			outfile.write(str(int(year)) + ',' + str(age_group) + ',' + str(int(population[year][age_group])) + "\n")
	outfile.close()
	outfile = open('productivity_ratios.csv', 'w')
	outfile.write("year,implan_sector,ratio\n")
	years = productivity_ratios.keys()
	years.sort()
	for year in years:
		sectors = productivity_ratios[year].keys()
		sectors.sort()
		for sector in sectors:
			outfile.write(str(year) + ',' + str(sector) + ',' + str(productivity_ratios[year][sector]) + "\n")
	outfile.close()
	outfile = open('government_forecast.csv', 'w')
	outfile.write("year,activity,dollars\n")
	years = ned_gov.keys()
	years.sort()
	for year in years:
		outfile.write(str(year) + ',' + 'FGOV_acct_gov' + ',' + str(int(ned_gov[year]['fed'])) + "\n")
		outfile.write(str(year) + ',' + 'SLGOV_acct_gov' + ',' + str(int(ned_gov[year]['sl'])) + "\n")
		outfile.write(str(year) + ',' + 'CAP_acct_gov' + ',' + str(int(ned_gov[year]['corp'])) + "\n")
	outfile.close()
	return None

if __name__ == '__main__':
	make_base_scenario()
