import csv
import logging
import sqlite3
import sys
from os.path import join

from sqlutil import Querier


def run_techscaling(year):
    props = read_props(year)
    # Update base year for years 2017 and above
    if year > 26:
        props['aa.base.year'] = props.get('aa.activitytotals.base.year', props['aa.base.year'])
        props['aa.base.data'] = props.get('aa.activitytotalsi.technologyoptionsi.dir', props['aa.base.data'])

    class Settings(object):
        def __init__(self):
            self.scendir = join(props["scenario.root"])
            self.input_dir = props["aa.base.data"]
            self.script_dir = join(self.scendir, "model", "code", "technology_scaling_scripts")
            self.working_dir = props["scenario.outputs"]
            self.cur_working_dir = props["aa.current.data"]

            self.baseyear = props["aa.base.year"]
            self.base_working_dir = join(self.working_dir, t(self.baseyear))
            self.curyear = props["t.year"]

            self.importer_string = "%_impt"
            self.exporter_string = "%_expt"

    ps = Settings()

    create_acttot_techopt(ps, connect_to_sqlite)


def create_acttot_techopt(ps, connect):
    set_up_logging()

    logging.info("Doing technology scaling for t{}".format(ps.curyear))

    tr = prepare_scripts(connect, ps)

    with tr:
        load_technology_options(tr, ps)

        tr.query_external(join(ps.script_dir, "002_load_tech.sql"))

        make_all_activity_totals(ps)
        load_all_activity_totals(ps, tr)
        load_make_use(tr, ps)
        calculate_scaling(tr, ps)
        update_techopt(tr, ps)

        tr.dump_to_csv(
            'select activity as "Activity",\n'
            'total_amount as "TotalAmount"\n'
            'from activity_totals\n'
            'where year_run = :year',
            join(ps.cur_working_dir, "ActivityTotalsW.csv"),
            year=ps.curyear
        )


def prepare_scripts(connect, ps):
    prepare_for_import(ps)

    querier = Querier(connect, row_factory=sqlite3.Row, debug_log=Logger())

    args = dict(
        baseyear=ps.baseyear,
        importer=ps.importer_string.replace("%", "%%"),
        exporter=ps.exporter_string.replace("%", "%%"),
    )

    return querier.transaction(**args)


def prepare_for_import(ps):
    with open(join(ps.input_dir, "TechnologyOptionsI.csv"), "rb") as inf:
        reader = csv.reader(inf)
        header = next(reader)
        act_col = header.index("Activity")
        opt_col = header.index("OptionName")
        wt_col = header.index("OptionWeight")
        put_col_start = max(act_col, opt_col, wt_col) + 1
        with open(join(ps.input_dir, "TechOptLongThin.csv"), "wb") as outf:
            writer = csv.writer(outf)
            for row in reader:
                for col, val in zip(header[put_col_start:], row[put_col_start:]):
                    writer.writerow([row[act_col], row[opt_col], row[wt_col], col, val])


def load_technology_options(tr, ps):
    tr.query("drop table if exists technology_options;")

    logging.info("Creating technology options table")
    tr.query(
        "create table technology_options\n"
        "(activity character varying,\n"
        "option_name character varying,\n"
        "option_weight double precision,\n"
        "put_name_code character varying,\n"
        "coefficient double precision);"
    )

    logging.info("Importing long thin tech options table")
    tr.load_from_csv(
        "technology_options",
        join(ps.input_dir, "TechOptLongThin.csv"),
        header=False
    )


def make_all_activity_totals(ps):
    base_fname = join(ps.base_working_dir, "ActivityTotalsW.csv")
    cur_fname = join(ps.cur_working_dir, "ActivityTotalsI.csv")
    out_fname = join(ps.cur_working_dir, "All_ActivityTotalsI.csv")
    with open(out_fname, "wb") as outf:
        writer = csv.writer(outf)
        writer.writerow(["year_run", "activity", "total_amount"])
        for year, fname in [(ps.baseyear, base_fname), (ps.curyear, cur_fname)]:
            with open(fname, "rb") as inf:
                reader = csv.reader(inf)
                header = next(reader)
                act_col = header.index("Activity")
                amt_col = header.index("TotalAmount")
                for line in reader:
                    writer.writerow([year, line[act_col], line[amt_col]])


def load_all_activity_totals(ps, tr):
    acttot_src = join(ps.cur_working_dir, "All_ActivityTotalsI.csv")

    prepare_activity_totals(tr)

    tr.load_from_csv(
        "activity_totals",
        acttot_src
    )


def prepare_activity_totals(tr):
    tr.query("drop table if exists activity_totals;")
    tr.query(
        "create table activity_totals\n"
        "(year_run integer,\n"
        "activity character varying,\n"
        "total_amount double precision);"
    )


