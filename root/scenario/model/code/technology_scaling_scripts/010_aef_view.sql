drop view if exists aef_ev;
create view aef_ev
as select year_run, am.activity, moru,
topt.put_name,
am.total_amount * topt.coefficient as amount
from activity_totals am
join technology_options topt on am.activity = topt.activity
where topt.option_name = 'EV';

drop view if exists aef_weighted;
create view aef_weighted
as select year_run, am.activity, moru, 
topt.put_name,
am.total_amount * topt.coefficient as amount
from activity_totals am
join 
   (select i.activity, i.put_name, i.moru, sum(i.option_weight*i.coefficient/allopts.sum_weight) as coefficient
     from technology_options i
     join (select activity, sum(weight) as sum_weight from tech_option group by activity) allopts
     on i.activity = allopts.activity group by i.activity, i.put_name, i.moru) 
topt on am.activity = topt.activity;
     
drop view if exists aef_make_use;
create view aef_make_use
as select year_run, am.activity, moru,
topt.put_name,
am.total_amount * topt.coefficient as amount,
topt.amount as make_use_base_amount
from activity_totals am
join make_use topt on am.activity = topt.activity;

drop view if exists all_3_amounts;
drop view if exists all_4_amounts;
create view all_4_amounts
as select b.year_run, b.activity, b.moru, b.put_name, 
a.amount as ev_amount, b.amount as weighted_amount, c.amount as makeuse_amount, c.make_use_base_amount
from aef_weighted b
left join aef_ev a on 
	a.activity = b.activity and a.put_name = b.put_name and a.moru = b.moru and a.year_run = b.year_run
left join aef_make_use c on
	c.activity = b.activity and c.put_name = b.put_name and c.moru = b.moru and c.year_run = b.year_run;



