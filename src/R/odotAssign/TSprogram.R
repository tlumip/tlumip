	#This function will assign highway trips to the specified network.
	#Input files and parameters are defined in the OdotAssign.properties file.
	
	#First we must load the SJava library into the current workspace
	library(SJava)
	
	#Next we must initialize the Java Virtual Machine
	# set up the classpath
	tsConfig <- javaConfig(classPath=c("/jim/util/workspace3.0m4/common-assign/build/classes",
			"/jim/util/workspace3.0m4/common-base/build/classes","/jim/util/workspace3.0m4/common-daf-v2/build/classes",
			"/jim/util/workspace3.0m4/tlumip/build/classes","/jim/util/workspace3.0m4/tlumip/config"))
	#start the JVM, adding the classpath to the configuration file that is loaded when the VM starts up
	.JavaInit(config=tsConfig)
	
	#Finally, we call the static run method on the TS class (will return true is successful, false unsuccessful)
	#Arguments to .Java are [1]=name of class, [2]=name of method, *[3]=args to method (if necessary)
	success <- FALSE
	success <- .Java("OdotAssign","assignAggregateTrips","OdotAssign.properties")
	show(success)