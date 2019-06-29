conda create -n swimpy python=2.7
activate swimpy
conda install cytoolz pytables pandas numpy
pip install zbox orca openmatrix activitysim ortools pywin32
pip install https://github.com/RSGInc/populationsim/zipball/master
XCOPY "C:\Program Files\PTV Vision\PTV Visum 17\Exe\PythonModules\VisumPy" "C:\Program Files\Anaconda2\envs\swim\Lib\site-packages\VisumPy" /i