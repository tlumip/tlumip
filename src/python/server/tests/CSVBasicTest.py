import csv
file("test.csv", 'w')
writer = csv.writer(file("test.csv", "ab+"))
writer.writerow(["a", "b", 1, 2, 3])
writer.writerow(["a", "b", 1, 2, 4])
writer.writerow(["a", "b", 1, 2, 5])
writer = None
reader = csv.reader(file("test.csv", "rb"))
for row in reader: print row