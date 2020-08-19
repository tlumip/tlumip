import codecs
import csv

_NO_ARG = object()


class Querier(object):
    def __init__(self, connect, debug_log=None, row_factory=None, **kwargs):
        self.connect = connect
        self.debug_log = debug_log
        self.row_factory = row_factory
        self.kwargs = kwargs
        self._entered = False

    def __enter__(self):
        self._conn = self.connect()
        if self.row_factory is not None:
            self._conn.row_factory = self.row_factory
        self._entered = True
        return self

    # noinspection PyBroadException
    def __exit__(self, *args):
        try:
            self._entered = False
            self._conn.close()
        except Exception:
            pass

    def query(self, query, debug_log=_NO_ARG, row_factory=_NO_ARG, **kwargs):
        with self.transaction(row_factory) as tr:
            return tr.query(query, debug_log, **kwargs)

    def query_many(self, query, argslist, debug_log=_NO_ARG, **kwargs):
        with self.transaction() as tr:
            tr.query_many(query, argslist, debug_log, **kwargs)

    def query_external(self, fname, start=None, end=None, debug_log=_NO_ARG, **kwargs):
        with self.transaction() as tr:
            tr.query_external(fname, start, end, debug_log, **kwargs)

    def dump_to_csv(self, query, fname, debug_log=_NO_ARG, **kwargs):
        with self.transaction() as tr:
            tr.dump_to_csv(query, fname, debug_log, **kwargs)

    def load_from_csv(self, table, fname, header=True, debug_log=_NO_ARG, **kwargs):
        with self.transaction() as tr:
            tr.load_from_csv(table, fname, header, debug_log, **kwargs)

    def transaction(self, row_factory=_NO_ARG, **kwargs):
        return Transaction(self, row_factory, **kwargs)


class Transaction(object):
    def __init__(self, querier, row_factory, **kwargs):
        self._querier = querier
        if row_factory is _NO_ARG:
            row_factory = querier.row_factory
        self._row_factory = row_factory
        self._kwargs = kwargs
        self._collapsed_entry = False

    # noinspection PyProtectedMember
    def __enter__(self):
        if not self._querier._entered:
            self._querier.__enter__()
            self._collapsed_entry = True
        self._conn = self._querier._conn
        self._conn.__enter__()
        self._cur = self._conn.cursor()
        return self

    # noinspection PyBroadException
    def __exit__(self, *args):
        try:
            try:
                self._conn.__exit__(*args)
            finally:
                if self._collapsed_entry:
                    self._querier.__exit__(*args)
        except Exception:
            pass

    def query(self, query, debug_log=_NO_ARG, **kwargs):
        kwargs = self._full_kwargs(kwargs)
        debug_log = self._real_debug_log(debug_log)
        query = query.format(**kwargs)

        debug_log.log("Executing query:")
        debug_log.log(query)
        self._draw_line(debug_log)

        self._cur.execute(query, kwargs)
        result = self._cur.fetchall()
        return result

    def query_many(self, query, argslist, debug_log=_NO_ARG, **kwargs):
        kwargs = self._full_kwargs(kwargs)
        debug_log = self._real_debug_log(debug_log)
        query = query.format(**kwargs)

        debug_log.log("Executing batch query:")
        debug_log.log(query)
        self._draw_line(debug_log)

        self._cur.executemany(self._cur, query, argslist)

    def query_external(self, fname, start=None, end=None, debug_log=_NO_ARG, **kwargs):
        result = None

        with codecs.open(fname, encoding="utf-8-sig") as fin:
            lines = list(fin)
            it = iter(lines)
            if start is not None:
                for line in it:
                    line = line.strip()
                    if line.startswith("--"):
                        line = line[2:].strip()
                        if line == start:
                            break

            finished = False
            while not finished:
                query = ""
                for line in it:
                    if line.startswith("--"):
                        if end is not None:
                            comment = line[2:].strip()
                            if comment == end:
                                finished = True
                                break
                    else:
                        if line.strip():
                            query += line
                        if line.strip().endswith(";"):
                            break
                else:
                    finished = True

                if not query.strip():
                    continue

                new_result = self.query(query, debug_log, **kwargs)
                if new_result is not None:
                    result = new_result

        return result

    def dump_to_csv(self, query, fname, debug_log=_NO_ARG, **kwargs):
        kwargs = self._full_kwargs(kwargs)
        debug_log = self._real_debug_log(debug_log)
        fname = fname.format(**kwargs)

        debug_log.log("Dumping to file {} results of query:".format(fname))
        debug_log.log(query)
        self._draw_line(debug_log)

        with open(fname, "wb") as outf:
            result = self.query(query, debug_log, **kwargs)
            writer = csv.writer(outf)
            writer.writerow(result[0].keys())
            for row in result:
                writer.writerow(list(row))

    def load_from_csv(self, table, fname, header=True, debug_log=_NO_ARG, **kwargs):
        kwargs = self._full_kwargs(kwargs)
        debug_log = self._real_debug_log(debug_log)
        table = table.format(**kwargs)
        fname = fname.format(**kwargs)

        debug_log.log("Loading file {} into table {}".format(fname, table))
        self._draw_line(debug_log)

        with open(fname, "rb") as outf:
            reader = csv.reader(outf)
            if header:
                next(reader)
            rows = list(reader)
            query = ("insert into {} values (" + ",".join(["?" for _ in rows[0]]) + ")").format(table)
            self._cur.executemany(query, rows)

    def _full_kwargs(self, kwargs):
        full_kwargs = dict(self._querier.kwargs)
        full_kwargs.update(self._kwargs)
        full_kwargs.update(kwargs)
        return full_kwargs

    def _real_debug_log(self, debug_log):
        if debug_log is _NO_ARG:
            debug_log = self._querier.debug_log
        if debug_log is None:
            debug_log = NullLogger()
        return debug_log

    @staticmethod
    def _draw_line(debug_log):
        return debug_log.log("-" * 80)


class NullLogger(object):
    def log(self, text):
        pass
