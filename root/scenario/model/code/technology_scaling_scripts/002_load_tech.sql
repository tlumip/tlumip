alter table technology_options add column put_name character;
alter table technology_options add column moru character(1);
update technology_options
    set moru = case when put_name_code like '%:1' then 'U' else 'M' end,
    put_name = case when put_name_code like '%:1' then substr(put_name_code, 1, length(put_name_code) - 2) else put_name_code end;

select null as "Creating activities table";
drop table if exists activity;
create table activity (
    activity character,
    unique (activity)
);
insert into activity select activity from technology_options group by activity order by activity;
alter table activity add column activity_type character(1);
update activity set activity_type='E' where activity like :exporter;
update activity set activity_type='I' where activity like :importer;


select null as "Creating options table";
drop table if exists tech_option;
create table tech_option as select activity, option_name, avg(option_weight) as weight from technology_options group by activity, option_name order by activity, option_name;

select null as "Creating puts table";
drop table if exists put;
create table put (
    put_name character,
    unique (put_name)
);
insert into put select put_name from technology_options group by put_name order by put_name;

select null as "removing zeroes from technology options";
delete from technology_options where coefficient = 0;

alter table technology_options rename to technology_options_old;

create table technology_options (
    activity character,
    option_name character,
    option_weight double,
    put_name_code character,
    coefficient double,
    put_name character,
    moru character(1),
    foreign key (activity) references activity (activity),
    foreign key (put_name) references put (put_name)
);

insert into technology_options select * from technology_options_old;

drop table technology_options_old;

