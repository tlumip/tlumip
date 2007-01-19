from pyPgSQL import PgSQL


cx = PgSQL.connect(user="jhicks", password="cats1761", host="192.168.1.212", database="tlumip")
cu = cx.cursor()



query = 'CREATE TABLE atr_counts ( year INT, an INT, bn INT, count INT)'
cu.execute(query)

query = "COPY atr_counts FROM '/mnt/zufa/jim/projects/tlumip/NetworkData/NewNetwork/01dec2006/atr_1996_2002.csv' WITH CSV HEADER"
cu.execute(query)

query = "SELECT COUNT(*) FROM atr_counts"
cu.execute(query)
result = cu.fetchone()
print result[0], "entries copied into table atr_counts."

query = 'COMMIT'
cu.execute(query)
