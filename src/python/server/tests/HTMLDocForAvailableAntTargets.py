import string, csv, re, pprint, sys
from StringIO import StringIO

data = file(r"\\athena\zShare\models\tlumip\runtime\tlumip_dev.xml").read()
echo = data.split('<target name="echo">')[1].split('</target>')[0]
message = re.compile('<echo message="(.*?)"/>', re.DOTALL)
messages = [m for m in map(string.strip, message.findall(echo)) if m.startswith("run")]
result = []
for entry in messages:
    name, description, arguments = entry.split("'")
    name = name.split(',')[0].strip()
    description = (" ".join(description.replace("\n", "##n").split())).replace("##n", "\n")
    description = description.replace("\n ", "\n").strip()
    arguments = [t for t in map(string.strip, arguments.split(',')) if t]
    result.append([name, description, arguments])

html = file("AvailableAntTargets.html", 'w')

print >>html, """<html><body>
<table border="1">
"""

print >>html, "<tr><th>Name</th><th>Description</th><th>Arguments</th></tr>"
for row in result:
    print >>html, "<tr><td>%s</td>" % row[0]
    print >>html, "<td>%s</td>" % row[1]
    print >>html, "<td>%s</td>" % row[2]
    print >>html, "</tr>"

print >>html, """</table>
</body></html>
"""

html.close()

import webbrowser
webbrowser.open_new("AvailableAntTargets.html")
