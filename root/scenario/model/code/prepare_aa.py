import os
import subprocess
import sys
from os.path import join

import retexchange
import techscaling


def main(year):
    retexchange.main("t{}".format(year))
    props = techscaling.read_props(year)
    run_preprocessor(props)
    if props["aa.technologyScaling"] and props["aa.updateImportsAndExports"]:
        move_preprocessor_files(props)
        techscaling.run_techscaling(year)


def run_preprocessor(props):
    javacmd = props["aa.command.java"]
    maxheap = props["aa.command.max.heap.size"]
    log4j = props["aa.command.log4j.config.file"]
    classpath = props["aa.command.classpath"]

    subprocess.call(
        [javacmd, "-Xmx" + maxheap, "-Dlog4j.configuration=" + log4j,
         "-cp", classpath, "com.hbaspecto.pecas.aa.control.AAPProcessor"]
    )


def move_preprocessor_files(props):
    cur_dir = props["aa.current.data"]
    try:
        os.remove(join(cur_dir, "ActivityTotalsI.csv"))
    except OSError:
        pass
    os.rename(join(cur_dir, "ActivityTotalsW.csv"), join(cur_dir, "ActivityTotalsI.csv"))


if __name__ == "__main__":
    main(int(sys.argv[1]))
