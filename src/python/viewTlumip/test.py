from pyPgSQL import PgSQL

cx = PgSQL.connect(user="jhicks", password="cats1761", host="192.168.1.212", database="tlumip")
cu = cx.cursor()

table = [[ (1, 101, 205, 1.2, 35.0, 1200.0, 'a', 1, 2, 1, 800),
          (2, 101, 201, 1.7, 45.0, 1400.0, 'a', 1, 2, 2, 1200),
          (2, 201, 205, 1.1, 45.0, 1100.0, 'a', 1, 3, 4, 1200) ]]
          

query = 'INSERT INTO links VALUES \n'
for i in range(0,len(table[0])-1):
    query = query + '    (%d,%d,%d,%f,%f,%f,%s,%d,%d,%d,%d),\n' % table[0][i]
query = query + '    (%d,%d,%d,%f,%f,%f,%s,%d,%d,%d,%d)' % table[0][len(table[0])-1]

print query
          
"""
          
print "updating database ..."

query = 'DROP TABLE links'
cu.execute(query)

query = 'CREATE TABLE links ( id INT, type INT, an INT, bn INT, length FLOAT4, speed FLOAT4, capacity FLOAT4, mode VARCHAR(16), vdf INT, lanes FLOAT4, linktype INT, count INT)'
cu.execute(query)

query = 'INSERT INTO links VALUES ' + self.linkTable
cu.execute(query)

query = 'COMMIT'
cu.execute(query)

"""