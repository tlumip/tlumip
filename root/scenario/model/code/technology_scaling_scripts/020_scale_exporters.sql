drop table if exists make_future;
create table make_future
as
select year_run, put_name, sum(make_use_base_amount) as old_amount, sum(makeuse_amount) as new_amount,
sum(makeuse_amount)/sum(make_use_base_amount) as scale_factor
from all_4_amounts
where moru = 'M' and
activity not like :importer
group by year_run, put_name;

-- Scale exporters initially in proportion to make amount changes (make factor)

update activity_totals
set total_amount = coalesce((
    select scaled_amount from (
        select mf.year_run, a.activity, a.total_amount as base_amount,
        mf.scale_factor, a.total_amount * mf.scale_factor as scaled_amount
        from activity_totals a
        join aef_make_use mu
            on a.activity = mu.activity
        and a.activity like :exporter
        join make_future mf
            on mf.put_name = mu.put_name
            and mu.year_run = mf.year_run
        where a.year_run = :baseyear
    ) x
    where activity_totals.year_run = x.year_run
    and activity_totals.activity = x.activity
    and activity_totals.year_run != :baseyear
), total_amount);