def load_make_use(tr, ps):
    tr.query("drop table if exists make_use;")

    logging.info("Creating make use table")
    tr.query(
        "create table make_use\n"
        "("
        "   activity character varying,\n"
        "   put_name character varying,\n"
        "   moru character(1),\n"
        "   coefficient double precision,\n"
        "   stdev double precision,\n"
        "   amount double precision,\n"
        "   foreign key (activity) references activity (activity),\n"
        "   foreign key (put_name) references put (put_name)\n"
        ");"
    )

    logging.info("Importing make use table")
    tr.load_from_csv(
        "make_use",
        join(ps.base_working_dir, "MakeUse.csv")
    )


def calculate_scaling(tr, ps):
    tr.query_external(join(ps.script_dir, "005_check_names.sql"))
    tr.query_external(join(ps.script_dir, "010_aef_view.sql"))
    dump_all_4_amounts(tr, ps)
    tr.query_external(join(ps.script_dir, "020_scale_exporters.sql"))
    tr.query_external(join(ps.script_dir, "021_scale_importers.sql"))
    tr.query_external(join(ps.script_dir, "022_calculate_use_scale.sql"))
    dump_use_down_i_up_factor(tr, ps)


def dump_all_4_amounts(tr, ps):
    tr.dump_to_csv(
        'select year_run as "YearRun",\n'
        'activity as "Activity",\n'
        'moru as "Moru",\n'
        'put_name as "PutName",\n'
        'ev_amount as "EvAmount",\n'
        'weighted_amount as "WeightedAmount",\n'
        'makeuse_amount as "MakeUseAmount",\n'
        'make_use_base_amount as "MakeUseBaseAmount"\n'
        'from all_4_amounts',
        join(ps.cur_working_dir, "TechOptAll4Amounts.csv")
    )


def dump_use_down_i_up_factor(tr, ps):
    tr.dump_to_csv(
        'select year_run as "YearRun",\n'
        'put_name as "PutName",\n'
        'net_exog_supply as "NetExogSupply",\n'
        'make_future_amount as "MakeFutureAmount",\n'
        'total_use_amount as "TotalUseAmount",\n'
        'import_amount as "ImportAmount",\n'
        'internaluse_amount as "InternalUseAmount",\n'
        'factor as "Factor"\n'
        'from use_down_i_up_factor',
        join(ps.cur_working_dir, "UseDownIUpFactor.csv")
    )


def update_techopt(tr, ps):
    logging.info("Writing TechnologyOptionsW in {}".format(ps.curyear))
    result = tr.query(
        "select activity, option_name, option_weight, put_name_code, coefficient "
        "from coefficient_update where year_run = :year", year=ps.curyear)

    updates = dict_from_query(result)
    header, base = read_techopt(ps)

    outfname = join(ps.cur_working_dir, "TechnologyOptionsW.csv")
    logging.info("Writing to {}".format(outfname))
    with open(outfname, "wb") as outf:
        writer = csv.writer(outf)
        writer.writerow(header)
        for row in base:
            act = row[0]
            opt = row[1]
            update_row = updates.get(act, {}).get(opt, {})
            new_row = list(row)
            for i, put in enumerate(header[2:], 2):
                if put in update_row:
                    new_row[i] = update_row[put]
            writer.writerow(new_row)


def dict_from_query(result):
    tbl = {}
    for row in result:
        by_act = tbl.setdefault(row["activity"], {})
        by_opt = by_act.setdefault(row["option_name"], {})
        by_opt[row["put_name_code"]] = row["coefficient"]
    return tbl


def read_techopt(ps):
    infname = join(ps.input_dir, "TechnologyOptionsI.csv")
    with open(infname, "rb") as inf:
        reader = csv.reader(inf)
        header = next(reader)
        return header, list(reader)


def t(year):
    return "t{}".format(year)


class Logger(object):
    # noinspection PyMethodMayBeStatic
    def log(self, text):
        logging.info(text)


def connect_to_sqlite(name="techscaling"):
    return sqlite3.connect("{}.sqlite".format(name))


def set_up_logging():
    logging.basicConfig(level=logging.INFO)
    console = logging.StreamHandler()  # Create console logger
    console.setLevel(logging.INFO)  # Set the info level
    formatter = logging.Formatter('%(name)-12s: %(levelname)-8s %(message)s')  # Create a format
    console.setFormatter(formatter)  # Set the format
    logging.getLogger('').addHandler(console)  # Add this to the root logger


def read_props(year):
    fname = join("outputs", t(year), "aa.properties")
    props = {}
    with open(fname) as prop_file:
        for line in prop_file:
            if not line.startswith("#") and "=" in line:
                i = line.index("=")
                prop, value = line[:i].strip(), line[i + 1:].strip()
                if value.lower() == "true":
                    props[prop] = True
                elif value.lower() == "false":
                    props[prop] = False
                else:
                    try:
                        props[prop] = int(value)
                    except ValueError:
                        props[prop] = value
    return props


if __name__ == "__main__":
    run_techscaling(int(sys.argv[1][1:]))
