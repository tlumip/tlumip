import sys, csv, os

# read and parse parameters file
"""
Expected parameters:
	ned.input.directory
	ned.activity_forecast.path
	ned.trade_forecast.path (deprecated)
	ned.construction_forecast.path
	ned.population_forecast.path
	ned.government_forecast.path
	ned.prior_activity_forecast.path
	ned.prior_trade_forecast.path (deprecated)
	ned.prior_construction_forecast.path
	ned.prior_population_forecast.path
	ned.prior_government_forecast.path
	ned.base.year=2009
	ned.model.year
	ned.base.year.model.year=19
	ned.allow.feedback=false
	ned.prior_aa_exchange_results.path
	ned.prior_aa_activity_summary.path
	ned.population_feedback_notes.path
	ned.prior_population_feedback_notes.path
"""
def ned():
	"""Produces a set of files containing model year economic and demographic forecasts for other SWIM2 modules.  Optionally incorporates feedback"""
	parameters_file = open(sys.argv[1], 'r')
	parameters = {}
	for line in parameters_file.readlines():
		#if line[:4] == 'ned.':
		line = line.split('=')
		if len(line) > 1:
		    parameters[line[0].strip()] = line[1].strip()
	parameters_file.close()

	# read the NED input files into dictionaries
	activities = {}
	input_file = csv.DictReader(open(parameters['ned.input.directory'] + '/activity_forecast.csv', 'rb'))
	for row in input_file:
		activities.setdefault(int(row['year']), {})
		activities[int(row['year'])][row['activity']] = {'output' : int(row['output']), 'employment' : int(row['employment'])}
	#
	population = {}
	input_file = csv.DictReader(open(parameters['ned.input.directory'] + '/population_forecast.csv', 'rb'))
	for row in input_file:
		population.setdefault(int(row['year']), {})
		population[int(row['year'])][row['age_group']] = int(row['population'])
	#
	"""
	trade = {}
	input_file = csv.DictReader(open(parameters['ned.input.directory'] + '/trade_forecast.csv', 'rb'))
	for row in input_file:
		trade.setdefault(int(row['year']), {})
		trade[int(row['year'])][row['trade_activity']] = int(row['dollars'])
	"""
	#
	construction = {}
	input_file = csv.DictReader(open(parameters['ned.input.directory'] + '/construction_forecast.csv', 'rb'))
	for row in input_file:
		construction.setdefault(int(row['year']), {})
		construction[int(row['year'])][row['res_or_non_res']] = int(row['dollars'])

	gov = {}
	input_file = csv.DictReader(open(parameters['ned.input.directory'] + '/government_forecast.csv', 'rb'))
	for row in input_file:
		gov.setdefault(int(row['year']), {})
		gov[int(row['year'])][row['activity']] = int(row['dollars'])

	input_file = None

	# model years don't necessariy start in the base year; the model year in the base year (2009) might be 19
	# that this module can't be run in a year prior to the base year
	if int(parameters['ned.model.year']) < int(parameters['ned.base.year.model.year']):
		raise IndexError("Model year is before base year.")

	this_year = int(parameters['ned.model.year']) + int(parameters['ned.base.year']) - int(parameters['ned.base.year.model.year'])
	last_year = this_year - 1
	base_year = int(parameters['ned.base.year'])

	# if this is the base year, just write the output files
	if  this_year == base_year:
		output_file = csv.writer(open(parameters['ned.activity_forecast.path'], 'wb'))
		output_file.writerow(['activity', 'employment', 'output'])
		for activity in activities[this_year].keys():
			output_file.writerow([activity, activities[this_year][activity]['employment'], activities[this_year][activity]['output']])
	
		"""
		output_file = csv.writer(open(parameters['ned.trade_forecast.path'], 'wb'))
		output_file.writerow(['trade_activity', 'dollars'])
		for commodity in trade[this_year].keys():
			output_file.writerow([commodity, trade[this_year][commodity]])
		"""
	
		output_file = csv.writer(open(parameters['ned.population_forecast.path'], 'wb'))
		output_file.writerow(['age_category', 'population', 'share'])
		total_pop = 0
		for age_group in population[this_year].keys():
			total_pop += int(population[this_year][age_group])
		for age_group in population[this_year].keys():
			output_file.writerow([age_group, int(population[this_year][age_group]), float(population[this_year][age_group]) / total_pop])
	
		output_file = csv.writer(open(parameters['ned.construction_forecast.path'], 'wb'))
		output_file.writerow(['activity', 'dollars'])
		output_file.writerow(['residential', construction[this_year]['residential']])
		output_file.writerow(['non_residential', construction[this_year]['non_residential']])
	
		output_file = csv.writer(open(parameters['ned.government_forecast.path'], 'wb'))
		output_file.writerow(['activity', 'dollars'])
		output_file.writerow(['FGOV_acct_gov', gov[this_year]['FGOV_acct_gov']])
		output_file.writerow(['SLGOV_acct_gov', gov[this_year]['SLGOV_acct_gov']])
		output_file.writerow(['CAP_acct_gov', gov[this_year]['CAP_acct_gov']])
	
	# else this is not the base year, so incorporate feedback
	else:
		# read the prior year's output files into dictionaries
		prior_activities = {}
		input_file = csv.DictReader(open(parameters['ned.prior_activity_forecast.path'], 'rb'))
		for row in input_file:
			prior_activities[row['activity']] = {'output' : int(row['output']), 'employment' : int(row['employment'])}
	
		prior_population = {}
		input_file = csv.DictReader(open(parameters['ned.prior_population_forecast.path'], 'rb'))
		for row in input_file:
			prior_population[row['age_category']] = int(row['population'])
	
		"""
		prior_trade = {}
		input_file = csv.DictReader(open(parameters['ned.prior_trade_forecast.path'], 'rb'))
		for row in input_file:
			prior_trade[row['trade_activity']] = int(row['dollars'])
		"""
		prior_construction = {}
		input_file = csv.DictReader(open(parameters['ned.prior_construction_forecast.path'], 'rb'))
		for row in input_file:
			prior_construction[row['activity']] = int(row['dollars'])
	
		prior_gov = {}
		input_file = csv.DictReader(open(parameters['ned.prior_government_forecast.path'], 'rb'))
		for row in input_file:
			prior_gov[row['activity']] = int(row['dollars'])
	
	
		# calculate ratios of this year's to last year's input values
		emp_ratio = {}
		out_ratio = {}
		for activity in activities[this_year].keys():
			if activities[last_year][activity]['employment']:
				emp_ratio[activity] = 1.0 * activities[this_year][activity]['employment'] / activities[last_year][activity]['employment']
			else:
				emp_ratio[activity] = 1
			if activities[last_year][activity]['output']:
				out_ratio[activity] = 1.0 * activities[this_year][activity]['output'] / activities[last_year][activity]['output']
			else:
				out_ratio[activity] = 1
		pop_ratio = {}
		for age_group in population[this_year].keys():
			pop_ratio[age_group] = 1.0 * population[this_year][age_group] / population[last_year][age_group]
		"""
		trade_ratio = {}
		for commodity in trade[this_year].keys():
			if trade[last_year][commodity]:
				trade_ratio[commodity] = 1.0 * trade[this_year][commodity] / trade[last_year][commodity]
			else:
				trade_ratio[commodity] = 1
		"""
		con_ratio = {}
		for rnr in construction[this_year].keys():
			con_ratio[rnr] = 1.0 * construction[this_year][rnr] / construction[last_year][rnr]
		gov_ratio = {}
		for activity in gov[this_year].keys():
			gov_ratio[activity] = 1.0 * gov[this_year][activity] / gov[last_year][activity]
		# read prior year's feedback files into dictionaries
		pass
	
		# adjust ratios with feedback
		pass
	
		# apply adjusted ratios to last year's output values and write this year's output files
		# round values to nearest integer
		output_file = csv.writer(open(parameters['ned.activity_forecast.path'], 'wb'))
		output_file.writerow(['activity', 'employment', 'output'])
		for activity in prior_activities.keys():
			output_file.writerow([activity, int(prior_activities[activity]['employment'] * emp_ratio[activity] + 0.5), 
				int(prior_activities[activity]['output'] * out_ratio[activity] + 0.5)])
	
		"""
		output_file = csv.writer(open(parameters['ned.trade_forecast.path'], 'wb'))
		output_file.writerow(['trade_activity', 'dollars'])
		for commodity in prior_trade.keys():
			output_file.writerow([commodity, int(prior_trade[commodity] * trade_ratio[commodity] + 0.5)])
		"""
	
		output_file = csv.writer(open(parameters['ned.population_forecast.path'], 'wb'))
		output_file.writerow(['age_category', 'population', 'share'])
		pop = {}
		total_pop = 0
		for age_group in prior_population.keys():
			pop[age_group] = int(prior_population[age_group] * pop_ratio[age_group] + 0.5)
			total_pop += pop[age_group]
		for age_group in pop.keys():
			output_file.writerow([age_group, pop[age_group], 1.0 * pop[age_group] / total_pop])
	
		output_file = csv.writer(open(parameters['ned.construction_forecast.path'], 'wb'))
		output_file.writerow(['activity', 'dollars'])
		output_file.writerow(['residential', int(prior_construction['residential'] * con_ratio['residential'] + 0.5)])
		output_file.writerow(['non_residential', int(prior_construction['non_residential'] * con_ratio['non_residential'] + 0.5)])

		output_file = csv.writer(open(parameters['ned.government_forecast.path'], 'wb'))
		output_file.writerow(['activity', 'dollars'])
		output_file.writerow(['FGOV_acct_gov', int(prior_gov['FGOV_acct_gov'] * gov_ratio['FGOV_acct_gov'] + 0.5)])
		output_file.writerow(['SLGOV_acct_gov', int(prior_gov['SLGOV_acct_gov'] * gov_ratio['SLGOV_acct_gov'] + 0.5)])
		output_file.writerow(['CAP_acct_gov', int(prior_gov['CAP_acct_gov'] * gov_ratio['CAP_acct_gov'] + 0.5)])

		output_file = None
	return None

if __name__ == '__main__':
	ned()
