drop table if exists base_year_net_exogenous_supply;
create table base_year_net_exogenous_supply as
select put_name, -sum(make_use_base_amount) as net_supply
from all_4_amounts where year_run = :baseyear group by put_name;

drop table if exists use_down_i_up_factor;
create table use_down_i_up_factor as
select mf.year_run, mf.put_name, 
  coalesce(ns.net_supply,0) as net_exog_supply,
  coalesce(mf.new_amount,0) as make_future_amount,
  coalesce(totuse.amount,0) as total_use_amount,
  coalesce(i.amount,0) as import_amount,
  coalesce(internaluse.amount,0) as internaluse_amount,
  (coalesce(-ns.net_supply,0) - coalesce(mf.new_amount,0) + coalesce(totuse.amount,0) - coalesce(i.amount,0))/
  	(coalesce(i.amount,0) + coalesce(totuse.amount,0))
  as factor
from base_year_net_exogenous_supply ns
join make_future mf on ns.put_name = mf.put_name
left join (
   select year_run, put_name, -sum(makeuse_amount) as amount
   from all_4_amounts
   where moru = 'U'
   group by year_run, put_name
) totuse on totuse.year_run = mf.year_run
   and totuse.put_name = mf.put_name
left join (
   select year_run, put_name, sum(makeuse_amount) as amount
   from all_4_amounts
   where moru = 'M' and activity like :importer
   group by year_run, put_name
) i on totuse.year_run = i.year_run
	and totuse.put_name = i.put_name
left join (
   select year_run, put_name, -sum(makeuse_amount) as amount
   from all_4_amounts
   where moru = 'U' and activity not like :exporter
   group by year_run, put_name
) internaluse on totuse.year_run = internaluse.year_run
	and totuse.put_name = internaluse.put_name;
--where mf.put_name like 'G202%'
--	order by totuse.year_run;

-- Update imports and exports again based on scale factor

update activity_totals
set total_amount = coalesce((
    select scaled2_amount from (
        select mf.year_run, a.activity, a.total_amount as scaled1_amount,
        mf.factor, a.total_amount * (1+mf.factor) as scaled2_amount
        from (
            select year_run, activity, total_amount from activity_totals
        ) a
        join aef_make_use mu
            on a.activity = mu.activity
            and a.year_run = mu.year_run
            and mu.activity like :importer
        join use_down_i_up_factor mf
            on mf.put_name = mu.put_name
            and mu.year_run = mf.year_run
    ) x
    where activity_totals.year_run = x.year_run
    and activity_totals.activity = x.activity
    and activity_totals.year_run != :baseyear
), total_amount);


update activity_totals
set total_amount = coalesce((
    select scaled2_amount from (
        select mf.year_run, a.activity, a.total_amount as scaled1_amount,
        mf.factor, a.total_amount * (1-mf.factor) as scaled2_amount
        from (
            select year_run, activity, total_amount from activity_totals
        ) a
        join aef_make_use mu
            on a.activity = mu.activity
            and a.year_run = mu.year_run
            and mu.activity like :exporter
        join use_down_i_up_factor mf
            on mf.put_name = mu.put_name
            and mu.year_run = mf.year_run
    ) x
    where activity_totals.year_run = x.year_run
    and activity_totals.activity = x.activity
    and activity_totals.year_run != :baseyear
), total_amount);

drop table if exists coefficient_update;
create table coefficient_update
as 
select 
    uf.year_run,
	t.activity, t.option_name, t.option_weight,
	t.put_name_code,
	case when t.moru = 'U' and t.activity not like :exporter
	  then t.coefficient*(1-coalesce(uf.factor,0))
	else t.coefficient end as coefficient,
	t.put_name,
	t.moru,
	t.coefficient as original_coefficient
from technology_options t
left join use_down_i_up_factor uf
  on t.put_name = uf.put_name;
  
