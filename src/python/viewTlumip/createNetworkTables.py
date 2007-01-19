from pyPgSQL import PgSQL


cx = PgSQL.connect(user="jhicks", password="cats1761", host="192.168.1.212", database="tlumip")
cu = cx.cursor()



query = 'CREATE TABLE links ( id INT, type INT, an INT, bn INT, length FLOAT4, mode VARCHAR(16), vdf INT, lanes INT, linktype INT, ul1 FLOAT4, ul2 FLOAT4, ul3 FLOAT4)'
cu.execute(query)

query = "COPY links FROM '/mnt/zufa/jim/projects/tlumip/NetworkData/NewNetwork/01dec2006/HistoricNetworksToPB_20061113/Emme2_Files/links_for_import.csv' WITH DELIMITER AS ','"
cu.execute(query)

query = 'CREATE TABLE nodes ( id INT, type INT, xcoord FLOAT4, ycoord FLOAT4)'
cu.execute(query)

query = "COPY nodes FROM '/mnt/zufa/jim/projects/tlumip/NetworkData/NewNetwork/01dec2006/HistoricNetworksToPB_20061113/Emme2_Files/nodes_for_import.csv' WITH DELIMITER AS ','"
cu.execute(query)

query = 'COMMIT'
cu.execute(query)
