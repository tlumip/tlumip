# Pick a "main_event.log" file
File <- choose.files()

# bring into R
x <- readLines(File)

# report back time stamps for cases where AA did not converge
print(x[grep("FATAL, Term",x)] )