drop table if exists use_future;
create table use_future
as 
select year_run, put_name, sum(make_use_base_amount) as old_amount, sum(makeuse_amount) as new_amount, 
sum(makeuse_amount)/sum(make_use_base_amount) as scale_factor
from all_4_amounts
where moru = 'U' and 
activity not like :exporter
group by year_run, put_name;

-- Scale Importers initially by use amount changes

update activity_totals
set total_amount = coalesce((
    select scaled_amount from (
        select uf.year_run, a.activity, a.total_amount as base_amount,
        uf.scale_factor, a.total_amount * uf.scale_factor as scaled_amount
        from activity_totals a
        join aef_make_use mu
            on a.activity = mu.activity
        and a.activity like :importer
        join use_future uf
            on uf.put_name = mu.put_name
            and mu.year_run = uf.year_run
        where a.year_run = :baseyear
    ) x
    where activity_totals.year_run = x.year_run
    and activity_totals.activity = x.activity
    and activity_totals.year_run != :baseyear
), total_amount);

