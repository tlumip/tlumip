select null as "using constraints to check names of activities";

alter table activity_totals rename to activity_totals_old;

create table activity_totals (
    year_run integer,
    activity character,
    total_amount double,
    foreign key (activity) references activity (activity)
);

insert into activity_totals select * from activity_totals_old;

drop table activity_totals_old;